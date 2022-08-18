package me.blvckbytes.bblibreflect.communicator.parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  A parameter used to define which slot to set to what item when using the
  SetSlotCommunicator.

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
@Setter
@Getter
@AllArgsConstructor
public class SetSlotParameter implements ICommunicatorParameter {

  // Item to set
  private @Nullable ItemStack item;

  // Slot to set to
  private int slot;

  // True means top inventory, false targets the bottom inventory
  private boolean top;

}
