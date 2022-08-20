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

  The base of all incoming packet communicator implementations. Implements
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
public abstract class APacketInCommunicator<T extends ICommunicatorParameter> extends APacketCommunicator<T> implements IPacketInCommunicator<T> {

  public APacketInCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    UnsafeSupplier<ClassHandle> packetType
  ) throws Exception {
    super(logger, helper, interceptor, requiresViewer, packetType.get());
  }

  public APacketInCommunicator(
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
   * Central point of redirecting packets to be received to the central API
   * @param receiver Sending receiver
   * @param parameter Parameter which parameterized the packet
   * @param packet Target packet
   * @param done Optional callback, called on completion
   */
  protected void receivePacket(IPacketReceiver receiver, T parameter, Object packet, @Nullable Runnable done) {
    receiver.receivePacket(packet, done);
  }

  /**
   * Receives multiple packets from a given receiver and, if required, synchronizes
   * their completion callback to only invoke the provided callback after all packets completed
   * @param receiver Target receiver to receive from
   * @param parameter Parameter which parameterized the packet
   * @param done Optional callback, called when all packets are done
   * @param packets Packets to receive
   */
  protected void receivePacketsFromReceiver(IPacketReceiver receiver, T parameter, @Nullable Runnable done, Object... packets) {
    // No callback required, just perform a sending invocation
    if (done == null) {
      for (Object packet : packets)
        receivePacket(receiver, parameter, packet, null);
      return;
    }

    AtomicInteger completionCount = new AtomicInteger(0);

    for (Object packet : packets) {
      receivePacket(receiver, parameter, packet, () -> {
        // Check if it's the last packet that completed, if so, call the callback
        if (completionCount.incrementAndGet() == packets.length)
          done.run();
      });
    }
  }

  /**
   * Receives multiple packets from each of a collection of multiple receivers and, if required,
   * synchronizes the completion callback of each receiver's iteration to only call the provided
   * callback after all iterations themselves completed
   * @param receivers Collection of target receivers to receive from
   * @param done Optional callback, called when all packets are done
   * @param receivingRoutine Routine used to receive packets from a single receiver of the
   *                       collection, taking the receiver and a completion callback
   */
  protected<R> void receivePacketsFromReceivers(Collection<R> receivers, @Nullable Runnable done, UnsafeBiConsumer<R, @Nullable Runnable> receivingRoutine) throws Exception {
    // No callback required, just perform an iteration invocation
    if (done == null) {
      for (R receiver : receivers)
        receivingRoutine.accept(receiver, null);
      return;
    }

    AtomicInteger completionCount = new AtomicInteger(0);

    for (R receiver : receivers) {
      // Check if it's the last iteration that completed, if so, call the callback
      receivingRoutine.accept(receiver, () -> {
        if (completionCount.incrementAndGet() == receivers.size())
          done.run();
      });
    }
  }

  /**
   * Simulate receiving a packet from a packet receiver
   * @param parameter Parameter to construct the packet from
   * @param receiver Sender of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult receiveFromReceiver(T parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    if (isRequiresViewer())
      return CommunicatorResult.VIEWER_REQUIRED;

    try {
      Object packet = createBasePacket(parameter);

      receivePacketsFromReceiver(receiver, parameter, done, packet);

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Simulate receiving a packet from multiple packet receivers
   * @param parameter Parameter to construct the packet from
   * @param receivers Senders of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult receiveFromReceivers(T parameter, Collection<? extends IPacketReceiver> receivers, @Nullable Runnable done) {
    if (isRequiresViewer())
      return CommunicatorResult.VIEWER_REQUIRED;

    try {
      Object packet = createBasePacket(parameter);

      receivePacketsFromReceivers(receivers, done, (receiver, subDone) -> {
        receivePacketsFromReceiver(receiver, parameter, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Simulate receiving a packet from a customizable viewer
   * @param parameter Parameter to construct the packet from
   * @param viewer Sender of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult receiveFromViewer(T parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    try {
      Object packet = createBasePacket(parameter);

      try {
        personalizeBasePacket(packet, parameter, viewer);
      } catch (UnsupportedOperationException ignored) {}

      receivePacketsFromReceiver(viewer, parameter, done, packet);

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Simulate receiving a packet from multiple customizable viewers
   * @param parameter Parameter to construct the packet from
   * @param viewers Senders of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult receiveFromViewers(T parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    try {
      Object packet = createBasePacket(parameter);

      receivePacketsFromReceivers(viewers, done, (viewer, subDone) -> {
        try {
          personalizeBasePacket(packet, parameter, viewer);
        } catch (UnsupportedOperationException ignored) {}

        receivePacketsFromReceiver(viewer, parameter, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Simulate receiving a packet from a player
   * @param parameter Parameter to construct the packet from
   * @param player Sender of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  @Override
  public CommunicatorResult receiveFromPlayer(T parameter, Player player, @Nullable Runnable done) {
    return receiveFromViewer(parameter, interceptor.getPlayerAsViewer(player), done);
  }

  /**
   * Simulate receiving a packet from multiple players
   * @param parameter Parameter to construct the packet from
   * @param players Senders of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  public CommunicatorResult receiveFromPlayers(T parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    try {
      Object packet = createBasePacket(parameter);

      receivePacketsFromReceivers(players, done, (player, subDone) -> {
        ICustomizableViewer viewer = interceptor.getPlayerAsViewer(player);

        try {
          personalizeBasePacket(packet, parameter, viewer);
        } catch (UnsupportedOperationException ignored) {}

        receivePacketsFromReceiver(viewer, parameter, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }
}

