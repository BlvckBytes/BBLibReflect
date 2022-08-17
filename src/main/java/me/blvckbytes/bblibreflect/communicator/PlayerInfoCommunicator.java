package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.PlayerInfoParameter;
import me.blvckbytes.bblibreflect.handle.*;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/17/2022

  Communicates sending player information, which updates the tab-list
  and enables or disables players from being rendered.

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
public class PlayerInfoCommunicator extends APacketCommunicator<PlayerInfoParameter> {

  private final EnumHandle E_ENUM_PLAYER_INFO_ACTION, E_ENUM_GAME_MODE;

  private final ClassHandle C_PO_PLAYER_INFO;

  private final FieldHandle F_PO_PLAYER_INFO__ENUM, F_PO_PLAYER_INFO__LIST, F_PLAYER_INFO_DATA__COMPONENT;

  private final MethodHandle M_CHAT_SERIALIZER__FROM_JSON, M_CRAFT_PLAYER__GET_PROFILE;

  private final ConstructorHandle CT_PLAYER_INFO_DATA;

  public PlayerInfoCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor);

    ClassHandle C_CHAT_SERIALIZER  = helper.getClass(RClass.CHAT_SERIALIZER);
    ClassHandle C_CRAFT_PLAYER     = helper.getClass(RClass.CRAFT_PLAYER);
    ClassHandle C_PLAYER_INFO_DATA = helper.getClass(RClass.PLAYER_INFO_DATA);
    ClassHandle C_ENUM_GAME_MODE   = helper.getClass(RClass.ENUM_GAME_MODE);
    ClassHandle C_BASE_COMPONENT   = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);

    C_PO_PLAYER_INFO          = helper.getClass(RClass.PACKET_O_PLAYER_INFO);
    E_ENUM_PLAYER_INFO_ACTION = helper.getClass(RClass.ENUM_PLAYER_INFO_ACTION).asEnum();
    E_ENUM_GAME_MODE          = helper.getClass(RClass.ENUM_GAME_MODE).asEnum();

    M_CHAT_SERIALIZER__FROM_JSON = C_CHAT_SERIALIZER.locateMethod().withParameters(JsonElement.class).withReturnType(C_BASE_COMPONENT, false, Assignability.TYPE_TO_TARGET).withStatic(true).required();
    M_CRAFT_PLAYER__GET_PROFILE  = C_CRAFT_PLAYER.locateMethod().withName("getProfile").withReturnType(GameProfile.class).required();

    F_PO_PLAYER_INFO__ENUM = C_PO_PLAYER_INFO.locateField().withType(E_ENUM_PLAYER_INFO_ACTION).required();
    F_PO_PLAYER_INFO__LIST = C_PO_PLAYER_INFO.locateField().withType(List.class).withGeneric(C_PLAYER_INFO_DATA).required();
    F_PLAYER_INFO_DATA__COMPONENT = C_PLAYER_INFO_DATA.locateField().withType(C_BASE_COMPONENT).required();

    CT_PLAYER_INFO_DATA = C_PLAYER_INFO_DATA.locateConstructor().withParameters(GameProfile.class, int.class).withParameters(C_ENUM_GAME_MODE, C_BASE_COMPONENT).required();
  }

  @Override
  public CommunicatorResult sendToViewer(PlayerInfoParameter parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    try {
      Tuple<Object, List<Object>> packetT = createPacket(parameter);
      Object packet = packetT.getA();
      List<Object> playerInfoList = packetT.getB();

      personalizePlayerInfoList(playerInfoList, parameter, viewer);
      viewer.sendPacket(packet, done);
      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public CommunicatorResult sendToViewers(PlayerInfoParameter parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    try {
      Tuple<Object, List<Object>> packetT = createPacket(parameter);
      Object packet = packetT.getA();
      List<Object> playerInfoList = packetT.getB();

      sendPacketsToReceivers(viewers, done, (viewer, subDone) -> {
        personalizePlayerInfoList(playerInfoList, parameter, viewer);
        viewer.sendPacket(packet, subDone);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public CommunicatorResult sendToPlayer(PlayerInfoParameter parameter, Player player, @Nullable Runnable done) {
    return sendToViewer(parameter, interceptor.getPlayerAsViewer(player), done);
  }

  @Override
  public CommunicatorResult sendToPlayers(PlayerInfoParameter parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    try {
      Tuple<Object, List<Object>> packetT = createPacket(parameter);
      Object packet = packetT.getA();
      List<Object> playerInfoList = packetT.getB();

      sendPacketsToReceivers(players, done, (player, subDone) -> {
        ICustomizableViewer viewer = interceptor.getPlayerAsViewer(player);
        personalizePlayerInfoList(playerInfoList, parameter, viewer);
        viewer.sendPacket(packet, subDone);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  /**
   * Personalize a list of player info objects by setting a personalized
   * base component value for the provided viewer on each entry.
   * @param playerInfoList Player info object list to personalize
   * @param parameter Parameter to use for personalization
   * @param viewer Viewer to personalize for
   */
  private void personalizePlayerInfoList(List<Object> playerInfoList, PlayerInfoParameter parameter, ICustomizableViewer viewer) throws Exception {
    // Personalize all player info entries for the current viewer
    for (int i = 0; i < parameter.getEntries().size(); i++) {
      PlayerInfoParameter.Entry entry = parameter.getEntries().get(i);
      Object playerInfo = playerInfoList.get(i);

      // Personalize the current entry's component
      JsonObject json = entry.getName() == null ? null : entry.getName().toJson(viewer.cannotRenderHexColors());
      F_PLAYER_INFO_DATA__COMPONENT.set(playerInfo, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, json));
    }
  }

  /**
   * Create the packet base with a list of player info objects which
   * have all data set except the base component itself (has to be personalized).
   * @param parameter Parameter to extract the information from
   * @return Constructed packet
   */
  private Tuple<Object, List<Object>> createPacket(PlayerInfoParameter parameter) throws Exception {
    // Create a new packet instance and set the action, it will be the same for all viewers
    Object info = helper.createEmptyPacket(C_PO_PLAYER_INFO);
    F_PO_PLAYER_INFO__ENUM.set(info, E_ENUM_PLAYER_INFO_ACTION.getByCopy(parameter.getAction()));

    // Create a new list of PlayerInfoData and set it's ref, it will also remain constant
    List<Object> playerInfoList = new ArrayList<>();
    F_PO_PLAYER_INFO__LIST.set(info, playerInfoList);

    // Each entry maps to a player info data object
    for (PlayerInfoParameter.Entry entry : parameter.getEntries()) {

      // Create the object with a null-component which is to be personalized later
      Object data = CT_PLAYER_INFO_DATA.newInstance(
        entry.resolveGameProfile(M_CRAFT_PLAYER__GET_PROFILE),
        entry.resolveLatency(interceptor),
        E_ENUM_GAME_MODE.getByCopy(entry.resolveGameMode()),
        null
      );

      playerInfoList.add(data);
    }

    return new Tuple<>(info, playerInfoList);
  }

  @Override
  public Class<PlayerInfoParameter> getParameterType() {
    return PlayerInfoParameter.class;
  }
}
