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

  Defines the publicly available functionality of an outgoing packet communicator.

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
public interface IPacketOutCommunicator<T extends ICommunicatorParameter> extends IPacketCommunicator<T> {

  /**
   * Send a new packet to a packet receiver
   * @param parameter Parameter to construct the packet from
   * @param receiver Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult sendToReceiver(T parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    return CommunicatorResult.VIEWER_REQUIRED;
  }

  /**
   * Send a new packet to multiple packet receivers
   * @param parameter Parameter to construct the packet from
   * @param receivers Receivers of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult sendToReceivers(T parameter, Collection<? extends IPacketReceiver> receivers, @Nullable Runnable done) {
    return CommunicatorResult.VIEWER_REQUIRED;
  }

  /**
   * Send a new packet to a customizable viewer
   * @param parameter Parameter to construct the packet from
   * @param viewer Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult sendToViewer(T parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Send a new packet to multiple customizable viewers
   * @param parameter Parameter to construct the packet from
   * @param viewers Receivers of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult sendToViewers(T parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Send a new packet to a player
   * @param parameter Parameter to construct the packet from
   * @param player Receiver of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult sendToPlayer(T parameter, Player player, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Send a new packet to multiple players
   * @param parameter Parameter to construct the packet from
   * @param players Receivers of the packet
   * @param done Optional completion callback
   * @return Result of the operation
   */
  default CommunicatorResult sendToPlayers(T parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    throw new UnsupportedOperationException();
  }

  /**
   * Parses an existing packet into the communicator's parameter representation
   * @param packet Packet to parse
   * @return Parsed parameter, null if the packet didn't match
   * the communicators managed packet class
   */
  @Nullable T parseOutgoing(Object packet);
}
