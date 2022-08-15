package me.blvckbytes.bblibreflect.handle;

import lombok.AllArgsConstructor;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents a reflection handle of a class which directly offers predicate
  builders to query members expressively.

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
@AllArgsConstructor
public class ClassHandle {

  // Caching manual encapsulations using the of() constructor here
  private static final Map<Class<?>, ClassHandle> encapsulations;

  // Caching enumeration constants
  private static final Map<Class<?>, EnumHandle> enumerations;

  static {
    encapsulations = new HashMap<>();
    enumerations = new HashMap<>();
  }

  protected final Class<?> c;

  /**
   * Get the encapsulated field directly
   */
  public Class<?> get() {
    return this.c;
  }

  /**
   * Checks whether an object is an instance of this class
   * @param o Object to check
   */
  public boolean isInstance(Object o) {
    return this.c.isInstance(o);
  }

  /**
   * Interpret this class as an enumeration and get a handle to it
   * @throws IllegalStateException Thrown if this class is not an enumeration
   */
  public EnumHandle asEnum() throws IllegalStateException {
    EnumHandle enumHandle = enumerations.get(c);

    // Use cached value
    if (enumHandle != null)
      return enumHandle;

    // Create a new enum handle on this class
    enumHandle = new EnumHandle(c);

    // Store in cache and return
    enumerations.put(c, enumHandle);
    return enumHandle;
  }

  /**
   * Create a new FieldHandle builder which will query this class
   */
  public FieldPredicateBuilder locateField() {
    return new FieldPredicateBuilder(this);
  }

  /**
   * Create a new MethodHandle builder which will query this class
   */
  public MethodPredicateBuilder locateMethod() {
    return new MethodPredicateBuilder(this);
  }

  /**
   * Create a new ConstructorHandle builder which will query this class
   */
  public ConstructorPredicateBuilder locateConstructor() {
    return new ConstructorPredicateBuilder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Class<?>))
      return false;

    return c.equals(obj);
  }

  @Override
  public int hashCode() {
    return c.hashCode();
  }

  /**
   * Create a new class handle on top of a vanilla class
   * @param c Target class
   */
  public static ClassHandle of(Class<?> c) {
    ClassHandle handle = encapsulations.get(c);

    // Create new instance
    if (handle == null) {
      handle = new ClassHandle(c);
      encapsulations.put(c, handle);
      return handle;
    }

    // Return existing instance
    return handle;
  }
}
