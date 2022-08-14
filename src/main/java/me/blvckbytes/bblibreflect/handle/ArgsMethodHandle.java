package me.blvckbytes.bblibreflect.handle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of a method determined by it's args.

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
public class ArgsMethodHandle extends AMethodHandle {

  public ArgsMethodHandle(
    Class<?> target,
    Class<?>[] argTypes,
    boolean allowSuper
  ) throws NoSuchMethodException {
    super(target, m -> {
      if (!allowSuper && !m.getDeclaringClass().equals(target))
        return false;

      Class<?>[] params = m.getParameterTypes();
      if (params.length != argTypes.length)
        return false;

      for (int i = 0; i < params.length; i++) {
        if (!params[i].isAssignableFrom(argTypes[i]))
          return false;
      }

      return true;
    });
  }

  public ArgsMethodHandle(
    Class<?> target,
    Class<?>[] argTypes,
    Class<?> returnType,
    boolean allowSuper
  ) throws NoSuchMethodException {
    super(target, m -> {
      if (!allowSuper && !m.getDeclaringClass().equals(target))
        return false;

      Class<?>[] params = m.getParameterTypes();
      if (params.length != argTypes.length)
        return false;

      for (int i = 0; i < params.length; i++) {
        if (!params[i].isAssignableFrom(argTypes[i]))
          return false;
      }

      return returnType.isAssignableFrom(m.getReturnType());
    });
  }
}
