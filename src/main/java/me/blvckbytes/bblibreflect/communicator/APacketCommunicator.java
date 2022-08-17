package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibutil.UnsafeBiConsumer;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  The base of all packet communicator implementations.

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
public abstract class APacketCommunicator<T> implements IPacketCommunicator<T> {

  protected final ILogger logger;
  protected final IReflectionHelper helper;
  protected final IPacketInterceptor interceptor;

  public APacketCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor
  ) {
    this.logger = logger;
    this.helper = helper;
    this.interceptor = interceptor;

    interceptor.registerCommunicator(this);
  }

  /**
   * Get the type of parameter this communicator accepts
   */
  public abstract Class<T> getParameterType();

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
}

