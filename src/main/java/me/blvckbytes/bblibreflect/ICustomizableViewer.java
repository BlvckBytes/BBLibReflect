package me.blvckbytes.bblibreflect;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/13/2022

  Represents a viewer of a customizable resource which will influence
  it's representation based on their abilities.

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
public interface ICustomizableViewer extends IPacketReceiver {

  /**
   * Get the currently open window ID of the player
   */
  int getCurrentWindowId();

  /**
   * Whether this viewer cannot render hex colors
   */
  boolean cannotRenderHexColors();

  /**
   * Get the version number of the client
   */
  int getClientVersion();

}
