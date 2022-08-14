package me.blvckbytes.bblibreflect.handle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of a constructor determined by it's args.

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
public class ArgsConstructorHandle extends AConstructorHandle {

  public ArgsConstructorHandle(
    Class<?> target,
    Class<?>[] args
  ) throws NoSuchMethodException {
    super(target, (c) -> {
      if (c.getParameterCount() != args.length)
        return false;

      Class<?>[] types = c.getParameterTypes();
      for (int i = 0; i < args.length; i++) {
        if (!types[i].isAssignableFrom(args[i]))
          return false;
      }

      return true;
    });
  }
}
