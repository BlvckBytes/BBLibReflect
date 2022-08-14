package me.blvckbytes.bblibreflect.handle;

import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of any given constructor.

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
public abstract class AConstructorHandle {

  private final Constructor<?> constructor;

  public AConstructorHandle(Class<?> target, IConstructorPredicate predicate) throws NoSuchMethodException {
    if (target == null)
      throw new IllegalStateException("Target has to be present.");

    Constructor<?> res = null;

    for (Constructor<?> c : target.getDeclaredConstructors()) {
      if (predicate.matches(c)) {
        res = c;
        break;
      }
    }

    if (res == null)
      throw new NoSuchMethodException("Could not satisfy the constructor predicate.");

    this.constructor = res;
    this.constructor.setAccessible(true);
  }

  public Object newInstance(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
    return this.constructor.newInstance(args);
  }
}
