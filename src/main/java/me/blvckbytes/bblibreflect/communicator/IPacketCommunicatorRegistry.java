package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/17/2022

  Describes a central packet communicator registry where each implemented
  communicator may register itself to become available. This registry also
  implements the packet communicator interface itself and routes all requests
  to the communicator in charge of the requested parameter-type.

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
public interface IPacketCommunicatorRegistry extends IPacketCommunicator<ICommunicatorParameter> {

  /**
   * Register a new available packet communicator which the
   * registry can route it's requests to
   * @param communicator Communicator to register
   */
  void registerCommunicator(APacketCommunicator<? extends ICommunicatorParameter> communicator);

}
