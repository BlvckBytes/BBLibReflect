package me.blvckbytes.bblibreflect.communicator.parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.bblibutil.component.IComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  A parameter used to define which JSON message to send in either the chat
  or the action-bar when using the ChatCommunicator.

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
@Getter
@Setter
@AllArgsConstructor
public class ChatMessageParameter implements ICommunicatorParameter {

  // Json message to be serialized
  IComponent message;

  // UUID of the sending player
  @Nullable UUID sender;

  // Where to display the message
  ChatMessageType type;

}
