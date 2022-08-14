package me.blvckbytes.bblibreflect.handle;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of a map-like field.

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
public class MapFieldHandle extends AFieldHandle {

  public MapFieldHandle(
    Class<?> target,
    Class<?> keyType,
    Class<?> valueType,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) throws NoSuchFieldException {
    super(target, (f, sc, tc) -> {
      if (isStatic != null && isStatic != Modifier.isStatic(f.getModifiers()))
        return null;

      if (isPublic != null && isPublic != Modifier.isPublic(f.getModifiers()))
        return false;

      if (sc && !allowSuper)
        return false;

      if (skip > tc)
        return false;

      if (!type.isAssignableFrom(f.getType()))
        return false;

      ParameterizedType pType = (ParameterizedType) f.getGenericType();

      return (
        keyType.isAssignableFrom((Class<?>) pType.getActualTypeArguments()[0]) &&
        valueType.isAssignableFrom((Class<?>) pType.getActualTypeArguments()[1])
      );
    });
  }

  public MapFieldHandle(
    Class<?> target,
    String name,
    Class<?> type
  ) throws NoSuchFieldException {
    super(target, (f, sc, tc) -> {
      if (!f.getType().equals(type))
        return false;

      return f.getName().equalsIgnoreCase(name);
    });
  }
}
