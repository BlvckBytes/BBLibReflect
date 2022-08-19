package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.TitleTitleParameter;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/16/2022

  Communicates sending title screen titles.

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
@AutoConstruct
public class TitleTitleCommunicator extends ATitleBaseCommunicator<TitleTitleParameter> {

  private final FieldHandle F_CLB_TITLE__BASE_COMPONENT, F_PO_TITLE__BASE_COMPONENT;

  public TitleTitleCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, RClass.CLIENTBOUND_TITLE_SET);

    if (isNewer) {
      F_CLB_TITLE__BASE_COMPONENT = getPacketType().locateField().withType(C_BASE_COMPONENT).required();
      F_PO_TITLE__BASE_COMPONENT  = null;
    }

    else {
      F_PO_TITLE__BASE_COMPONENT  = getPacketType().locateField().withType(C_BASE_COMPONENT).required();
      F_CLB_TITLE__BASE_COMPONENT = null;
    }
  }

  @Override
  protected void personalizeBasePacket(Object packet, TitleTitleParameter parameter, ICustomizableViewer viewer) throws Exception {
    Object titleComponent = componentToBaseComponent(parameter.getTitle(), viewer);

    if (F_PO_TITLE__BASE_COMPONENT != null)
      F_PO_TITLE__BASE_COMPONENT.set(packet, titleComponent);

    else if (F_CLB_TITLE__BASE_COMPONENT != null)
      F_CLB_TITLE__BASE_COMPONENT.set(packet, titleComponent);
  }

  @Override
  public Class<TitleTitleParameter> getParameterType() {
    return TitleTitleParameter.class;
  }

  @Override
  public @Nullable TitleTitleParameter parseOutgoing(Object packet) {
    // TODO: Implement
    throw new UnsupportedOperationException();
  }
}
