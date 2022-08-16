package me.blvckbytes.bblibreflect.handle;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/15/2022

  Extends the ClassHandle by interpreting the provided class as an
  enumeration and providing multiple methods of finding the target constant.

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
public class EnumHandle extends ClassHandle {

  // TODO: Check against enum copies if they match the number of entries, else throw

  private final List<Enum<?>> e;

  /**
   * Create a new enumeration handle on top of a enumeration class
   * @param c Class which represents an enumeration
   * @throws IllegalStateException Thrown if the provided class is not an enumeration
   */
  public EnumHandle(Class<?> c) throws IllegalStateException {
    super(c);

    Object[] constants = c.getEnumConstants();

    // The provided class hasn't been of an enumeration type
    if (constants == null)
      throw new IllegalStateException("This class does not represent an enumeration.");

    // Create a unmodifiable list of constants and wrap into a handle
    e = List.of((Enum<?>[]) constants);
  }

  /**
   * Get an enumeration constant by it's ordinal integer
   * @param ordinal Ordinal integer
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  @SuppressWarnings("unchecked")
  public Enum<?> getByOrdinal(int ordinal) throws EnumConstantNotPresentException {
    try {
      return e.get(ordinal);
    } catch (Exception e) {
      throw new EnumConstantNotPresentException((Class<? extends Enum<?>>) c, "ordinal=" + ordinal);
    }
  }

  /**
   * Get an enumeration constant by looking up the ordinal of a
   * copy enum which has it's constants sorted in the exact same order.
   * @param other Constant of a copy
   * @return Enumeration constant
   * @throws EnumConstantNotPresentException Thrown if there is no constant with this ordinal value
   */
  public Enum<?> getByCopy(Enum<?> other) throws EnumConstantNotPresentException {
    return getByOrdinal(other.ordinal());
  }
}
