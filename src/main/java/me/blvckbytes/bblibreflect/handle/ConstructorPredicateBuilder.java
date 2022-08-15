package me.blvckbytes.bblibreflect.handle;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Incrementally builds up all available parameters to execute a constructor
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
public class ConstructorPredicateBuilder extends APredicateBuilder<ConstructorHandle> {

  private final ClassHandle targetClass;
  private @Nullable Boolean isPublic;
  private final List<ComparableType> parameterTypes;

  /**
   * Create a new constructor predicate builder on a class handle
   * @param targetClass Class to search through
   */
  public ConstructorPredicateBuilder(ClassHandle targetClass) {
    this.targetClass = targetClass;
    this.parameterTypes = new ArrayList<>();
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target constructor's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public ConstructorPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  ///////////////////////////////// Parameters //////////////////////////////////

  /**
   * Add more parameter types of the target constructor to the sequence
   * @param types Types to be present
   */
  public ConstructorPredicateBuilder withParameters(Class<?>... types) {
    for (Class<?> t : types)
      withParameter(t, false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target constructor to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public ConstructorPredicateBuilder withParameter(Class<?> generic, boolean allowBoxing, Assignability assignability) {
    this.parameterTypes.add(new ComparableType(generic, allowBoxing, assignability));
    return this;
  }

  /**
   * Add more parameter types of the target constructor to the sequence
   * @param types Types to be present
   */
  public ConstructorPredicateBuilder withParameters(ClassHandle... types) {
    for (ClassHandle t : types)
      withParameter(t.get(), false, Assignability.NONE);
    return this;
  }

  /**
   * Add another parameter type of the target constructor to the sequence
   * @param generic Type to be present
   * @param allowBoxing Whether boxed and unboxed versions of the type are equivalent
   * @param assignability Whether assignability matching is enabled, and in which direction
   */
  public ConstructorPredicateBuilder withParameter(ClassHandle generic, boolean allowBoxing, Assignability assignability) {
    return withParameter(generic.get(), allowBoxing, assignability);
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public @Nullable ConstructorHandle optional() {
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
  public ConstructorHandle required() throws Exception {
    return new ConstructorHandle(targetClass.get(), c -> {

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(c.getModifiers()) != isPublic)
        return false;

      // Not exactly as many parameters as requested
      int numParameters = parameterTypes.size();
      if (c.getParameterCount() != numParameters)
        return false;

      // Check parameters, if applicable
      Class<?>[] parameters = c.getParameterTypes();

      // Parameters need to match in sequence
      for (int i = 0; i < numParameters; i++) {
        if (!parameterTypes.get(i).matches(parameters[i]))
          return false;
      }

      return true;
    });
  }
}
