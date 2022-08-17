package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.TitleParameter;
import me.blvckbytes.bblibreflect.handle.*;
import me.blvckbytes.bblibutil.Triple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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

  //=========================================================================//
  //                                 Sending                                 //
  //=========================================================================//

  @Override
  public CommunicatorResult sendToViewer(TitleParameter parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    try {
      Triple<Object, Object, Object> packets = createPackets(parameter);

      personalizeTitles(packets, parameter, viewer);
      sendPacketsToReceiver(viewer, done, packets.getA(), packets.getB(), packets.getC());

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public CommunicatorResult sendToViewers(TitleParameter parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    try {
      Triple<Object, Object, Object> packets = createPackets(parameter);

      sendPacketsToReceivers(viewers, done, (viewer, subDone) -> {
        personalizeTitles(packets, parameter, viewer);
        sendPacketsToReceiver(viewer, subDone, packets.getA(), packets.getB(), packets.getC());
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public CommunicatorResult sendToPlayer(TitleParameter parameter, Player player, @Nullable Runnable done) {
    return sendToViewer(parameter, interceptor.getPlayerAsViewer(player), done);
  }

  @Override
  public CommunicatorResult sendToPlayers(TitleParameter parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    try {
      Triple<Object, Object, Object> packets = createPackets(parameter);

      sendPacketsToReceivers(players, done, (player, subDone) -> {
        ICustomizableViewer viewer = interceptor.getPlayerAsViewer(player);
        personalizeTitles(packets, parameter, viewer);
        sendPacketsToReceiver(viewer, subDone, packets.getA(), packets.getB(), packets.getC());
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Applies personalized titles to packets
   * @param packets Packets to apply to
   * @param parameter Parameter to use for personalization
   * @param viewer Viewer to personalize for
   */
  private void personalizeTitles(Triple<Object, Object, Object> packets, TitleParameter parameter, ICustomizableViewer viewer) throws Exception {
    // Create a personalized title- and subtitle component
    Object titleComponent = M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getTitle().toJson(viewer.cannotRenderHexColors()));
    Object subTitleComponent = M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getSubtitle().toJson(viewer.cannotRenderHexColors()));

    // Decide on which fields to apply to

    if (!isNewerTitles) {
      F_PO_TITLE__BASE_COMPONENT.set(packets.getB(), titleComponent);
      F_PO_TITLE__BASE_COMPONENT.set(packets.getC(), subTitleComponent);
    }

    else {
      F_CLB_TITLE__BASE_COMPONENT.set(packets.getB(), M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getTitle().toJson(viewer.cannotRenderHexColors())));
      F_CLB_SUBTITLE__BASE_COMPONENT.set(packets.getC(), M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getSubtitle().toJson(viewer.cannotRenderHexColors())));
    }
  }

  /**
   * Create the packet bases with all parameters set except the base
   * component itself (has to be personalized).
   * @param parameter Parameter to extract the information from
   * @return Constructed packet
   */
  private Triple<Object, Object, Object> createPackets(TitleParameter parameter) throws Exception {
    Object setTimes, setTitle, setSubtitle;

    // Older version, create three different instances of the same packet
    if (!isNewerTitles) {
      setTimes = helper.createEmptyPacket(C_PO_TITLE);
      setTitle = helper.createEmptyPacket(C_PO_TITLE);
      setSubtitle = helper.createEmptyPacket(C_PO_TITLE);

      F_PO_TITLE__ENUM_TITLE_ACTION.set(setTimes, E_ENUM_TITLE_ACTION.getByOrdinal(3));
      F_PO_TITLE__FADE_IN.set(setTimes, parameter.getFadeIn());
      F_PO_TITLE__STAY.set(setTimes, parameter.getDuration());
      F_PO_TITLE__FADE_OUT.set(setTimes, parameter.getFadeOut());

      F_PO_TITLE__ENUM_TITLE_ACTION.set(setTitle, E_ENUM_TITLE_ACTION.getByOrdinal(0));
      F_PO_TITLE__ENUM_TITLE_ACTION.set(setTimes, E_ENUM_TITLE_ACTION.getByOrdinal(1));
    }

    // Newer version, create three different packets
    else {
      setTimes = helper.createEmptyPacket(C_CLB_TITLES_ANIMATION);
      setTitle = helper.createEmptyPacket(C_CLB_TITLE);
      setSubtitle = helper.createEmptyPacket(C_CLB_SUBTITLE);

      F_CLB_TITLES_ANIMATION__FADE_IN.set(setTimes, parameter.getFadeIn());
      F_CLB_TITLES_ANIMATION__STAY.set(setTimes, parameter.getDuration());
      F_CLB_TITLES_ANIMATION__FADE_OUT.set(setTimes, parameter.getFadeOut());
    }

    return new Triple<>(setTimes, setTitle, setSubtitle);
  }

  @Override
  public Class<TitleParameter> getParameterType() {
    return TitleParameter.class;
  }
}
