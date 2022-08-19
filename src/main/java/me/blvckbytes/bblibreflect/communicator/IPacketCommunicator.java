package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/20/2022

  Defines the publicly available functionality of any packet communicator.

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
public interface IPacketCommunicator<T extends ICommunicatorParameter> {

  /**
   * Get the type of parameter this communicator accepts
   */
  Class<T> getParameterType();

  /**
   * Get the type of packet this communicator manages
   */
  ClassHandle getPacketType();

}
