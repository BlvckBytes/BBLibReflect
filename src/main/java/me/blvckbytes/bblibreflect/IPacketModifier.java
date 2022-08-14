package me.blvckbytes.bblibreflect;

import org.jetbrains.annotations.Nullable;

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

  Object modifyIncoming(IPacketReceiver sender, Object incoming);

  Object modifyOutgoing(IPacketReceiver receiver, Object outgoing);

}
