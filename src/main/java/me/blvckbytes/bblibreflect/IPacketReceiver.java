package me.blvckbytes.bblibreflect;

import com.google.gson.JsonElement;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/12/2022

  The very basic interface of any endpoint which can receive packets.

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
public interface IPacketReceiver {

  /**
   * Sends a batch of packets to this receiver
   * @param packets Packets to send
   */
  void sendPackets(Object... packets);

  /**
   * If available, this retrieves the receiver's UUID
   */
  @Nullable UUID getUuid();

  /**
   * Get the receiver's netty channel
   */
  Channel getChannel();

}
