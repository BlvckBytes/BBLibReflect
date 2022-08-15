package me.blvckbytes.bblibreflect;

import com.mojang.authlib.GameProfile;
import io.netty.channel.*;
import io.netty.util.concurrent.GenericFutureListener;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibdi.IAutoConstructed;
import me.blvckbytes.bblibreflect.handle.Assignability;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.util.*;

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
public class PacketInterceptor implements IPacketInterceptor, IPacketModifier, Listener, IAutoConstructed {

  // Name of ChannelHandler within the player's pipeline
  private final String HANDLER_NAME;

  // List of globally registered modifiers
  private final List<Tuple<IPacketModifier, ModificationPriority>> globalModifiers;

  // List of per-player registered modifiers
  // Use UUIDs here to allow persistence across re-joins
  private final Map<UUID, ArrayList<Tuple<IPacketModifier, ModificationPriority>>> specificModifiers;

  // List of wrapped players
  private final Map<UUID, ICustomizableViewer> viewers;

  // Mapping players to their last known client version (used for reloads)
  private Map<UUID, Integer> clientVersions;

  // Used to buffer the clientVersions map across reloads
  private final Metadatable clientVersionBuffer;

  // Vanilla channel future list before proxying, used for restoring
  @Nullable private Object vanillaChannelFutureList;

  private final ILogger logger;
  private final APlugin plugin;

  private final FieldHandle F_ENTITY_PLAYER__PLAYER_CONNECTION, F_PLAYER_CONNECTION__NETWORK_MANAGER,
    F_NETWORK_MANAGER__CHANNEL, F_CRAFT_SERVER__MINECRAFT_SERVER, F_MINECRAFT_SERVER__SERVER_CONNECTION,
    F_SERVER_CONNECTION__FUTURE_LIST, F_PI_HANDSHAKE__VERSION,  F_PO_LOGIN__GAME_PROFILE, F_PO_OPEN_WINDOW__WINDOW_ID;

  private final MethodHandle M_CRAFT_PLAYER__GET_HANDLE, M_NETWORK_MANAGER__SEND_PACKET, M_CHANNEL_INITIALIZER__INIT_CHANNEL;

  private final ClassHandle C_PI_HANDSHAKE,  C_PO_OPEN_WINDOW, C_PO_LOGIN;

  public PacketInterceptor(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper reflection,
    @AutoInject APlugin plugin
  ) throws Exception {

    ClassHandle C_CRAFT_SERVER      = reflection.getClass(RClass.CRAFT_SERVER);
    ClassHandle C_PACKET            = reflection.getClass(RClass.PACKET);
    ClassHandle C_MINECRAFT_SERVER  = reflection.getClass(RClass.MINECRAFT_SERVER);
    ClassHandle C_NETWORK_MANAGER   = reflection.getClass(RClass.NETWORK_MANAGER);
    ClassHandle C_CRAFT_PLAYER      = reflection.getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_PLAYER_CONNECTION = reflection.getClass(RClass.PLAYER_CONNECTION);
    ClassHandle C_ENTITY_PLAYER     = reflection.getClass(RClass.ENTITY_PLAYER);
    ClassHandle C_SERVER_CONNECTION = reflection.getClass(RClass.SERVER_CONNECTION);

    C_PI_HANDSHAKE                  = reflection.getClass(RClass.PACKET_I_HANDSHAKE);
    C_PO_LOGIN                      = reflection.getClass(RClass.PACKET_O_LOGIN);
    C_PO_OPEN_WINDOW                = reflection.getClass(RClass.PACKET_O_OPEN_WINDOW);

    M_CRAFT_PLAYER__GET_HANDLE = C_CRAFT_PLAYER.locateMethod().withName("getHandle").required();
    M_NETWORK_MANAGER__SEND_PACKET = C_NETWORK_MANAGER.locateMethod().withParameters(C_PACKET).withParameters(GenericFutureListener.class).required();

    F_ENTITY_PLAYER__PLAYER_CONNECTION    = C_ENTITY_PLAYER.locateField().withType(C_PLAYER_CONNECTION).required();
    F_PLAYER_CONNECTION__NETWORK_MANAGER  = C_PLAYER_CONNECTION.locateField().withType(C_NETWORK_MANAGER).required();
    F_NETWORK_MANAGER__CHANNEL            = C_NETWORK_MANAGER.locateField().withType(Channel.class).required();
    F_CRAFT_SERVER__MINECRAFT_SERVER      = C_CRAFT_SERVER.locateField().withType(C_MINECRAFT_SERVER, false, Assignability.TYPE_TO_TARGET).required();
    F_MINECRAFT_SERVER__SERVER_CONNECTION = C_MINECRAFT_SERVER.locateField().withType(C_SERVER_CONNECTION).required();
    F_SERVER_CONNECTION__FUTURE_LIST      = C_SERVER_CONNECTION.locateField().withType(List.class).withGeneric(ChannelFuture.class).required();
    F_PI_HANDSHAKE__VERSION      = C_PI_HANDSHAKE.locateField().withType(int.class).required();
    F_PO_LOGIN__GAME_PROFILE     = C_PO_LOGIN.locateField().withType(GameProfile.class).required();
    F_PO_OPEN_WINDOW__WINDOW_ID  = C_PO_OPEN_WINDOW.locateField().withType(int.class).required();

    M_CHANNEL_INITIALIZER__INIT_CHANNEL = ClassHandle.of(ChannelInitializer.class).locateMethod().withName("initChannel").withParameters(Channel.class).required();

    // Packet modifier registry
    this.globalModifiers = Collections.synchronizedList(new ArrayList<>());
    this.specificModifiers = Collections.synchronizedMap(new HashMap<>());
    this.clientVersions = Collections.synchronizedMap(new HashMap<>());

    this.viewers = Collections.synchronizedMap(new HashMap<>());
    this.logger = logger;
    this.plugin = plugin;

    // Generate a globally unique handler name
    this.HANDLER_NAME = (
      "pi_" +
      plugin.getName()
        .replace(" ", "_")
        .toLowerCase()
    );

    // Register self as a modifier
    globalModifiers.add(new Tuple<>(this, ModificationPriority.HIGH));

    // Store client versions temporarily on the first available world
    this.clientVersionBuffer = Bukkit.getWorlds().get(0);

    // Proxy the network manager list
    proxyFutureList();
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
    ICustomizableViewer viewer = viewers.get(p.getUniqueId());

    // For some reason, the viewer is not available yet, inject now
    if (viewer == null)
      viewer = inject(p);

    return viewer;
  }

  @Override
  public void cleanup() {
    // Update the client version buffer map
    clientVersionBuffer.removeMetadata(HANDLER_NAME, plugin);
    clientVersionBuffer.setMetadata(HANDLER_NAME, new FixedMetadataValue(plugin, clientVersions));

    // Unproxy the network manager list
    unproxyFutureList();

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
  @SuppressWarnings("unchecked")
  public void initialize() {
    List<MetadataValue> metadata = clientVersionBuffer.getMetadata(HANDLER_NAME);

    // Loop through all stored values
    for (MetadataValue value : metadata) {
      // Not by this plugin
      if (!value.getOwningPlugin().equals(plugin))
        continue;

      // Not a map
      if (!(value.value() instanceof Map))
        continue;

      // Set the cache to the metadata value
      this.clientVersions = (Map<UUID, Integer>) value.value();
    }

    // Inject all players after a reload
    for (Player p : Bukkit.getOnlinePlayers())
      inject(p);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Uninject the player
    IPacketReceiver receiver = viewers.remove(e.getPlayer().getUniqueId());
    if (receiver != null)
      uninject(receiver);

    // Also remove the version cache
    clientVersions.remove(e.getPlayer().getUniqueId());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Inject a player manually, used after reloads
   * @param p Target player
   */
  private InterceptedViewer inject(Player p) {
    try {
      // Get the handle of the underlying CraftPlayer
      Object entityPlayer = M_CRAFT_PLAYER__GET_HANDLE.invoke(p);

      // Get the NMS EntityPlayer's PlayerConnection
      Object playerConnection = F_ENTITY_PLAYER__PLAYER_CONNECTION.get(entityPlayer);

      // Get the PlayerConnection's NetworkManager
      Object networkManager = F_PLAYER_CONNECTION__NETWORK_MANAGER.get(playerConnection);

      // Get the Channel of that NetworkManager
      Channel channel = (Channel) F_NETWORK_MANAGER__CHANNEL.get(networkManager);

      // Create a new intercepted viewer
      InterceptedViewer viewer = new InterceptedViewer(
        channel, networkManager, logger, M_NETWORK_MANAGER__SEND_PACKET
      );

      // Try to look up the previous client version in the cache
      viewer.setClientVersion(clientVersions.getOrDefault(p.getUniqueId(), -1));

      // Set UUID immediately, as it's known already
      viewer.setUuid(p.getUniqueId());
      viewers.put(p.getUniqueId(), viewer);
      return viewer;
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  /**
   * Remove a previously created injection from a receiver
   * @param receiver Receiver to uninject
   */
  private void uninject(IPacketReceiver receiver) {
    try {
      Channel channel = receiver.getChannel();

      // Remove pipeline entry
      ChannelPipeline pipe = channel.pipeline();

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
   * Undo a previously injected NetworkManager's future list field proxy
   * by restoring back to the vanilla field ref
   */
  private void unproxyFutureList() {
    // Not yet proxied
    if (vanillaChannelFutureList == null)
      return;

    try {
      // Get the MinecraftServer from the Server's underlying CraftServer
      Object minecraftServer = F_CRAFT_SERVER__MINECRAFT_SERVER.get(Bukkit.getServer());

      // Get it's server connection
      Object serverConnection = F_MINECRAFT_SERVER__SERVER_CONNECTION.get(minecraftServer);

      // Restore the vanilla list ref
      F_SERVER_CONNECTION__FUTURE_LIST.set(serverConnection, vanillaChannelFutureList);

      // Clear the ref again
      vanillaChannelFutureList = null;
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Inject a non-modifying read-only proxy into the NetworkManager's ChannelFuture
   * list field and keep the vanilla ref for later restoring. Then, proxy all
   * contained ChannelFutures and proxy added futures right before being relayed
   * to the real list again.
   */
  @SuppressWarnings("unchecked")
  private void proxyFutureList() {
    try {
      // Get the MinecraftServer from the Server's underlying CraftServer
      Object minecraftServer = F_CRAFT_SERVER__MINECRAFT_SERVER.get(Bukkit.getServer());

      // Get it's server connection
      Object serverConnection = F_MINECRAFT_SERVER__SERVER_CONNECTION.get(minecraftServer);

      // Get the list of channel futures
      List<ChannelFuture> futureList = (List<ChannelFuture>) F_SERVER_CONNECTION__FUTURE_LIST.get(serverConnection);

      // Create a proxy which internally relays to the real list and catches add calls
      Object proxiedFutureList = Proxy.newProxyInstance(
        futureList.getClass().getClassLoader(),
        new Class[]{List.class},

        (proxy, method, args) -> {
          // Proxy future before adding it to the list
          if (method.getName().equals("add"))
            proxyChannelFuture((ChannelFuture) args[0]);

          return method.invoke(futureList, args);
        }
      );

      // Synchronizing to the real list before overriding to avoid
      // changing it while it itself is being modified
      synchronized (futureList) {
        // Loop and proxy all existing futures before proxying the list
        for (ChannelFuture existingFuture : futureList)
          proxyChannelFuture(existingFuture);

        // Override vanilla field with proxy
        F_SERVER_CONNECTION__FUTURE_LIST.set(serverConnection, proxiedFutureList);

        // Store the vanilla ref
        vanillaChannelFutureList = futureList;
      }
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Injects a new handler into the future's channel as soon as available by
   * proxying it's channel initializer and injecting right after it's ready
   * @param future ChannelFuture to proxy
   */
  private void proxyChannelFuture(ChannelFuture future) {
    try {
      Channel channel = future.channel();
      List<String> names = channel.pipeline().names();

      // Loop all handlers within the channel's pipeline
      for (String name : names) {
        ChannelHandler handler = channel.pipeline().get(name);

        // Yes, that happens...
        if (handler == null)
          continue;

        // Try to find the child handler field
        FieldHandle childField = ClassHandle.of(handler.getClass()).locateField()
            .withName("childHandler")
            .optional();

        // Has no child handler, not of interest
        if (childField == null)
          continue;

        // Get the vanilla initializer
        ChannelInitializer<?> vanillaInitializer = (ChannelInitializer<?>) childField.get(handler);

        // Create a new simple relaying initializer
        ChannelInitializer<?> proxyInitializer = new ChannelInitializer<>() {

          @Override
          protected void initChannel(Channel ch) throws Exception {
            // Have the channel initialized by the real initializer first
            M_CHANNEL_INITIALIZER__INIT_CHANNEL.invoke(vanillaInitializer, ch);

            // Get the network manager through the channel handler added by the vanilla initializer
            Object networkManager = ch.pipeline().get("packet_handler");

            // Let's just assume it's going to be a player
            // UUID is staying null otherwise, which signals that it's a player-less receiver
            InterceptedViewer viewer = new InterceptedViewer(
              ch, networkManager, logger, M_NETWORK_MANAGER__SEND_PACKET
            );

            injectChannel(viewer);
          }
        };

        // Install the proxy initializer
        // I don't plan on un-proxying this later, as it's irrelevant
        childField.set(handler, proxyInitializer);
        break;
      }
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public Object modifyIncoming(IPacketReceiver sender, Object incoming) {
    // Not an intercepted viewer, cannot update anything
    if (!(sender instanceof InterceptedViewer))
      return incoming;

    InterceptedViewer viewer = (InterceptedViewer) sender;

    try {
      // Update the client version, now that it's known
      if (C_PI_HANDSHAKE.isInstance(incoming)) {
        int version = (int) F_PI_HANDSHAKE__VERSION.get(incoming);
        viewer.setClientVersion(version);
      }
    } catch (Exception e) {
      logger.logError(e);
    }

    return incoming;
  }

  @Override
  public Object modifyOutgoing(IPacketReceiver receiver, Object outgoing) {
    // Not an intercepted viewer, cannot update anything
    if (!(receiver instanceof InterceptedViewer))
      return receiver;

    InterceptedViewer viewer = (InterceptedViewer) receiver;

    try {
      // Update the UUID, now that it's known
      if (C_PO_LOGIN.isInstance(outgoing)) {
        GameProfile profile = (GameProfile) F_PO_LOGIN__GAME_PROFILE.get(outgoing);
        viewer.setUuid(profile.getId());

        // Also register as a viewer now
        viewers.put(profile.getId(), viewer);

        // Cache the client version
        clientVersions.put(profile.getId(), viewer.getClientVersion());
      }

      // Update the client's window id
      if (C_PO_OPEN_WINDOW.isInstance(outgoing)) {
        int windowId = (int) F_PO_OPEN_WINDOW__WINDOW_ID.get(outgoing);
        viewer.setCurrentWindowId(windowId);
      }
    } catch (Exception e) {
      logger.logError(e);
    }

    return outgoing;
  }
}
