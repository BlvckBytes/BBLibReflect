package me.blvckbytes.bblibreflect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPipeline;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibdi.IAutoConstructed;
import me.blvckbytes.bblibreflect.handle.Assignability;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
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

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Registers a duplex channel handler for every player on the server
  at all times and allows external modifiers to register within a list of either
  global modifiers (for all players) or player-specifc modifiers. The ping packet,
  which does not correspond to an online player, is also caught. Player injections
  are released on leaving the server, while all injections are undone at the end
  of this module's lifecycle.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published
  by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
@AutoConstruct
public class PacketInterceptor implements IPacketInterceptor, Listener, IAutoConstructed {

  // Name of ChannelHandler within the player's pipeline
  private final String HANDLER_NAME;

  // List of globally registered modifiers
  private final List<Tuple<IPacketModifier, ModificationPriority>> globalModifiers;

  // List of per-player registered modifiers
  // Use UUIDs here to allow persistence across re-joins
  private final Map<UUID, ArrayList<Tuple<IPacketModifier, ModificationPriority>>> specificModifiers;

  // List of wrapped players
  private final Map<Player, ICustomizableViewer> viewers;

  // Vanilla network manager list before proxying, used for restoring
  @Nullable private Object vanillaNML;

  private final ILogger logger;
  private final ReentrantLock channelLock;
  private final Function<Player, Integer> windowId;

  private final FieldHandle F_ENTITY_PLAYER__PLAYER_CONNECTION, F_PLAYER_CONNECTION__NETWORK_MANAGER,
    F_NETWORK_MANAGER__CHANNEL, F_CRAFT_SERVER__MINECRAFT_SERVER, F_MINECRAFT_SERVER__SERVER_CONNECTION,
    F_SERVER_CONNECTION__NETWORK_LIST, F_NETWORK_MANAGER__QUEUE, F_ENTITY_HUMAN__CONTAINER_DEFAULT_OR_ACTIVE,
    F_CONTAINER__WINDOW_ID;

  private final @Nullable FieldHandle F_ENTITY_HUMAN__CONTAINER_ACTIVE;

  private final MethodHandle M_CRAFT_PLAYER__GET_HANDLE, M_NETWORK_MANAGER__SEND_PACKET;

  public PacketInterceptor(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper reflection,
    @AutoInject JavaPlugin plugin
  ) throws Exception {

    ClassHandle C_CRAFT_SERVER      = reflection.getClass(RClass.CRAFT_SERVER);
    ClassHandle C_PACKET            = reflection.getClass(RClass.PACKET);
    ClassHandle C_MINECRAFT_SERVER  = reflection.getClass(RClass.MINECRAFT_SERVER);
    ClassHandle C_NETWORK_MANAGER   = reflection.getClass(RClass.NETWORK_MANAGER);
    ClassHandle C_CRAFT_PLAYER      = reflection.getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_PLAYER_CONNECTION = reflection.getClass(RClass.PLAYER_CONNECTION);
    ClassHandle C_ENTITY_PLAYER     = reflection.getClass(RClass.ENTITY_PLAYER);
    ClassHandle C_SERVER_CONNECTION = reflection.getClass(RClass.SERVER_CONNECTION);
    ClassHandle C_QUEUED_PACKET     = reflection.getClass(RClass.QUEUED_PACKET);

    ClassHandle C_ENTITY_HUMAN = reflection.getClass(RClass.ENTITY_HUMAN);
    ClassHandle C_CONTAINER    = reflection.getClass(RClass.CONTAINER);

    M_CRAFT_PLAYER__GET_HANDLE = C_CRAFT_PLAYER.locateMethod().withName("getHandle").required();
    M_NETWORK_MANAGER__SEND_PACKET = C_NETWORK_MANAGER.locateMethod().withParameters(C_PACKET).required();

    F_ENTITY_PLAYER__PLAYER_CONNECTION    = C_ENTITY_PLAYER.locateField().withType(C_PLAYER_CONNECTION).required();
    F_PLAYER_CONNECTION__NETWORK_MANAGER  = C_PLAYER_CONNECTION.locateField().withType(C_NETWORK_MANAGER).required();
    F_NETWORK_MANAGER__CHANNEL            = C_NETWORK_MANAGER.locateField().withType(Channel.class).required();
    F_CRAFT_SERVER__MINECRAFT_SERVER      = C_CRAFT_SERVER.locateField().withType(C_MINECRAFT_SERVER, false, Assignability.TYPE_TO_TARGET).required();
    F_MINECRAFT_SERVER__SERVER_CONNECTION = C_MINECRAFT_SERVER.locateField().withType(C_SERVER_CONNECTION).required();
    F_SERVER_CONNECTION__NETWORK_LIST     = C_SERVER_CONNECTION.locateField().withType(List.class).withGeneric(C_NETWORK_MANAGER).required();
    F_NETWORK_MANAGER__QUEUE              = C_NETWORK_MANAGER.locateField().withType(Queue.class).withGeneric(C_QUEUED_PACKET).required();
    F_ENTITY_HUMAN__CONTAINER_DEFAULT_OR_ACTIVE = C_ENTITY_HUMAN.locateField().withType(C_CONTAINER).required();
    F_ENTITY_HUMAN__CONTAINER_ACTIVE            = C_ENTITY_HUMAN.locateField().withType(C_CONTAINER).withSkip(1).optional();
    F_CONTAINER__WINDOW_ID = C_CONTAINER.locateField().withType(int.class).withPublic(true).required();

    this.globalModifiers = Collections.synchronizedList(new ArrayList<>());
    this.specificModifiers = Collections.synchronizedMap(new HashMap<>());
    this.viewers = new HashMap<>();
    this.channelLock = new ReentrantLock();
    this.windowId = getWindowIdAccess();
    this.logger = logger;

    // Generate a globally unique handler name
    this.HANDLER_NAME = (
      "pi_" +
      plugin.getName()
        .replace(" ", "_")
        .toLowerCase()
    );
  }

  private ICustomizableViewer playerToReceiver(Player p) throws Exception {
    // Get the handle of the underlying CraftPlayer
    Object entityPlayer = M_CRAFT_PLAYER__GET_HANDLE.invoke(p);

    // Get the NMS EntityPlayer's PlayerConnection
    Object playerConnection = F_ENTITY_PLAYER__PLAYER_CONNECTION.get(entityPlayer);

    // Get the PlayerConnection's NetworkManager
    Object networkManager = F_PLAYER_CONNECTION__NETWORK_MANAGER.get(playerConnection);

    // Get the Channel of that NetworkManager
    Channel channel = (Channel) F_NETWORK_MANAGER__CHANNEL.get(networkManager);

    // Create a new anonymous instance for this player
    return new ICustomizableViewer() {

      @Override
      public int getCurrentWindowId() {
        return windowId.apply(p);
      }

      @Override
      public boolean cannotRenderHexColors() {
        // TODO: Implement properly
        return false;
      }

      @Override
      public void sendPackets(Object... packets) {
        try {
          // Send packets in a sequence
          for (Object packet : packets)
            M_NETWORK_MANAGER__SEND_PACKET.invoke(networkManager, packet);
        } catch (Exception e) {
          logger.logError(e);
        }
      }

      @Override
      public @Nullable UUID getUuid() {
        return p.getUniqueId();
      }

      @Override
      public Channel getChannel() {
        return channel;
      }

    };
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
  public ICustomizableViewer getPlayerAsViewer(Player p) {
    try {
      ICustomizableViewer viewer = viewers.get(p);

      // Create a new receiver if not present yet
      if (viewer == null) {
        viewer = playerToReceiver(p);
        viewers.put(p, viewer);
      }

      return viewer;
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  @Override
  public void broadcastPackets(Object... packets) {
    viewers.values().forEach(v -> v.sendPackets(packets));
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
    for (Player p : Bukkit.getOnlinePlayers()) {
      IPacketReceiver receiver = viewers.remove(p);
      if (receiver != null)
        uninject(receiver);
    }
  }

  @Override
  public void initialize() {
    // Proxy the network manager list
    proxyNetworkList();

    // Inject all players after a reload
    for (Player p : Bukkit.getOnlinePlayers())
      inject(p);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent e) {
    inject(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    IPacketReceiver receiver = viewers.remove(e.getPlayer());
    if (receiver != null)
      uninject(receiver);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Remove a previously created injection from a receiver
   * @param receiver Receiver to uninject
   */
  private void uninject(IPacketReceiver receiver) {
    try {
      // Remove pipeline entry
      ChannelPipeline pipe = receiver.getChannel().pipeline();

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
   * Create a new channel injection on any packet receiver
   * @param receiver Receiver to inject
   */
  private void injectChannel(@Nullable IPacketReceiver receiver) {
    // Cannot inject anything
    if (receiver == null)
      return;

    ChannelPipeline pipe = receiver.getChannel().pipeline();

    // Already registered in the pipeline, remove the old listener
    // This may happen when a player has already been injected by the NetworkManager-list proxy,
    // and now joined. Just remove the early handler and register a new one, which now knows
    // the player ref which the previous handler couldn't know.
    if (pipe.names().contains(HANDLER_NAME))
      pipe.remove(HANDLER_NAME);

    // Create a new intercepted channel handler which will relay all traffic to interceptors
    ChannelDuplexHandler handler = new InterceptedChannelDuplexHandler(
      logger, receiver,
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
  private void inject(Player p) {
    try {
      channelLock.lock();
      injectChannel(getPlayerAsViewer(p));
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

    try {
      // Get the MinecraftServer from the Server's underlying CraftServer
      Object minecraftServer = F_CRAFT_SERVER__MINECRAFT_SERVER.get(Bukkit.getServer());

      // Get it's server connection
      Object serverConnection = F_MINECRAFT_SERVER__SERVER_CONNECTION.get(minecraftServer);

      // Restore the vanilla list ref
      F_SERVER_CONNECTION__NETWORK_LIST.set(serverConnection, vanillaNML);
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
      // Get the vanilla packet queue within the network manager
      Object queue = F_NETWORK_MANAGER__QUEUE.get(nm);

      // Create a proxied queue
      F_NETWORK_MANAGER__QUEUE.set(nm, Proxy.newProxyInstance(
        nm.getClass().getClassLoader(),
        new Class[]{Queue.class},
        (proxy, method, args) -> {
          // Queue has been polled
          if (method.getName().equals("poll")) {
            // Undo proxy by re-setting the vanilla ref
            F_NETWORK_MANAGER__QUEUE.set(nm, queue);

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
      // Get the MinecraftServer from the Server's underlying CraftServer
      Object minecraftServer = F_CRAFT_SERVER__MINECRAFT_SERVER.get(Bukkit.getServer());

      // Get it's server connection
      Object serverConnection = F_MINECRAFT_SERVER__SERVER_CONNECTION.get(minecraftServer);

      // Get the List<NetworkManager> within the ServerConnection
      Object networkList = F_SERVER_CONNECTION__NETWORK_LIST.get(serverConnection);

      // Create a proxied list
      Object proxiedList = Proxy.newProxyInstance(
        networkList.getClass().getClassLoader(),
        new Class[]{List.class},

        (proxy, method, args) -> {
          // A new NetworkManager has just been instantiated and added to the list
          if (method.getName().equals("add")) {

            // Wait until the internal packet queue polled once, to
            // know when the connection has been initialized completely,
            // then inject this channel
            callOnceOnQueuePoll(args[0], () -> {
              try {
                // Get the Channel of the added NetworkManager
                Object networkManager = args[0];
                Channel channel = (Channel) F_NETWORK_MANAGER__CHANNEL.get(networkManager);

                channelLock.lock();
                injectChannel(new IPacketReceiver() {

                  @Override
                  public void sendPackets(Object... packets) {
                    try {
                      // Send packets in a sequence
                      for (Object packet : packets)
                        M_NETWORK_MANAGER__SEND_PACKET.invoke(networkManager, packet);
                    } catch (Exception e) {
                      logger.logError(e);
                    }
                  }

                  @Override
                  public @Nullable UUID getUuid() {
                    // Has no UUID yet
                    return null;
                  }

                  @Override
                  public Channel getChannel() {
                    return channel;
                  }
                });
              } catch (Exception e) {
                logger.logError(e);
              } finally {
                channelLock.unlock();
              }
            });
          }
          return method.invoke(networkList, args);
        }
      );

      // Wrap this proxied list in a synchronizer
      proxiedList = Collections.synchronizedList((List<?>) proxiedList);

      // Set the field's value to the proxied list
      // and save the vanilla ref
      F_SERVER_CONNECTION__NETWORK_LIST.set(serverConnection, proxiedList);
      vanillaNML = networkList;
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Find access to the active window ID of a player
   */
  private Function<Player, Integer> getWindowIdAccess() {
    try {
      // Newer versions just have one field (active) where as older have two (default, active)
      FieldHandle containerField = (
        F_ENTITY_HUMAN__CONTAINER_ACTIVE == null ?
          F_ENTITY_HUMAN__CONTAINER_DEFAULT_OR_ACTIVE :
          F_ENTITY_HUMAN__CONTAINER_ACTIVE
      );

      // Get the container at runtime and then access it's ID field
      return p -> {
        try {
          // Get the handle of the underlying CraftPlayer
          Object entityPlayer = M_CRAFT_PLAYER__GET_HANDLE.invoke(p);

          // Get the player's active container
          Object container = containerField.get(entityPlayer);

          // Get that container's window ID
          return (int) F_CONTAINER__WINDOW_ID.get(container);
        } catch (Exception e) {
          return 0;
        }
      };
    } catch (Exception e) {
      logger.logError(e);
      return p -> 0;
    }
  }
}
