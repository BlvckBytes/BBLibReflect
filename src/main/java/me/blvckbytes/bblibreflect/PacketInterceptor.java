package me.blvckbytes.bblibreflect;

import io.netty.channel.*;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibdi.IAutoConstructed;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Registers a duplex channel handler for every player on the server
  at all times and allows external modifiers to register within a list of either
  global modifiers (for all players) or player-specifc modifiers. The ping packet,
  which does not correspond to an online player, is also caught. Player injections
  are released on leaving the server, while all injections are undone at the end
  of this module's lifecycle.
*/
@AutoConstruct
public class PacketInterceptor implements IPacketInterceptor, Listener, IAutoConstructed {

  // Name of ChannelHandler within the player's pipeline
  private final String HANDLER_NAME;

  // List of globally registered modifiers
  private final List<Tuple<IPacketModifier, ModificationPriority>> globalModifiers;

  // List of per-player registered modifiers
  // Use UUIDs here to allow persistence accross re-joins
  private final Map<UUID, ArrayList<Tuple<IPacketModifier, ModificationPriority>>> specificModifiers;

  // Vanilla network manager list before proxying, used for restoring
  @Nullable private Object vanillaNML;

  private final ILogger logger;
  private final MCReflect refl;
  private final ReentrantLock channelLock;

  public PacketInterceptor(
    @AutoInject ILogger logger,
    @AutoInject MCReflect refl,
    @AutoInject JavaPlugin plugin
  ) {
    this.globalModifiers = Collections.synchronizedList(new ArrayList<>());
    this.specificModifiers = Collections.synchronizedMap(new HashMap<>());
    this.channelLock = new ReentrantLock();

    this.logger = logger;
    this.refl = refl;

    // Generate a globally unique handler name
    this.HANDLER_NAME = (
      "pi_" +
      plugin.getName()
        .replace(" ", "_")
        .toLowerCase()
    );
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void register(IPacketModifier modifier, ModificationPriority priority) {
    this.globalModifiers.add(
      priority == ModificationPriority.HIGH ? 0 : this.globalModifiers.size(),
      new Tuple<>(modifier, priority)
    );
  }

  @Override
  public void unregister(IPacketModifier modifier) {
    this.globalModifiers.removeIf(t -> t.getA().equals(modifier));
  }

  @Override
  public boolean isRegistered(IPacketModifier modifier) {
    return this.globalModifiers
      .stream()
      .anyMatch(t -> t.getA().equals(modifier));
  }

  @Override
  public void registerSpecific(UUID target, IPacketModifier modifier, ModificationPriority priority) {
    // Create empty list to add to
    if (!this.specificModifiers.containsKey(target))
      this.specificModifiers.put(target, new ArrayList<>());

    // Add modifier to list
    this.specificModifiers.get(target).add(
      priority == ModificationPriority.HIGH ? 0 : this.specificModifiers.get(target).size(),
      new Tuple<>(modifier, priority)
    );
  }

  @Override
  public void unregisterSpecific(UUID target, IPacketModifier modifier) {
    // Player not even known yet
    if (!this.specificModifiers.containsKey(target))
      return;

    // Remove modifier from list
    List<Tuple<IPacketModifier, ModificationPriority>> modifiers = this.specificModifiers.get(target);
    modifiers.removeIf(t -> t.getA().equals(modifier));

    // Remove from map when no more modifiers remain
    if (modifiers.size() == 0)
      this.specificModifiers.remove(target);
  }

  @Override
  public boolean isRegisteredSpecific(UUID target, IPacketModifier modifier) {
    return this.specificModifiers.getOrDefault(target, new ArrayList<>())
      .stream()
      .anyMatch(t -> t.getA().equals(modifier));
  }

  @Override
  public void cleanup() {
    // Unproxy the network manager list
    unproxyNetworkList();

    // Unregister all globals
    // Loop in reverse to avoid concurrent modifications
    for (int i = this.globalModifiers.size() - 1; i >= 0; i--)
      this.unregister(this.globalModifiers.get(i).getA());

    // Unregister all specifics
    for (Map.Entry<UUID, ArrayList<Tuple<IPacketModifier, ModificationPriority>>> entry : specificModifiers.entrySet()) {

      // Loop in reverse to avoid concurrent modifications
      List<Tuple<IPacketModifier, ModificationPriority>> modifiers = entry.getValue();
      for (int i = modifiers.size() - 1; i >= 0; i--)
        this.unregisterSpecific(entry.getKey(), modifiers.get(i).getA());
    }

    // Uninject all players before a reload
    for (Player p : Bukkit.getOnlinePlayers())
      uninjectPlayer(p);
  }

  @Override
  public void initialize() {
    // Proxy the network manager list
    proxyNetworkList();

    // Inject all players after a reload
    for (Player p : Bukkit.getOnlinePlayers())
      injectPlayer(p);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent e) {
    injectPlayer(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    uninjectPlayer(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Remove a previously created injection from the player
   * @param p Target player
   */
  private void uninjectPlayer(Player p) {
    try {
      Channel nc = refl.getNetworkChannel(p);

      // Remove pipeline entry
      ChannelPipeline pipe = nc.pipeline();

      // Not registered in the pipeline
      if (!pipe.names().contains(HANDLER_NAME))
        return;

      // Remove handler
      pipe.remove(HANDLER_NAME);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Create a new injection for a pipeline
   * @param nm NetworkManager instance corresponding to the player, optional (null means not yet connected)
   * @param p Target player, optional (null means not yet connected)
   */
  private void injectChannel(@Nullable Player p, Object nm, ChannelPipeline pipe) {
    // Already registered in the pipeline, remove the old listener
    // This may happen when a player has already been injected by the NetworkManager-list proxy,
    // and now joined. Just remove the early handler and register a new one, which now knows
    // the player ref which the previous handler couldn't know.
    if (pipe.names().contains(HANDLER_NAME))
      pipe.remove(HANDLER_NAME);

    // Create a new intercepted channel handler which will relay all traffic to interceptors
    ChannelDuplexHandler handler = new InterceptedChannelDuplexHandler(
      nm, logger,
      // UUID is null for non-player-bound packets
      p == null ? null : p.getUniqueId(),
      // Provide refs to modifier lists to relay to
      globalModifiers, specificModifiers
    );

    // Create a new channel handler that overrides R/W to intercept
    // This handler gets created in this closure to provide player context
    // Packet handler registered already, add afterwards
    if (pipe.names().contains("packet_handler"))
      pipe.addBefore("packet_handler", HANDLER_NAME, handler);

    // No packet handler yet, just register as last entry
    else
      pipe.addLast(HANDLER_NAME, handler);
  }

  /**
   * Create a new injection for the player
   * @param p Target player, mandatory
   */
  private void injectPlayer(Player p) {
    try {
      Channel nc = refl.getNetworkChannel(p);
      Object nm = refl.getNetworkManager(p);

      channelLock.lock();
      injectChannel(p, nm, nc.pipeline());
    } catch (Exception e) {
      logger.logError(e);
    } finally {
      channelLock.unlock();
    }
  }

  /**
   * Undo a previously injected NML field proxy by restoring
   * back to the vanilla field ref
   */
  private void unproxyNetworkList() {
    // Not yet proxied
    if (vanillaNML == null)
      return;

    // Restore the vanilla NML field
    try {
      Object cs = refl.getCraftServer();
      Object console = refl.getFieldByName(cs, "console");
      Object sc = refl.getFieldByType(console, refl.getReflClass(ReflClass.SERVER_CONNECTION), 0);
      Field nmlf = refl.findGenericFieldByType(sc.getClass(), List.class, refl.getReflClass(ReflClass.NETWORK_MANAGER), 0);
      nmlf.set(sc, vanillaNML);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Proxy the network-manager's internal packet queue and wait for
   * poll() calls. When a call occurs, run the callback and unproxy immediately
   * @param nm NetworkManger to monitor
   * @param queuePolled Callback, invoked on poll()
   */
  private void callOnceOnQueuePoll(Object nm, Runnable queuePolled) {
    try {
      // Get the queue field value
      Class<?> qpC = refl.findInnerClass(refl.getReflClass(ReflClass.NETWORK_MANAGER), "QueuedPacket");
      Field qpF = refl.findGenericFieldByType(refl.getReflClass(ReflClass.NETWORK_MANAGER), Queue.class, qpC, 0);

      // Vanilla ref
      Object queue = qpF.get(nm);

      // Create a proxied queue
      qpF.set(nm, Proxy.newProxyInstance(
        nm.getClass().getClassLoader(),
        new Class[]{Queue.class},
        (proxy, method, args) -> {
          // Queue has been polled
          if (method.getName().equals("poll")) {
            // Undo proxy by re-setting the vanilla ref
            qpF.set(nm, queue);

            // Invoke callback
            queuePolled.run();
          }

          // Relay method call to vanilla queue
          return method.invoke(queue, args);
        }
      ));
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Inject a non-modifying read-only proxy into the NML field and
   * keep the vanilla ref for later restoring. Monitor for new NetworkManagers
   * and inject into their pipelines as soon as possible.
   */
  private void proxyNetworkList() {
    try {
      Object cs = refl.getCraftServer();
      Object console = refl.getFieldByName(cs, "console");
      Object sc = refl.getFieldByType(console, refl.getReflClass(ReflClass.SERVER_CONNECTION), 0);
      Field nmlf = refl.findGenericFieldByType(sc.getClass(), List.class, refl.getReflClass(ReflClass.NETWORK_MANAGER), 0);

      Object nml = nmlf.get(sc);

      // Create a proxied list
      Object proxiedList = Proxy.newProxyInstance(
        nml.getClass().getClassLoader(),
        new Class[]{List.class},

        (proxy, method, args) -> {
          // A new NetworkManager has just been instantiated and added to the list
          if (method.getName().equals("add")) {

            // Wait until the internal packet queue polled once, to
            // know when the connection has been initialized completely,
            // then inject this channel
            callOnceOnQueuePoll(args[0], () -> {
              try {
                Channel ch = refl.getNetworkChannel(args[0]);

                channelLock.lock();
                injectChannel(null, args[0], ch.pipeline());
              } catch (Exception e) {
                logger.logError(e);
              } finally {
                channelLock.unlock();
              }
            });
          }
          return method.invoke(nml, args);
        }
      );

      // Wrap this proxied list in a synchronizer
      proxiedList = Collections.synchronizedList((List<? extends Object>) proxiedList);

      // Set the field's value to the proxied list
      // and save the vanilla ref
      nmlf.set(sc, proxiedList);
      vanillaNML = nml;
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
