package me.blvckbytes.bblibreflect.handle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of any given field.

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
public class FieldHandle {

  private final Field field;

  /**
   * Create a new field handle by locating the target field within
   * the given target class by dispatching the predicate immediately.
   * @param target Target class to search in
   * @param predicate Predicate which chooses the matching field
   * @throws NoSuchFieldException Thrown if the predicate didn't yield any results
   */
  public FieldHandle(Class<?> target, IFieldPredicate predicate) throws NoSuchFieldException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    int counter = 0;
    Field res = null;

    // Walk up the hierarchy chain
    Class<?> curr = target;
    while (res == null && curr != null && curr != Object.class) {

      // Loop all fields of the current class
      for (Field f : curr.getDeclaredFields()) {
        Boolean result = predicate.matches(f, counter);

        // Null means that it would have matched, but the
        // skip counter has not yet elapsed
        if (result == null) {
          counter++;
          continue;
        }

        // Predicate match, take the field
        if (result) {
          res = f;
          break;
        }
      }

      curr = curr.getSuperclass();
    }

    // The predicate matched on none of them
    if (res == null)
      throw new NoSuchFieldException("Could not satisfy the field predicate.");

    // Set the field accessible and hold a reference to it
    this.field = res;
    this.field.setAccessible(true);
  }

  /**
   * Set the field's value on an object instance
   * @param o Target object to modify
   * @param v Field value to set
   */
  public void set(Object o, Object v) throws IllegalAccessException {
    this.field.set(o, v);
  }

  /**
   * Get the field's value from an object instance
   * @param o Target object to read from
   * @return Field value
   */
  public Object get(Object o) throws IllegalAccessException {
    return this.field.get(o);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Field))
      return false;

    return field.equals(obj);
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }
}
