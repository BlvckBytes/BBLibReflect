package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.ChatMessageParameter;
import me.blvckbytes.bblibreflect.handle.AFieldHandle;
import me.blvckbytes.bblibreflect.handle.AMethodHandle;
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

  private final Class<?> C_PO_CHAT, C_CHAT_MESSAGE_TYPE;
  private final AFieldHandle F_PO_CHAT__CHAT_MESSAGE_TYPE, F_PO_CHAT__BASE_COMPONENT;
  private final AMethodHandle M_CHAT_SERIALIZER__FROM_JSON;

  public ChatCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper
  ) throws Exception {
    super(logger, helper);

    Class<?> C_BASE_COMPONENT    = requireClass(RClass.I_CHAT_BASE_COMPONENT);
    Class<?> C_CHAT_SERIALIZER   = requireClass(RClass.CHAT_SERIALIZER);

    C_PO_CHAT           = requireClass(RClass.PACKET_O_CHAT);
    C_CHAT_MESSAGE_TYPE = requireClass(RClass.CHAT_MESSAGE_TYPE);

    F_PO_CHAT__CHAT_MESSAGE_TYPE = requireScalarField(C_PO_CHAT, C_CHAT_MESSAGE_TYPE, 0, false, false, null);
    F_PO_CHAT__BASE_COMPONENT    = requireScalarField(C_PO_CHAT, C_BASE_COMPONENT, 0, false, false, null);

    M_CHAT_SERIALIZER__FROM_JSON = requireArgsMethod(C_CHAT_SERIALIZER, new Class[] { JsonElement.class }, C_BASE_COMPONENT, false);
  }

  @Override
  public void sendParameterized(IPacketReceiver receiver, ChatMessageParameter parameter) {
    try {
      Object packet = helper.createEmptyPacket(C_PO_CHAT);

      F_PO_CHAT__CHAT_MESSAGE_TYPE.set(packet, getMessageType(parameter.isChat()));
      F_PO_CHAT__BASE_COMPONENT.set(packet, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, parameter.getJson()));

      receiver.sendPackets(packet);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Get the message type enum for either the chat or the action-bar
   * @param chat True means chat, false action-bar
   * @return Enum constant
   */
  @SuppressWarnings("unchecked")
  private Enum<?> getMessageType(boolean chat) {
    Enum<?>[] types = ((Class<? extends Enum<?>>) C_CHAT_MESSAGE_TYPE).getEnumConstants();
    return types[chat ? 0 : 2];
  }
}
