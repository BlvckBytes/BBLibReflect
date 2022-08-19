package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.Getter;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import me.blvckbytes.bblibreflect.handle.Assignability;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.UnsafeBiConsumer;
import me.blvckbytes.bblibutil.UnsafeSupplier;
import me.blvckbytes.bblibutil.component.IComponent;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  The base of all packet communicator implementations. Holds commonly used handles,
  the packet class that's managed, whether a viewer is required as well as all
  required injected dependency references. Implements the communicator API in a
  very generally applicative way so inheritors don't have to, but may.

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
public abstract class APacketCommunicator<T extends ICommunicatorParameter> implements IPacketCommunicator<T> {

  protected final ILogger logger;
  protected final IReflectionHelper helper;
  protected final IPacketInterceptor interceptor;

  protected final MethodHandle M_CHAT_SERIALIZER__FROM_JSON, M_CRAFT_ITEM_STACK__AS_NMS_COPY, M_CRAFT_PLAYER__GET_PROFILE ;
  protected final ClassHandle C_CHAT_SERIALIZER, C_BASE_COMPONENT, C_CRAFT_ITEM_STACK, C_ITEM_STACK, C_CRAFT_PLAYER;

  @Getter(AccessLevel.PROTECTED)
  private final ClassHandle packetClass;

  private final boolean requiresViewer;

  public APacketCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    UnsafeSupplier<ClassHandle> packetClass
  ) throws Exception {
    this(logger, helper, interceptor, requiresViewer, packetClass.get());
  }

  public APacketCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    ClassHandle packetClass
  ) throws Exception {
    this.logger = logger;
    this.helper = helper;
    this.interceptor = interceptor;
    this.requiresViewer = requiresViewer;
    this.packetClass = packetClass;

    C_CHAT_SERIALIZER  = helper.getClass(RClass.CHAT_SERIALIZER);
    C_BASE_COMPONENT   = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);
    C_CRAFT_ITEM_STACK = helper.getClass(RClass.CRAFT_ITEM_STACK);
    C_ITEM_STACK       = helper.getClass(RClass.ITEM_STACK);
    C_CRAFT_PLAYER     = helper.getClass(RClass.CRAFT_PLAYER);

    M_CHAT_SERIALIZER__FROM_JSON = C_CHAT_SERIALIZER.locateMethod()
      .withParameters(JsonElement.class)
      .withReturnType(C_BASE_COMPONENT, false, Assignability.TYPE_TO_TARGET)
      .withStatic(true)
      .required();

    M_CRAFT_ITEM_STACK__AS_NMS_COPY = C_CRAFT_ITEM_STACK.locateMethod()
      .withName("asNMSCopy")
      .withStatic(true)
      .required();

    M_CRAFT_PLAYER__GET_PROFILE = C_CRAFT_PLAYER.locateMethod()
      .withName("getProfile")
      .withReturnType(GameProfile.class)
      .required();

    interceptor.registerCommunicator(this);
  }

  //=========================================================================//
  //                         Required Implementations                        //
  //=========================================================================//

  /**
   * Get the type of parameter this communicator accepts
   */
  public abstract Class<T> getParameterType();

  /**
   * Create a new zeroed out base packet
   * @param parameter Parameter to create off of
   * @return Zeroed out base packet
   * @throws Exception Internal errors
   */
  protected abstract Object createBasePacket(T parameter) throws Exception;

  /**
   * Personalize a previously created base packet for a specific viewer. If the implementation
   * does not support personalization, throw a {@link UnsupportedOperationException}.
   * @param packet Packet to personalize
   * @param parameter Parameter to personalize with
   * @param viewer Viewer to personalize for
   * @throws Exception Internal errors
   */
  protected abstract void personalizeBasePacket(Object packet, T parameter, ICustomizableViewer viewer) throws Exception;

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Create a new empty packet instance of the communicator's
   * managed packet type
   * @return Packet instance ready to use
   */
  protected Object createPacket() throws Exception {
    return helper.createEmptyPacket(packetClass);
  }

  /**
   * Transform an internal component into a IChatBaseComponent
   * while also customizing it for a specific viewer
   * @param component Component to transform
   * @param viewer Viewer which is viewing this component, optional
   * @return Transformed component
   * @throws Exception Internal errors
   */
  public Object componentToBaseComponent(IComponent component, @Nullable ICustomizableViewer viewer) throws Exception {
    return M_CHAT_SERIALIZER__FROM_JSON.invoke(null, component.toJson(viewer == null || viewer.cannotRenderHexColors()));
  }

  /**
   * Sends multiple packets to a given receiver and, if required, synchronizes
   * their completion callback to only invoke the provided callback after all packets completed
   * @param receiver Target receiver to send to
   * @param done Optional callback, called when all packets are done
   * @param packets Packets to send
   */
  protected void sendPacketsToReceiver(IPacketReceiver receiver, @Nullable Runnable done, Object... packets) {
    // No callback required, just perform a sending invocation
    if (done == null) {
      for (Object packet : packets)
        receiver.sendPacket(packet, null);
      return;
    }

    AtomicInteger completionCount = new AtomicInteger(0);

    for (Object packet : packets) {
      receiver.sendPacket(packet, () -> {
        // Check if it's the last packet that completed, if so, call the callback
        if (completionCount.incrementAndGet() == packets.length)
          done.run();
      });
    }
  }

  /**
   * Sends multiple packets to each of a collection of multiple receivers and, if required,
   * synchronizes the completion callback of each receiver's iteration to only call the provided
   * callback after all iterations themselves completed
   * @param receivers Collection of target receivers to send to
   * @param done Optional callback, called when all packets are done
   * @param sendingRoutine Routine used to tend out packets to a single receiver of the
   *                       collection, taking the receiver and a completion callback
   */
  protected<R> void sendPacketsToReceivers(Collection<R> receivers, @Nullable Runnable done, UnsafeBiConsumer<R, @Nullable Runnable> sendingRoutine) throws Exception {
    // No callback required, just perform an iteration invocation
    if (done == null) {
      for (R receiver : receivers)
        sendingRoutine.accept(receiver, null);
      return;
    }

    AtomicInteger completionCount = new AtomicInteger(0);

    for (R receiver : receivers) {
      // Check if it's the last iteration that completed, if so, call the callback
      sendingRoutine.accept(receiver, () -> {
        if (completionCount.incrementAndGet() == receivers.size())
          done.run();
      });
    }
  }

  //=========================================================================//
  //                       IPacketCommunicator Sending                       //
  //=========================================================================//

  /**
   * Send a new packet to a packet receiver
   * @param parameter Parameter to construct the packet from
   * @param receiver Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToReceiver(T parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    if (requiresViewer)
      return CommunicatorResult.VIEWER_REQUIRED;

    try {
      Object packet = createBasePacket(parameter);

      sendPacketsToReceiver(receiver, done, packet);

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Send a new packet to multiple packet receivers
   * @param parameter Parameter to construct the packet from
   * @param receivers Receivers of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToReceivers(T parameter, Collection<? extends IPacketReceiver> receivers, @Nullable Runnable done) {
    if (requiresViewer)
      return CommunicatorResult.VIEWER_REQUIRED;

    try {
      Object packet = createBasePacket(parameter);

      sendPacketsToReceivers(receivers, done, (receiver, subDone) -> {
        sendPacketsToReceiver(receiver, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Send a new packet to a customizable viewer
   * @param parameter Parameter to construct the packet from
   * @param viewer Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToViewer(T parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    try {
      Object packet = createBasePacket(parameter);

      try {
        personalizeBasePacket(packet, parameter, viewer);
      } catch (UnsupportedOperationException ignored) {}

      sendPacketsToReceiver(viewer, done, packet);

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Send a new packet to multiple customizable viewers
   * @param parameter Parameter to construct the packet from
   * @param viewers Receivers of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToViewers(T parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    try {
      Object packet = createBasePacket(parameter);

      sendPacketsToReceivers(viewers, done, (viewer, subDone) -> {
        try {
          personalizeBasePacket(packet, parameter, viewer);
        } catch (UnsupportedOperationException ignored) {}

        sendPacketsToReceiver(viewer, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Send a new packet to a player
   * @param parameter Parameter to construct the packet from
   * @param player Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToPlayer(T parameter, Player player, @Nullable Runnable done) {
    return sendToViewer(parameter, interceptor.getPlayerAsViewer(player), done);
  }

  /**
   * Send a new packet to multiple players
   * @param parameter Parameter to construct the packet from
   * @param players Receivers of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToPlayers(T parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    try {
      Object packet = createBasePacket(parameter);

      sendPacketsToReceivers(players, done, (player, subDone) -> {
        ICustomizableViewer viewer = interceptor.getPlayerAsViewer(player);

        try {
          personalizeBasePacket(packet, parameter, viewer);
        } catch (UnsupportedOperationException ignored) {}

        sendPacketsToReceiver(viewer, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }
}

