package me.blvckbytes.bblibreflect.handle;

import java.lang.reflect.Modifier;

public class ScalarFieldHandle extends AFieldHandle {

  public ScalarFieldHandle(
    Class<?> target,
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

      return type.isAssignableFrom(f.getType());
    });
  }

  public ScalarFieldHandle(
    Class<?> target,
    String name
  ) throws NoSuchFieldException {
    super(target, (f, sc, tc) -> f.getName().equalsIgnoreCase(name));
  }
}
