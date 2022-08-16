package me.blvckbytes.bblibreflect.communicator.parameter;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents all player info game mode values.

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
public enum EnumGameMode {

  NOT_SET,
  SURVIVAL,
  CREATIVE,
  ADVENTURE,
  SPECTATOR
  ;

  /**
   * Get the enum constant matching a player's game mode
   * @param p Target player
   * @return Matching constant
   */
  public static EnumGameMode getFromPlayer(Player p) {
    switch (p.getGameMode()) {
      case CREATIVE:
        return CREATIVE;
      case SURVIVAL:
        return SURVIVAL;
      case ADVENTURE:
        return ADVENTURE;
      case SPECTATOR:
        return SPECTATOR;

      default:
        return NOT_SET;
    }
  }
}
