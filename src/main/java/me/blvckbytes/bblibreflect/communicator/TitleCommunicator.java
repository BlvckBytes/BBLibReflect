package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.TitleParameter;
import me.blvckbytes.bblibreflect.handle.*;
import me.blvckbytes.bblibutil.logger.ILogger;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/16/2022

  Communicates sending title screens.

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
public class TitleCommunicator extends APacketCommunicator<TitleParameter> {

  private final EnumHandle E_ENUM_TITLE_ACTION;

  private final ClassHandle C_PO_TITLE, C_CLB_TITLES_ANIMATION, C_CLB_TITLE, C_CLB_SUBTITLE;

  private final FieldHandle F_CLB_TITLE__BASE_COMPONENT, F_CLB_SUBTITLE__BASE_COMPONENT,
    F_CLB_TITLES_ANIMATION__FADE_IN, F_CLB_TITLES_ANIMATION__STAY, F_CLB_TITLES_ANIMATION__FADE_OUT,
    F_PO_TITLE__BASE_COMPONENT, F_PO_TITLE__ENUM_TITLE_ACTION, F_PO_TITLE__FADE_IN, F_PO_TITLE__STAY, F_PO_TITLE__FADE_OUT;

  private final MethodHandle M_CHAT_SERIALIZER__FROM_JSON;

  private final boolean isNewerTitles;

  public TitleCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor);

    ClassHandle C_CHAT_SERIALIZER  = helper.getClass(RClass.CHAT_SERIALIZER);
    ClassHandle C_BASE_COMPONENT   = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);

    C_PO_TITLE                = helper.getClassOptional(RClass.PACKET_O_TITLE);
    E_ENUM_TITLE_ACTION       = helper.getEnumOptional(RClass.ENUM_TITLE_ACTION);
    C_CLB_TITLES_ANIMATION    = helper.getClassOptional(RClass.CLIENTBOUND_TITLES_ANIMATION);
    C_CLB_TITLE               = helper.getClassOptional(RClass.CLIENTBOUND_TITLE_SET);
    C_CLB_SUBTITLE            = helper.getClassOptional(RClass.CLIENTBOUND_SUBTITLE_SET);

    M_CHAT_SERIALIZER__FROM_JSON = C_CHAT_SERIALIZER.locateMethod().withParameters(JsonElement.class).withReturnType(C_BASE_COMPONENT, false, Assignability.TYPE_TO_TARGET).withStatic(true).required();

    isNewerTitles = C_CLB_TITLE != null && C_CLB_SUBTITLE != null && C_CLB_TITLES_ANIMATION != null;

    // PacketPlayOut (older)
    if (C_PO_TITLE != null) {
      F_PO_TITLE__BASE_COMPONENT    = C_PO_TITLE.locateField().withType(C_BASE_COMPONENT).required();
      F_PO_TITLE__ENUM_TITLE_ACTION = C_PO_TITLE.locateField().withType(E_ENUM_TITLE_ACTION).required();
      F_PO_TITLE__FADE_IN           = C_PO_TITLE.locateField().withType(int.class).required();
      F_PO_TITLE__STAY              = C_PO_TITLE.locateField().withType(int.class).withSkip(1).required();
      F_PO_TITLE__FADE_OUT          = C_PO_TITLE.locateField().withType(int.class).withSkip(2).required();

      F_CLB_TITLE__BASE_COMPONENT = null;
      F_CLB_SUBTITLE__BASE_COMPONENT = null;
      F_CLB_TITLES_ANIMATION__FADE_IN = null;
      F_CLB_TITLES_ANIMATION__STAY = null;
      F_CLB_TITLES_ANIMATION__FADE_OUT = null;
    }

    // Clientbound packets (newer)
    else if (isNewerTitles) {
      F_CLB_TITLE__BASE_COMPONENT      = C_CLB_TITLE.locateField().withType(C_BASE_COMPONENT).required();
      F_CLB_SUBTITLE__BASE_COMPONENT   = C_CLB_SUBTITLE.locateField().withType(C_BASE_COMPONENT).required();
      F_CLB_TITLES_ANIMATION__FADE_IN  = C_CLB_TITLES_ANIMATION.locateField().withType(int.class).required();
      F_CLB_TITLES_ANIMATION__STAY     = C_CLB_TITLES_ANIMATION.locateField().withType(int.class).withSkip(1).required();
      F_CLB_TITLES_ANIMATION__FADE_OUT = C_CLB_TITLES_ANIMATION.locateField().withType(int.class).withSkip(2).required();

      F_PO_TITLE__BASE_COMPONENT = null;
      F_PO_TITLE__ENUM_TITLE_ACTION = null;
      F_PO_TITLE__FADE_IN = null;
      F_PO_TITLE__STAY = null;
      F_PO_TITLE__FADE_OUT = null;
    }

    else
      throw new IllegalStateException("Couldn't find neither newer nor older title packets.");
  }

  @Override
  public void sendParameterized(List<IPacketReceiver> receivers, TitleParameter parameter) {
    // TODO: Reuse packets properly
    for (IPacketReceiver receiver : receivers)
      sendParameterized(receiver, parameter);
  }

  @Override
  public void sendParameterized(IPacketReceiver receiver, TitleParameter parameter) {
    ICustomizableViewer viewer = asViewer(receiver);

    try {
      Object setTimes, setTitle, setSubtitle;

      // Older version, create three different instances of the same packet
      if (!isNewerTitles) {
        setTimes = helper.createEmptyPacket(C_PO_TITLE);
        setTitle = helper.createEmptyPacket(C_PO_TITLE);
        setSubtitle = helper.createEmptyPacket(C_PO_TITLE);

        // 0 TITLE, 1 SUBTITLE, 2 ACTIONBAR, 3 TIMES, 4 CLEAR, 5 RESET

        F_PO_TITLE__ENUM_TITLE_ACTION.set(setTimes, E_ENUM_TITLE_ACTION.getByOrdinal(3));
        F_PO_TITLE__FADE_IN.set(setTimes, parameter.getFadeIn());
        F_PO_TITLE__STAY.set(setTimes, parameter.getDuration());
        F_PO_TITLE__FADE_OUT.set(setTimes, parameter.getFadeOut());

        F_PO_TITLE__ENUM_TITLE_ACTION.set(setTitle, E_ENUM_TITLE_ACTION.getByOrdinal(0));
        F_PO_TITLE__BASE_COMPONENT.set(setTitle, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getTitle().toJson(viewer.cannotRenderHexColors())));

        F_PO_TITLE__ENUM_TITLE_ACTION.set(setTimes, E_ENUM_TITLE_ACTION.getByOrdinal(1));
        F_PO_TITLE__BASE_COMPONENT.set(setSubtitle, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getSubtitle().toJson(viewer.cannotRenderHexColors())));
      }

      // Newer version, create three different packets
      else {
        setTimes = helper.createEmptyPacket(C_CLB_TITLES_ANIMATION);
        setTitle = helper.createEmptyPacket(C_CLB_TITLE);
        setSubtitle = helper.createEmptyPacket(C_CLB_SUBTITLE);

        F_CLB_TITLES_ANIMATION__FADE_IN.set(setTimes, parameter.getFadeIn());
        F_CLB_TITLES_ANIMATION__STAY.set(setTimes, parameter.getDuration());
        F_CLB_TITLES_ANIMATION__FADE_OUT.set(setTimes, parameter.getFadeOut());

        F_CLB_TITLE__BASE_COMPONENT.set(setTitle, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getTitle().toJson(viewer.cannotRenderHexColors())));
        F_CLB_SUBTITLE__BASE_COMPONENT.set(setSubtitle, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getSubtitle().toJson(viewer.cannotRenderHexColors())));
      }

      viewer.sendPacket(setTimes, null);
      viewer.sendPacket(setTitle, null);
      viewer.sendPacket(setSubtitle, null);
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
