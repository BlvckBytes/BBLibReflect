package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.ChatMessageParameter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.ConstructorHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.component.TextComponent;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Communicates sending chat messages to either the chat or the action bar.

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
public class ChatInCommunicator extends APacketInCommunicator<ChatMessageParameter> {

  private final FieldHandle F_PI_CHAT__MESSAGE, F_MINECRAFT_SERVER__PLAYER_LIST;
  private final ClassHandle C_CHAT_MESSAGE_TYPE, C_RESOURCE_KEY;
  private final ConstructorHandle CTOR_FILTERED_TEXT, CTOR_PLAYER_CHAT_MESSAGE;
  private final MethodHandle M_PLAYER_LIST__BROADCAST_CHAT_MESSAGE, M_MESSAGE_SIGNATURE__GET_UNSIGNED;

  public ChatInCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, true, helper.getClass(RClass.PACKET_I_CHAT));

    F_PI_CHAT__MESSAGE = getPacketType().locateField().withType(String.class).required();

    if (ServerVersion.getCurrent().greaterThanOrEqual(ServerVersion.V1_19)) {
      ClassHandle C_MESSAGE_SIGNATURE = helper.getClass(RClass.MESSAGE_SIGNATURE);
      ClassHandle C_FILTERED_TEXT = helper.getClass(RClass.FILTERED_TEXT);
      ClassHandle C_PLAYER_LIST = helper.getClass(RClass.PLAYER_LIST);

      C_CHAT_MESSAGE_TYPE = helper.getClass(RClass.CHAT_MESSAGE_TYPE);
      C_RESOURCE_KEY      = helper.getClass(RClass.RESOURCE_KEY);

      CTOR_FILTERED_TEXT = C_FILTERED_TEXT.locateConstructor()
        .withParameters(Object.class, Object.class)
        .required();

      CTOR_PLAYER_CHAT_MESSAGE = helper.getClass(RClass.PLAYER_CHAT_MESSAGE)
        .locateConstructor()
        .withParameters(C_BASE_COMPONENT, C_MESSAGE_SIGNATURE)
        .withParameters(Optional.class)
        .required();

      M_PLAYER_LIST__BROADCAST_CHAT_MESSAGE = C_PLAYER_LIST.locateMethod()
        .withParameters(C_FILTERED_TEXT, C_ENTITY_PLAYER, C_RESOURCE_KEY)
        .required();

      F_MINECRAFT_SERVER__PLAYER_LIST = C_MINECRAFT_SERVER.locateField()
        .withType(C_PLAYER_LIST)
        .required();

      M_MESSAGE_SIGNATURE__GET_UNSIGNED = C_MESSAGE_SIGNATURE.locateMethod()
        .withReturnType(C_MESSAGE_SIGNATURE)
        .withStatic(true)
        .required();
    }

    else {
      C_CHAT_MESSAGE_TYPE = null;
      C_RESOURCE_KEY      = null;

      CTOR_FILTERED_TEXT       = null;
      CTOR_PLAYER_CHAT_MESSAGE = null;

      F_MINECRAFT_SERVER__PLAYER_LIST       = null;
      M_PLAYER_LIST__BROADCAST_CHAT_MESSAGE = null;
      M_MESSAGE_SIGNATURE__GET_UNSIGNED     = null;
    }
  }

  @Override
  protected void receivePacket(IPacketReceiver receiver, ChatMessageParameter parameter, Object packet, @Nullable Runnable done) {

    // FIXME: Was this at all necessary? Maybe the AsyncPlayerChatEvent is right above it anyways? Investigate...

    // Sneak incoming chat packets past the validator routine...
    // Signed chat packets are impossible to emulate otherwise
    if (ServerVersion.getCurrent().greaterThanOrEqual(ServerVersion.V1_19) && parameter.getSender() != null) {
      Player receiverPlayer = Bukkit.getPlayer(parameter.getSender());

      if (receiverPlayer == null)
        super.receivePacket(receiver, parameter, packet, done);

      try {
        Object messageComponent = componentToBaseComponent(new TextComponent((String) F_PI_CHAT__MESSAGE.get(packet)), null);

        // Create a new chat message instance with the UNSIGNED signature constant
        // Since it's "injected" past verification, nobody is going to notice that
        Object chatMessage = CTOR_PLAYER_CHAT_MESSAGE.newInstance(
          messageComponent,
          M_MESSAGE_SIGNATURE__GET_UNSIGNED.invoke(null),
          Optional.empty()
        );

        // No filtering occurring here... raw=filtered
        Object filteredText = CTOR_FILTERED_TEXT.newInstance(chatMessage, chatMessage);

        // Find the n-th static ResourceKey constant within the ChatMessageType of itself
        Object messageType = C_CHAT_MESSAGE_TYPE.locateField()
          .withType(C_RESOURCE_KEY)
          .withGeneric(C_CHAT_MESSAGE_TYPE)
          .withStatic(true)
          .withSkip(parameter.getType().ordinal())
          .required()
          .get(null);

        // Invoke the broadcast chat message method on the player list reference
        // using the prepared parameters from above
        M_PLAYER_LIST__BROADCAST_CHAT_MESSAGE.invoke(
          F_MINECRAFT_SERVER__PLAYER_LIST.get(O_MINECRAFT_SERVER),
          filteredText, getEntityPlayer(receiverPlayer), messageType
        );
      } catch (Exception e) {
        logger.logError(e);
      }

      return;
    }

    super.receivePacket(receiver, parameter, packet, done);
  }

  @Override
  protected Object createBasePacket(ChatMessageParameter parameter) throws Exception {
    Object packet = createPacket();

    F_PI_CHAT__MESSAGE.set(packet, parameter.getMessage().toPlainText().replace('§', '&'));

    return packet;
  }

  @Override
  protected void personalizeBasePacket(Object packet, ChatMessageParameter parameter, ICustomizableViewer viewer) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable ChatMessageParameter parseIncoming(Object packet) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<ChatMessageParameter> getParameterType() {
    return ChatMessageParameter.class;
  }
}
