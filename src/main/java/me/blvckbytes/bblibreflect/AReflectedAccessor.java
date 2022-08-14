package me.blvckbytes.bblibreflect;

import me.blvckbytes.bblibreflect.handle.*;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  The base of all classes which care to access fields, methods and
  classes using reflection. Contains mostly convenient shorthands.

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
public abstract class AReflectedAccessor {

  protected final ILogger logger;
  protected final IReflectionHelper helper;

  public AReflectedAccessor(
    ILogger logger, IReflectionHelper helper
  ) {
    this.logger = logger;
    this.helper = helper;
  }

  protected Class<?> requireClass(RClass rc) throws ClassNotFoundException {
    return helper.getClass(rc);
  }

  protected Class<?> optionalClass(RClass rc) {
    try {
      return helper.getClass(rc);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  protected Field requireCollectionField(
    Class<?> target,
    Class<?> collectionType,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) throws NoSuchFieldException {
    return new CollectionFieldHandle(target, collectionType, type, skip, allowSuper, isStatic, isPublic).getField();
  }

  protected @Nullable Field optionalCollectionField(
    Class<?> target,
    Class<?> collectionType,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) {
    try {
      return new CollectionFieldHandle(target, collectionType, type, skip, allowSuper, isStatic, isPublic).getField();
    } catch (Exception e) {
      return null;
    }
  }

  protected Field requireMapField(
    Class<?> target,
    Class<?> keyType,
    Class<?> valueType,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) throws NoSuchFieldException {
    return new MapFieldHandle(target, keyType, valueType, type, skip, allowSuper, isStatic, isPublic).getField();
  }

  protected @Nullable Field optionalMapField(
    Class<?> target,
    Class<?> keyType,
    Class<?> valueType,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) {
    try {
      return new MapFieldHandle(target, keyType, valueType, type, skip, allowSuper, isStatic, isPublic).getField();
    } catch (Exception e) {
      return null;
    }
  }

  protected Field requireScalarField(
    Class<?> target,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) throws NoSuchFieldException {
    return new ScalarFieldHandle(target, type, skip, allowSuper, isStatic, isPublic).getField();
  }

  protected @Nullable Field optionalScalarField(
    Class<?> target,
    Class<?> type,
    int skip,
    boolean allowSuper,
    Boolean isStatic,
    Boolean isPublic
  ) {
    try {
      return new ScalarFieldHandle(target, type, skip, allowSuper, isStatic, isPublic).getField();
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  protected Method requireNamedMethod(
    Class<?> target,
    String name,
    boolean allowSuper
  ) throws NoSuchMethodException {
    return new NamedMethodHandle(target, name, allowSuper).getMethod();
  }

  protected Method requireNamedMethod(
    Class<?> target,
    String name,
    Class<?> returnType,
    boolean allowSuper
  ) throws NoSuchMethodException {
    return new NamedMethodHandle(target, name, returnType, allowSuper).getMethod();
  }

  protected Method requireArgsMethod(
    Class<?> target,
    Class<?>[] argTypes,
    boolean allowSuper
  ) throws NoSuchMethodException {
    return new ArgsMethodHandle(target, argTypes, allowSuper).getMethod();
  }

  protected Method requireArgsMethod(
    Class<?> target,
    Class<?>[] argTypes,
    Class<?> returnType,
    boolean allowSuper
  ) throws NoSuchMethodException {
    return new ArgsMethodHandle(target, argTypes, returnType, allowSuper).getMethod();
  }

  protected Constructor<?> requireArgsConstructor(
    Class<?> target,
    Class<?>[] argTypes
  ) throws NoSuchMethodException {
    return new ArgsConstructorHandle(target, argTypes).getConstructor();
  }
}
