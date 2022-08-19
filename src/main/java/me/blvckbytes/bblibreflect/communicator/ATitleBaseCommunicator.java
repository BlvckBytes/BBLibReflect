package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.ServerVersion;
import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import me.blvckbytes.bblibreflect.handle.EnumHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/19/2022

  As base packet creation is the same along all title communicators,
  this base class extracts common logic.

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
public abstract class ATitleBaseCommunicator<T extends ICommunicatorParameter> extends APacketCommunicator<T> {

  private final EnumHandle E_ENUM_TITLE_ACTION;
  private final FieldHandle F_PO_TITLE__ENUM_TITLE_ACTION;

  protected final boolean isNewer;

  public ATitleBaseCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    RClass newerClass
  ) throws Exception {
    super(logger, helper, interceptor, true, () -> (
      ServerVersion.getCurrent().greaterThan(ServerVersion.V1_16_5) ?
        helper.getClass(newerClass) :
        helper.getClass(RClass.PACKET_O_TITLE)
    ));

    isNewer = ServerVersion.getCurrent().greaterThan(ServerVersion.V1_16_5);

    if (isNewer) {
      E_ENUM_TITLE_ACTION           = null;
      F_PO_TITLE__ENUM_TITLE_ACTION = null;
    } else {
      E_ENUM_TITLE_ACTION           = helper.getClass(RClass.ENUM_TITLE_ACTION).asEnum();
      F_PO_TITLE__ENUM_TITLE_ACTION = getPacketType().locateField().withType(E_ENUM_TITLE_ACTION).required();
    }
  }

  @Override
  protected Object createBasePacket(T parameter) throws Exception {
    Object packet = createPacket();

    if (F_PO_TITLE__ENUM_TITLE_ACTION != null)
      F_PO_TITLE__ENUM_TITLE_ACTION.set(packet, E_ENUM_TITLE_ACTION.getByOrdinal(0));

    return packet;
  }
}
