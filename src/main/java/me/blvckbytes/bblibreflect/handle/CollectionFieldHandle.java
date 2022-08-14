package me.blvckbytes.bblibreflect.handle;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of a generic collection-like field.

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
public class CollectionFieldHandle extends AFieldHandle {

  public CollectionFieldHandle(
    Class<?> target,
    Class<?> collectionType,
    Class<?> genericType,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) throws NoSuchFieldException {
    super(target, (f, tc) -> {
      if (isStatic != null && isStatic != Modifier.isStatic(f.getModifiers()))
        return null;

      if (isPublic != null && isPublic != Modifier.isPublic(f.getModifiers()))
        return false;

      if (!allowSuper && !f.getDeclaringClass().equals(target))
        return false;

      if (skip > tc)
        return false;

      if (!collectionType.isAssignableFrom(f.getType()))
        return false;

      ParameterizedType type = (ParameterizedType) f.getGenericType();

      return genericType.isAssignableFrom((Class<?>) type.getActualTypeArguments()[0]);
    });
  }

  public CollectionFieldHandle(
    Class<?> target,
    Class<?> collectionType,
    String name
  ) throws NoSuchFieldException {
    super(target, (f, tc) -> {
      if (!collectionType.isAssignableFrom(f.getType()))
        return false;

      return f.getName().equalsIgnoreCase(name);
    });
  }
}
