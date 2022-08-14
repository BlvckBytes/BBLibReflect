package me.blvckbytes.bblibreflect.handle;

import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;

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
public abstract class AFieldHandle {

  @Getter
  private final Field field;

  public AFieldHandle(Class<?> target, IFieldPredicate predicate) throws NoSuchFieldException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    // Keeps track of type occurrences
    Map<Class<?>, Integer> typeCounters = new HashMap<>();
    Field res = null;

    // Walk up the hierarchy chain
    Class<?> curr = target;
    while (res == null && curr != null && curr != Object.class) {

      for (Field f : curr.getDeclaredFields()) {
        int typeCounter = typeCounters.getOrDefault(f.getType(), 0);
        Boolean result = predicate.matches(f, typeCounter);

        if (result != null && result) {
          res = f;
          break;
        }

        // Only count up if result wasn't null
        if (result != null)
          typeCounters.put(f.getType(), typeCounter + 1);
      }

      curr = curr.getSuperclass();
    }

    if (res == null)
      throw new NoSuchFieldException("Could not satisfy the field predicate.");

    this.field = res;
    this.field.setAccessible(true);
  }
}
