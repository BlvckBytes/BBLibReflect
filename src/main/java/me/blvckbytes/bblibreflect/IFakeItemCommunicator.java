package me.blvckbytes.bblibreflect;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Communicates setting items in the player's inventory in a fake manner, so
  that the server doesn't handle them and they're only virtual.

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
public interface IFakeItemCommunicator {

  /**
   * Perform a clientside only slot change within the players viewed top inventory
   * @param p Target player
   * @param is ItemStack to set
   * @param slot Slot ID to change
   * @return Success state
   */
  boolean setFakeTopInventorySlot(Player p, @Nullable ItemStack is, int slot);

  /**
   * Perform a clientside only slot change within the players own inventory
   * @param p Target player
   * @param is ItemStack to set
   * @param slot Slot ID to change
   * @return Success state
   */
  boolean setFakeInventorySlot(Player p, @Nullable ItemStack is, int slot);

}
