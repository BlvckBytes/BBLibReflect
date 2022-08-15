package me.blvckbytes.bblibreflect.handle;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Incrementally builds up all available parameters to execute a method
  predicate in the most granular way possible.

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
public class MethodPredicateBuilder extends APredicateBuilder<MethodHandle> {

  private final ClassHandle targetClass;
  private @Nullable Boolean isStatic;
  private @Nullable Boolean isPublic;
  private @Nullable String name;
  private @Nullable ComparableType returnType;
  private final List<ComparableType> parameterTypes;
  private boolean allowSuperclass;

  /**
   * Create a new method predicate builder on a class handle
   * @param targetClass Class to search through
   */
  public MethodPredicateBuilder(ClassHandle targetClass) {
    this.targetClass = targetClass;
    this.isStatic = false;
    this.allowSuperclass = false;
    this.parameterTypes = new ArrayList<>();
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target method's static modifier presence
   * @param mode Static modifier presence, null means wildcard
   */
  public MethodPredicateBuilder withStatic(@Nullable Boolean mode) {
    this.isStatic = mode;
    return this;
  }

  /**
   * Define the target method's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public MethodPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  //////////////////////////////////// Name /////////////////////////////////////

  /**
   * Define the target method's name
   * @param name Method name, null means wildcard
   */
  public MethodPredicateBuilder withName(@Nullable String name) {
    this.name = name;
    return this;
  }

  //////////////////////////////////// Type /////////////////////////////////////

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   */
  public MethodPredicateBuilder withReturnType(@Nullable Class<?> type) {
    return withReturnType(type, false, Assignability.NONE);
  }

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   */
  public MethodPredicateBuilder withReturnType(@Nullable ClassHandle type) {
    return withReturnType(type == null ? null : type.get());
  }

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withReturnType(@Nullable Class<?> type, boolean allowBoxing, Assignability assignability) {
    if (type == null) {
      this.returnType = null;
      return this;
    }

    this.returnType = new ComparableType(type, allowBoxing, assignability);
    return this;
  }

  /**
   * Define the target method's return type
   * @param type Method return type, null means wildcard
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withReturnType(@Nullable ClassHandle type, boolean allowBoxing, Assignability assignability) {
    return withReturnType(type == null ? null : type.get(), allowBoxing, assignability);
  }

  ///////////////////////////////// Parameters //////////////////////////////////

  /**
   * Add more parameter types of the target method to the sequence
   * @param types Types to be present
   */
  public MethodPredicateBuilder withParameters(Class<?>... types) {
    for (Class<?> t : types)
      withParameter(t, false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target method to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withParameter(Class<?> generic, boolean allowBoxing, Assignability assignability) {
    this.parameterTypes.add(new ComparableType(generic, allowBoxing, assignability));
    return this;
  }

  /**
   * Add more parameter types of the target method to the sequence
   * @param types Types to be present
   */
  public MethodPredicateBuilder withParameters(ClassHandle... types) {
    for (ClassHandle t : types)
      withParameter(t.get(), false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target method to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public MethodPredicateBuilder withParameter(ClassHandle generic, boolean allowBoxing, Assignability assignability) {
    return withParameter(generic.get(), allowBoxing, assignability);
  }

  ////////////////////////////////// Superclass /////////////////////////////////

  /**
   * Define whether walking up into the superclass is allowed
   * @param mode Superclass walking mode
   */
  public MethodPredicateBuilder withAllowSuperclass(boolean mode) {
    this.allowSuperclass = mode;
    return this;
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public @Nullable MethodHandle optional() {
    try {
      return required();
    }

    catch (IncompletePredicateBuilderException e) {
      throw e;
    }

    catch (Exception e) {
      return null;
    }
  }

  @Override
  public MethodHandle required() throws Exception {
    // At least a name , a return type or parameter types are required
    if (name == null && returnType == null && parameterTypes.size() == 0)
      throw new IncompletePredicateBuilderException();

    return new MethodHandle(targetClass.get(), m -> {

      // Is inside of another class but superclass walking is disabled
      if (!allowSuperclass && m.getDeclaringClass() != targetClass.get())
        return false;

      // Static modifier mismatch
      if (isStatic != null && Modifier.isStatic(m.getModifiers()) != isStatic)
        return false;

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(m.getModifiers()) != isPublic)
        return false;

      // Name mismatch
      if (name != null && !m.getName().equalsIgnoreCase(name))
        return false;

      // Return type mismatch
      if (returnType != null && !returnType.matches(m.getReturnType()))
        return false;

      // Check parameters, if applicable
      int numParameters = parameterTypes.size();
      if (numParameters > 0) {
        Class<?>[] parameters = m.getParameterTypes();

        // Not exactly as many parameters as requested
        if (numParameters != parameters.length)
          return false;

        // Parameters need to match in sequence
        for (int i = 0; i < numParameters; i++) {
          if (!parameterTypes.get(i).matches(parameters[i]))
            return false;
        }
      }

      return true;
    });
  }
}
