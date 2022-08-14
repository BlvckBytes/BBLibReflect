package me.blvckbytes.bblibreflect.handle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of a named method.

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
public class NamedMethodHandle extends AMethodHandle {

  public NamedMethodHandle(
    Class<?> target,
    String name,
    boolean allowSuper
  ) throws NoSuchMethodException {
    super(target, m -> {
      if (!allowSuper && !m.getDeclaringClass().equals(target))
        return false;
      return m.getName().equalsIgnoreCase(name);
    });
  }

  public NamedMethodHandle(
    Class<?> target,
    String name,
    Class<?> returnType,
    boolean allowSuper
  ) throws NoSuchMethodException {
    super(target, m -> {
      if (!allowSuper && !m.getDeclaringClass().equals(target))
        return false;

      if (!m.getName().equalsIgnoreCase(name))
        return false;

      return returnType.isAssignableFrom(m.getReturnType());
    });
  }
}
