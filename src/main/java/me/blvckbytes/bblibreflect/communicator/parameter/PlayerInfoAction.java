package me.blvckbytes.bblibreflect.communicator.parameter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents all player info update actions.

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
public enum PlayerInfoAction implements ICommunicatorParameter {

  // Add a player to the tab list
  ADD_PLAYER,

  // Update a player's game mode
  UPDATE_GAME_MODE,

  // Update the latency indicator of a player
  UPDATE_LATENCY,

  // Update the displayed name of a player
  UPDATE_DISPLAY_NAME,

  // Remove a player from the tab list
  REMOVE_PLAYER

  ;

}
