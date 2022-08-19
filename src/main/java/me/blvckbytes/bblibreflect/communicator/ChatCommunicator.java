package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.ChatMessageParameter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.EnumHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
  private final FieldHandle F_PO_CHAT__CHAT_MESSAGE_TYPE, F_PO_CHAT__BASE_COMPONENT, F_PO_CHAT__UUID;

  public ChatCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, true, helper.getClass(RClass.PACKET_O_CHAT));

    ClassHandle C_BASE_COMPONENT    = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);

    C_PO_CHAT           = helper.getClass(RClass.PACKET_O_CHAT);
    E_CHAT_MESSAGE_TYPE = helper.getClass(RClass.CHAT_MESSAGE_TYPE).asEnum();

    F_PO_CHAT__CHAT_MESSAGE_TYPE = C_PO_CHAT.locateField().withType(E_CHAT_MESSAGE_TYPE).required();
    F_PO_CHAT__BASE_COMPONENT    = C_PO_CHAT.locateField().withType(C_BASE_COMPONENT).required();
    F_PO_CHAT__UUID              = C_PO_CHAT.locateField().withType(UUID.class).optional();
  }

  @Override
  protected Object createBasePacket(ChatMessageParameter parameter) throws Exception {
    Object packet = helper.createEmptyPacket(C_PO_CHAT);

    F_PO_CHAT__CHAT_MESSAGE_TYPE.set(packet, E_CHAT_MESSAGE_TYPE.getByCopy(parameter.getType()));

    if (F_PO_CHAT__UUID != null && parameter.getSender() != null)
      F_PO_CHAT__UUID.set(packet, parameter.getSender());

    return packet;
  }

  @Override
  protected void personalizeBasePacket(Object packet, ChatMessageParameter parameter, ICustomizableViewer viewer) throws Exception {
    F_PO_CHAT__BASE_COMPONENT.set(packet, componentToBaseComponent(parameter.getMessage(), viewer));
  }

  @Override
  public @Nullable ChatMessageParameter parseOutgoing(Object packet) {
    // TODO: Implement
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<ChatMessageParameter> getParameterType() {
    return ChatMessageParameter.class;
  }
}
