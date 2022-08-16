package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.PlayerInfoParameter;
import me.blvckbytes.bblibreflect.handle.*;
import me.blvckbytes.bblibutil.logger.ILogger;

import java.util.ArrayList;
import java.util.List;

@AutoConstruct
public class PlayerInfoCommunicator extends APacketCommunicator<PlayerInfoParameter> {

  private final EnumHandle E_ENUM_PLAYER_INFO_ACTION, E_ENUM_GAME_MODE;

  private final ClassHandle C_PO_PLAYER_INFO;

  private final FieldHandle F_PO_PLAYER_INFO__ENUM, F_PO_PLAYER_INFO__LIST;

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

    CT_PLAYER_INFO_DATA = C_PLAYER_INFO_DATA.locateConstructor().withParameters(GameProfile.class, int.class).withParameters(C_ENUM_GAME_MODE, C_BASE_COMPONENT).required();
  }

  @Override
  public void sendParameterized(List<IPacketReceiver> receivers, PlayerInfoParameter parameter) {
    try {
      // Create a new packet instance and set the action, it will be the same for all viewers
      Object info = helper.createEmptyPacket(C_PO_PLAYER_INFO);
      F_PO_PLAYER_INFO__ENUM.set(info, E_ENUM_PLAYER_INFO_ACTION.getByCopy(parameter.getAction()));

      // Create a new list of PlayerInfoData and set it's ref, it will also remain constant
      List<Object> dataList = new ArrayList<>();
      F_PO_PLAYER_INFO__LIST.set(info, dataList);

      // TODO: Don't customize if the JSON is null anyways

      // Loop all viewers and create a customized JSON for each entry
      for (IPacketReceiver receiver : receivers) {
        ICustomizableViewer viewer = asViewer(receiver);

        // Re-populate the data list
        dataList.clear();
        for (PlayerInfoParameter.Entry entry : parameter.getEntries()) {

          // TODO: Only patch the base component, don't create the whole PlayerInfoData again each time

          // Personalize the entry's JSON for the current viewer
          JsonObject json = entry.getName() == null ? null : entry.getName().toJson(viewer.cannotRenderHexColors());

          Object data = CT_PLAYER_INFO_DATA.newInstance(
            entry.resolveGameProfile(M_CRAFT_PLAYER__GET_PROFILE),
            entry.resolveLatency(interceptor),
            E_ENUM_GAME_MODE.getByCopy(entry.resolveGameMode()),
            M_CHAT_SERIALIZER__FROM_JSON.invoke(null, json)
          );

          dataList.add(data);
        }

        // Send to the current viewer
        viewer.sendPacket(info, null);
      }

    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public void sendParameterized(IPacketReceiver receiver, PlayerInfoParameter parameter) {
    // TODO: Don't loop if there's just one receiver, also don't use List.of() then
    sendParameterized(List.of(receiver), parameter);
  }
}
