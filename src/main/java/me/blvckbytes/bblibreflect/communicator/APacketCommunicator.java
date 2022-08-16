package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
public abstract class APacketCommunicator<T> {

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
  }

  // TODO: Packet parsing

  /**
   * Sends a new packet of the communicator's managed type
   * @param receivers Receivers of the packet
   * @param parameter Parameter used to initialize the packet
   */
  public abstract void sendParameterized(List<IPacketReceiver> receivers, T parameter);

  /**
   * Sends a new packet of the communicator's managed type
   * @param receiver Receiver of the packet
   * @param parameter Parameter used to initialize the packet
   */
  public abstract void sendParameterized(IPacketReceiver receiver, T parameter);

  /**
   * Sends a new packet of the communicator's managed type
   * @param receivers Receivers of the packet
   * @param parameter Parameter used to initialize the packet
   */
  public void sendParameterized(Collection<Player> receivers, T parameter) {
    this.sendParameterized(
      receivers.stream()
        .map(interceptor::getPlayerAsViewer)
        .collect(Collectors.toList()),
      parameter
    );
  }

  /**
   * Sends a new packet of the communicator's managed type
   * @param receiver Receiver of the packet
   * @param parameter Parameter used to initialize the packet
   */
  public void sendParameterized(Player receiver, T parameter) {
    this.sendParameterized(
      interceptor.getPlayerAsViewer(receiver),
      parameter
    );
  }

  /**
   * Require the packet receiver to be a viewer
   * @param receiver Packet receiver to interpret as a viewer
   * @return Viewer instance
   */
  protected ICustomizableViewer asViewer(IPacketReceiver receiver) {
    if (!(receiver instanceof ICustomizableViewer))
      throw new IllegalStateException("This operation requires the receiver to be an ICustomizableViewer.");
    return ((ICustomizableViewer) receiver);
  }
}

