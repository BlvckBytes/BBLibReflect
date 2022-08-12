package me.blvckbytes.bblibreflect;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Represents a class that can modify in- and outgoing packets
  and will be registered within a chain of modifiers

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
public interface IPacketModifier {

  /**
   * Interception method to modify incoming packets
   * @param sender UUID of the player who's client sent the packet (can be null if not yet connected)
   * @param ps Source of this packet
   * @param incoming Incoming packet
   * @return Modified incoming packet, null to terminate the packet
   */
  Object modifyIncoming(UUID sender, PacketSource ps, Object incoming);

  /**
   * Interception method to modify outgoing packets
   * @param receiver UUID of the player who's client will receive the packet (can be null if not yet connected)
   * @param nm NetworkManager corresponding to the requesting client
   * @param outgoing Outgoing packet
   * @return Modified outgoing packet, null to terminate the packet
   */
  Object modifyOutgoing(UUID receiver, Object nm, Object outgoing);
}
