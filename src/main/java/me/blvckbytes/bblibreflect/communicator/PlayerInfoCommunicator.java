package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.PlayerInfoParameter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.ConstructorHandle;
import me.blvckbytes.bblibreflect.handle.EnumHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
public class PlayerInfoCommunicator extends APacketOutCommunicator<PlayerInfoParameter> {

  private final EnumHandle E_ENUM_PLAYER_INFO_ACTION, E_ENUM_GAME_MODE;

  private final FieldHandle F_PO_PLAYER_INFO__ENUM, F_PO_PLAYER_INFO__LIST, F_PLAYER_INFO_DATA__COMPONENT;

  private final ConstructorHandle CT_PLAYER_INFO_DATA;

  public PlayerInfoCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, true, helper.getClass(RClass.PACKET_O_PLAYER_INFO));

    ClassHandle C_PLAYER_INFO_DATA = helper.getClass(RClass.PLAYER_INFO_DATA);
    ClassHandle C_ENUM_GAME_MODE   = helper.getClass(RClass.ENUM_GAME_MODE);
    ClassHandle C_BASE_COMPONENT   = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);

    E_ENUM_PLAYER_INFO_ACTION = helper.getClass(RClass.ENUM_PLAYER_INFO_ACTION).asEnum();
    E_ENUM_GAME_MODE          = helper.getClass(RClass.ENUM_GAME_MODE).asEnum();

    F_PO_PLAYER_INFO__ENUM = getPacketType().locateField().withType(E_ENUM_PLAYER_INFO_ACTION).required();
    F_PO_PLAYER_INFO__LIST = getPacketType().locateField().withType(List.class).withGeneric(C_PLAYER_INFO_DATA).required();
    F_PLAYER_INFO_DATA__COMPONENT = C_PLAYER_INFO_DATA.locateField().withType(C_BASE_COMPONENT).required();

    if (ServerVersion.getCurrent().greaterThanOrEqual(ServerVersion.V1_19)) {
      ClassHandle C_PROFILE_PUBLIC_KEY = helper.getClass(RClass.PROFILE_PUBLIC_KEY);
      ClassHandle C_PROFILE_PUBLIC_KEY__FIRST_INNER = C_PROFILE_PUBLIC_KEY.locateClass().required();

      CT_PLAYER_INFO_DATA = C_PLAYER_INFO_DATA.locateConstructor()
        .withParameters(GameProfile.class, int.class)
        .withParameters(C_ENUM_GAME_MODE, C_BASE_COMPONENT, C_PROFILE_PUBLIC_KEY__FIRST_INNER)
        .required();
    }

    else
      CT_PLAYER_INFO_DATA = C_PLAYER_INFO_DATA.locateConstructor().withParameters(GameProfile.class, int.class).withParameters(C_ENUM_GAME_MODE, C_BASE_COMPONENT).required();
  }

  @Override
  protected Object createBasePacket(PlayerInfoParameter parameter) throws Exception {
    // Create a new packet instance and set the action, it will be the same for all viewers
    Object packet = createPacket();
    F_PO_PLAYER_INFO__ENUM.set(packet, E_ENUM_PLAYER_INFO_ACTION.getByCopy(parameter.getAction()));

    // Create a new list of PlayerInfoData and set it's ref, it will also remain constant
    List<Object> playerInfoList = new ArrayList<>();
    F_PO_PLAYER_INFO__LIST.set(packet, playerInfoList);

    // Just a single entry
    if (parameter.getEntry() != null)
      playerInfoList.add(createBasePlayerInfoData(parameter.getEntry()));

      // Each entry maps to a player info data object
    else if (parameter.getEntries() != null) {
      for (PlayerInfoParameter.Entry entry : parameter.getEntries())
        playerInfoList.add(createBasePlayerInfoData(entry));
    }

    return packet;
  }

  @Override
  protected void personalizeBasePacket(Object packet, PlayerInfoParameter parameter, ICustomizableViewer viewer) throws Exception {
    List<?> playerInfoList = (List<?>) F_PO_PLAYER_INFO__LIST.get(packet);

    // Just a single entry
    if (parameter.getEntry() != null) {
      PlayerInfoParameter.Entry entry = parameter.getEntry();
      Object playerInfo = playerInfoList.get(0);

      // Personalize the current entry's component
      JsonObject json = entry.getName() == null ? null : entry.getName().toJson(viewer.cannotRenderHexColors());
      F_PLAYER_INFO_DATA__COMPONENT.set(playerInfo, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, json));
    }

    else if (parameter.getEntries() != null) {
      // Personalize all player info entries for the current viewer
      for (int i = 0; i < parameter.getEntries().size(); i++) {
        PlayerInfoParameter.Entry entry = parameter.getEntries().get(i);
        Object playerInfo = playerInfoList.get(i);

        // Personalize the current entry's component
        JsonObject json = entry.getName() == null ? null : entry.getName().toJson(viewer.cannotRenderHexColors());
        F_PLAYER_INFO_DATA__COMPONENT.set(playerInfo, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, json));
      }
    }
  }

  @Override
  public Class<PlayerInfoParameter> getParameterType() {
    return PlayerInfoParameter.class;
  }

  /**
   * Create a new base instance of the player info data by setting all
   * of it's properties except the personalized component (is set to null).
   * @param entry Entry to create from
   */
  private Object createBasePlayerInfoData(PlayerInfoParameter.Entry entry) throws Exception {
    // Create the object with a null-component which is to be personalized later

    if (ServerVersion.getCurrent().greaterThanOrEqual(ServerVersion.V1_19)) {
      return CT_PLAYER_INFO_DATA.newInstance(
        entry.resolveGameProfile(M_CRAFT_PLAYER__GET_PROFILE),
        entry.resolveLatency(interceptor),
        E_ENUM_GAME_MODE.getByCopy(entry.resolveGameMode()),
        null, null
      );
    }

    return CT_PLAYER_INFO_DATA.newInstance(
      entry.resolveGameProfile(M_CRAFT_PLAYER__GET_PROFILE),
      entry.resolveLatency(interceptor),
      E_ENUM_GAME_MODE.getByCopy(entry.resolveGameMode()),
      null
    );
  }

  @Override
  public @Nullable PlayerInfoParameter parseOutgoing(Object packet) {
    // TODO: Implement
    throw new UnsupportedOperationException();
  }
}
