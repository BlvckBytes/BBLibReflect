package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.TitleSubtitleParameter;
import me.blvckbytes.bblibreflect.handle.ConstructorHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/16/2022

  Communicates sending title screen subtitles.

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
public class TitleSubtitleCommunicator extends ATitleBaseCommunicator<TitleSubtitleParameter> {

  private final FieldHandle F_CLB_SUBTITLE__BASE_COMPONENT, F_PO_SUBTITLE__BASE_COMPONENT;
  private final ConstructorHandle CTOR_CLB_SUBTITLE;

  public TitleSubtitleCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, RClass.CLIENTBOUND_SUBTITLE_SET);

    if (isNewer) {
      F_CLB_SUBTITLE__BASE_COMPONENT = getPacketType().locateField().withType(C_BASE_COMPONENT).required();
      CTOR_CLB_SUBTITLE = getPacketType().locateConstructor().withParameters(C_BASE_COMPONENT).optional();
      F_PO_SUBTITLE__BASE_COMPONENT  = null;
    }

    else {
      F_PO_SUBTITLE__BASE_COMPONENT  = getPacketType().locateField().withType(C_BASE_COMPONENT).withSkip(1).required();
      CTOR_CLB_SUBTITLE = null;
      F_CLB_SUBTITLE__BASE_COMPONENT = null;
    }
  }

  @Override
  protected Object createBasePacket(TitleSubtitleParameter parameter) throws Exception {
    if (CTOR_CLB_SUBTITLE != null)
      return CTOR_CLB_SUBTITLE.newInstance(componentToBaseComponent(parameter.getSubtitle(), null));

    return super.createPacket();
  }

  @Override
  protected void personalizeBasePacket(Object packet, TitleSubtitleParameter parameter, ICustomizableViewer viewer) throws Exception {
    Object subtitleComponent = componentToBaseComponent(parameter.getSubtitle(), viewer);

    if (F_PO_SUBTITLE__BASE_COMPONENT != null)
      F_PO_SUBTITLE__BASE_COMPONENT.set(packet, subtitleComponent);

    else if (F_CLB_SUBTITLE__BASE_COMPONENT != null)
      F_CLB_SUBTITLE__BASE_COMPONENT.set(packet, subtitleComponent);
  }

  @Override
  public Class<TitleSubtitleParameter> getParameterType() {
    return TitleSubtitleParameter.class;
  }

  @Override
  public @Nullable TitleSubtitleParameter parseOutgoing(Object packet) {
    // TODO: Implement
    throw new UnsupportedOperationException();
  }
}
