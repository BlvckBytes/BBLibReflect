package me.blvckbytes.bblibreflect.handle;

import lombok.Getter;

import java.lang.reflect.Method;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of any given method.

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
public abstract class AMethodHandle {

  @Getter
  private final Method method;

  public AMethodHandle(Class<?> target, IMethodPredicate predicate) throws NoSuchMethodException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    Method res = null;

    // Walk up the hierarchy chain
    Class<?> curr = target;
    while (res == null && curr != null && curr != Object.class) {

      for (Method m : curr.getDeclaredMethods()) {

        if (predicate.matches(m, curr != target)) {
          res = m;
          break;
        }

      }

      curr = curr.getSuperclass();
    }

    if (res == null)
      throw new NoSuchMethodException("Could not satisfy the method predicate.");

    this.method = res;
    this.method.setAccessible(true);
  }
}
