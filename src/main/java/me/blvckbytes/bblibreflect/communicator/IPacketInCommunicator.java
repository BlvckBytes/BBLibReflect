package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/17/2022

  Defines the publicly available functionality of an incoming packet communicator.

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
public interface IPacketInCommunicator<T extends ICommunicatorParameter> extends IPacketCommunicator<T> {

  /**
   * Simulate receiving a packet from a packet receiver
   * @param parameter Parameter to construct the packet from
   * @param receiver Sender of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult receiveFromReceiver(T parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Simulate receiving a packet from multiple packet receivers
   * @param parameter Parameter to construct the packet from
   * @param receivers Senders of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult receiveFromReceivers(T parameter, Collection<? extends IPacketReceiver> receivers, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Simulate receiving a packet from a customizable viewer
   * @param parameter Parameter to construct the packet from
   * @param viewer Sender of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult receiveFromViewer(T parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Simulate receiving a packet from multiple customizable viewers
   * @param parameter Parameter to construct the packet from
   * @param viewers Senders of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult receiveFromViewers(T parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Simulate receiving a packet from a player
   * @param parameter Parameter to construct the packet from
   * @param player Sender of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult receiveFromPlayer(T parameter, Player player, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Simulate receiving a packet from multiple players
   * @param parameter Parameter to construct the packet from
   * @param players Senders of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult receiveFromPlayers(T parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Parses an existing packet into the communicator's parameter representation
   * @param packet Packet to parse
   * @return Parsed parameter, null if the packet didn't match
   * the communicators managed packet class
   */
  @Nullable T parseIncoming(Object packet);

  // TODO: Think about a patch(Object, Parameter) method

  /**
   * Get the type of parameter this communicator accepts
   */
  Class<T> getParameterType();
}
