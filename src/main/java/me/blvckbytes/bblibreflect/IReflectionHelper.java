package me.blvckbytes.bblibreflect;

import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.EnumHandle;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/13/2022

  The central reflection helper endpoint which offers all reflection
  and packet based routines.

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
public interface IReflectionHelper {

  /**
   * Create a new empty packet instance of a specific class
   * @param c Class to create
   * @return Empty packet instance
   */
  Object createEmptyPacket(ClassHandle c);

  /**
   * Load a known reflection required class by it's identifier
   * @param rc Class identifier
   * @return Loaded class, if available
   */
  ClassHandle getClass(RClass rc) throws ClassNotFoundException;

  /**
   * Load a known reflection required class by it's identifier
   * @param rc Class identifier
   * @return Loaded class, if available
   */
  @Nullable ClassHandle getClassOptional(RClass rc);

  /**
   * Load a known reflection required class as an enumeration
   * by it's identifier
   * @param rc Class identifier
   * @return Loaded enumeration, if available
   */
  @Nullable EnumHandle getEnumOptional(RClass rc);

  /**
   * Get the version specific burning time of any material within
   * a furnace when used as a fuel source
   * @param mat Material to check
   * @return Burning time in ticks, empty if the material is not a fuel source
   */
  Optional<Integer> getBurnTime(Material mat);

}
