package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibutil.UnsafeBiConsumer;
import me.blvckbytes.bblibutil.UnsafeSupplier;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  The base of all outgoing packet communicator implementations. Implements
  the communicator API in a very generally applicative way so inheritors
  don't have to, but may.

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
public abstract class APacketOutCommunicator<T extends ICommunicatorParameter> extends APacketCommunicator<T> implements IPacketOutCommunicator<T> {

  public APacketOutCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    UnsafeSupplier<ClassHandle> packetType
  ) throws Exception {
    this(logger, helper, interceptor, requiresViewer, packetType.get());
  }

  public APacketOutCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    ClassHandle packetType
  ) throws Exception {
    super(logger, helper, interceptor, requiresViewer, packetType);

    interceptor.getPacketCommunicatorRegistry().registerCommunicator(this);
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
   * @param sendingRoutine Routine used to send out packets to a single receiver of the
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

  /**
   * Send a new packet to a packet receiver
   * @param parameter Parameter to construct the packet from
   * @param receiver Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult sendToReceiver(T parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    if (isRequiresViewer())
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
    if (isRequiresViewer())
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

