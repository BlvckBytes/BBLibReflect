package me.blvckbytes.bblibreflect.handle;

import com.google.common.primitives.Primitives;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Wraps a plain class as a type to be compared against other types and
  offers comparison schema configurations.

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
public class ComparableType {

  private final Class<?> type;
  private final boolean ignoreBoxing;
  private final Assignability assignability;

  /**
   * Create a new parameterized comparable type
   * @param type Base type
   * @param ignoreBoxing Whether to ignore boxing/unboxing
   * @param assignability Assignability mode/direction
   */
  public ComparableType(Class<?> type, boolean ignoreBoxing, Assignability assignability) {
    // If boxing is to be ignored, unwrap ahead of time
    this.type = ignoreBoxing ? Primitives.unwrap(type) : type;
    this.ignoreBoxing = ignoreBoxing;
    this.assignability = assignability;
  }

  /**
   * Checks whether this type matches another type
   * @param other Type to check against
   */
  public boolean matches(Class<?> other) {
    // Unbox the other type, if boxing is ignored
    if (ignoreBoxing)
      other = Primitives.unwrap(other);

    return isAssignable(other);
  }

  /**
   * Checks whether this type matches the specified
   * assignability with another type
   * @param other Type to check against
   */
  private boolean isAssignable(Class<?> other) {
    if (assignability == Assignability.NONE)
      return type.equals(other);

    // Check assignability modes
    switch (assignability) {

      case TARGET_TO_TYPE:
        return other.isAssignableFrom(type);

      case TYPE_TO_TARGET:
        return type.isAssignableFrom(other);

      // Don't try to match on assignability, just compare
      default:
        return type.equals(other);
    }
  }
}
