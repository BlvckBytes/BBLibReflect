package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.TitleTimingsParameter;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/16/2022

  Communicates sending title screen timings.

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
public class TitleTimingsCommunicator extends ATitleBaseCommunicator<TitleTimingsParameter> {

  private final FieldHandle F_CLB_ANIMATION__FADE_IN, F_CLB_ANIMATION__DURATION,
    F_CLB_ANIMATION__FADE_OUT, F_PO_TITLE__FADE_IN, F_PO_TITLE__DURATION, F_PO_TITLE__FADE_OUT;

  public TitleTimingsCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, RClass.CLIENTBOUND_TITLES_ANIMATION);

    if (isNewer) {
      F_CLB_ANIMATION__FADE_IN  = getPacketClass().locateField().withType(int.class).required();
      F_CLB_ANIMATION__DURATION = getPacketClass().locateField().withType(int.class).withSkip(1).required();
      F_CLB_ANIMATION__FADE_OUT = getPacketClass().locateField().withType(int.class).withSkip(2).required();

      F_PO_TITLE__FADE_IN  = null;
      F_PO_TITLE__DURATION = null;
      F_PO_TITLE__FADE_OUT = null;
    }

    else {
      F_PO_TITLE__FADE_IN  = getPacketClass().locateField().withType(int.class).required();
      F_PO_TITLE__DURATION = getPacketClass().locateField().withType(int.class).required();
      F_PO_TITLE__FADE_OUT = getPacketClass().locateField().withType(int.class).required();

      F_CLB_ANIMATION__FADE_IN  = null;
      F_CLB_ANIMATION__DURATION = null;
      F_CLB_ANIMATION__FADE_OUT = null;
    }
  }

  @Override
  protected Object createBasePacket(TitleTimingsParameter parameter) throws Exception {
    Object packet = super.createBasePacket(parameter);

    if (isNewer) {
      F_CLB_ANIMATION__FADE_IN.set(packet, parameter.getFadeIn());
      F_CLB_ANIMATION__DURATION.set(packet, parameter.getDuration());
      F_CLB_ANIMATION__FADE_OUT.set(packet, parameter.getFadeOut());
    }

    else {
      F_PO_TITLE__FADE_IN.set(packet, parameter.getFadeIn());
      F_PO_TITLE__DURATION.set(packet, parameter.getDuration());
      F_PO_TITLE__FADE_OUT.set(packet, parameter.getFadeOut());
    }

    return packet;
  }

  @Override
  protected void personalizeBasePacket(Object packet, TitleTimingsParameter parameter, ICustomizableViewer viewer) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<TitleTimingsParameter> getParameterType() {
    return TitleTimingsParameter.class;
  }
}
