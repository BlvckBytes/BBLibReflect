package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.ChatMessageParameter;
import me.blvckbytes.bblibreflect.handle.*;
import me.blvckbytes.bblibutil.logger.ILogger;

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
public class ChatCommunicator extends APacketCommunicator<ChatMessageParameter> {

  private final ClassHandle C_PO_CHAT;
  private final EnumHandle E_CHAT_MESSAGE_TYPE;
  private final FieldHandle F_PO_CHAT__CHAT_MESSAGE_TYPE, F_PO_CHAT__BASE_COMPONENT;
  private final MethodHandle M_CHAT_SERIALIZER__FROM_JSON;

  public ChatCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper
  ) throws Exception {
    super(logger, helper);

    ClassHandle C_BASE_COMPONENT    = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);
    ClassHandle C_CHAT_SERIALIZER   = helper.getClass(RClass.CHAT_SERIALIZER);

    C_PO_CHAT           = helper.getClass(RClass.PACKET_O_CHAT);
    E_CHAT_MESSAGE_TYPE = helper.getClass(RClass.CHAT_MESSAGE_TYPE).asEnum();

    F_PO_CHAT__CHAT_MESSAGE_TYPE = C_PO_CHAT.locateField().withType(E_CHAT_MESSAGE_TYPE).required();
    F_PO_CHAT__BASE_COMPONENT    = C_PO_CHAT.locateField().withType(C_BASE_COMPONENT).required();

    M_CHAT_SERIALIZER__FROM_JSON = C_CHAT_SERIALIZER.locateMethod()
      .withParameters(JsonElement.class)
      .withReturnType(C_BASE_COMPONENT, false, Assignability.TYPE_TO_TARGET)
      .withStatic(true).required();
  }

  @Override
  public void sendParameterized(IPacketReceiver receiver, ChatMessageParameter parameter) {
    try {
      Object packet = helper.createEmptyPacket(C_PO_CHAT);

      F_PO_CHAT__CHAT_MESSAGE_TYPE.set(packet, E_CHAT_MESSAGE_TYPE.getByOrdinal(parameter.isChat() ? 0 : 2));
      F_PO_CHAT__BASE_COMPONENT.set(packet, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getJson()));

      receiver.sendPackets(packet);
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
