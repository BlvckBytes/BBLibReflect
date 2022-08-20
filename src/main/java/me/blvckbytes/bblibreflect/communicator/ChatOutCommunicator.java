package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.ChatMessageParameter;
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
public class ChatOutCommunicator extends APacketOutCommunicator<ChatMessageParameter> {

  private final EnumHandle E_CHAT_MESSAGE_TYPE;
  private final FieldHandle F_PO_CHAT__CHAT_MESSAGE_TYPE, F_PO_CHAT__BASE_COMPONENT, F_PO_CHAT__UUID,
    F_CLB_CHAT__BASE_COMPONENT, F_CLB_CHAT__MESSAGE, F_CLB_CHAT__TYPE_ID;

  public ChatOutCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, true, () -> (
        ServerVersion.getCurrent().greaterThanOrEqual(ServerVersion.V1_19) ?
          helper.getClass(RClass.CLIENTBOUND_SYSTEM_CHAT_PACKET) :
          helper.getClass(RClass.PACKET_O_CHAT)
    ));

    if (ServerVersion.getCurrent().greaterThanOrEqual(ServerVersion.V1_19)) {
      // FIXME: WHY does this only show up as a string when it's a component? Even the server says the component is null!!! UGH
      F_CLB_CHAT__BASE_COMPONENT = getPacketType().locateField().withType(C_BASE_COMPONENT).optional();
      F_CLB_CHAT__MESSAGE        = F_CLB_CHAT__BASE_COMPONENT == null ? getPacketType().locateField().withType(String.class).required() : null;
      F_CLB_CHAT__TYPE_ID        = getPacketType().locateField().withType(int.class).required();

      E_CHAT_MESSAGE_TYPE = null;

      F_PO_CHAT__CHAT_MESSAGE_TYPE = null;
      F_PO_CHAT__BASE_COMPONENT    = null;
      F_PO_CHAT__UUID              = null;
    }

    else {
      // FIXME: Not really an enum >= 1.19...
      E_CHAT_MESSAGE_TYPE = helper.getClass(RClass.CHAT_MESSAGE_TYPE).asEnum();

      F_PO_CHAT__CHAT_MESSAGE_TYPE = getPacketType().locateField().withType(E_CHAT_MESSAGE_TYPE).required();
      F_PO_CHAT__BASE_COMPONENT    = getPacketType().locateField().withType(C_BASE_COMPONENT).required();
      F_PO_CHAT__UUID              = getPacketType().locateField().withType(UUID.class).optional();

      F_CLB_CHAT__TYPE_ID        = null;
      F_CLB_CHAT__BASE_COMPONENT = null;
      F_CLB_CHAT__MESSAGE        = null;
    }
  }

  @Override
  protected Object createBasePacket(ChatMessageParameter parameter) throws Exception {
    Object packet = createPacket();

    if (F_PO_CHAT__CHAT_MESSAGE_TYPE != null) {
      F_PO_CHAT__CHAT_MESSAGE_TYPE.set(packet, E_CHAT_MESSAGE_TYPE.getByCopy(parameter.getType()));

      if (F_PO_CHAT__UUID != null && parameter.getSender() != null)
        F_PO_CHAT__UUID.set(packet, parameter.getSender());

      return packet;
    }

    F_CLB_CHAT__TYPE_ID.set(packet, parameter.getType().ordinal());

    return packet;
  }

  @Override
  protected void personalizeBasePacket(Object packet, ChatMessageParameter parameter, ICustomizableViewer viewer) throws Exception {
    Object baseComponent = componentToBaseComponent(parameter.getMessage(), viewer);

    if (F_PO_CHAT__BASE_COMPONENT != null)
      F_PO_CHAT__BASE_COMPONENT.set(packet, baseComponent);
    else {
      if (F_CLB_CHAT__BASE_COMPONENT != null)
        F_CLB_CHAT__BASE_COMPONENT.set(packet, baseComponent);
      else
        F_CLB_CHAT__MESSAGE.set(packet, "OMG!!!!!");
    }
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
