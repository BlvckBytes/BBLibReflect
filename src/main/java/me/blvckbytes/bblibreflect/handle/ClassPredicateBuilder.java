package me.blvckbytes.bblibreflect.handle;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/20/2022

  Incrementally builds up all available parameters to execute a class
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
public class ClassPredicateBuilder extends APredicateBuilder<ClassHandle> {

  private final ClassHandle targetClass;
  private @Nullable Boolean isPublic;
  private @Nullable Boolean isStatic;
  private int skip;

  /**
   * Create a new class predicate builder on a class handle
   * @param targetClass Class to search through
   */
  public ClassPredicateBuilder(ClassHandle targetClass) {
    this.targetClass = targetClass;
  }

  ////////////////////////////////// Modifiers //////////////////////////////////

  /**
   * Define the target class's public modifier presence
   * @param mode Public modifier presence, null means wildcard
   */
  public ClassPredicateBuilder withPublic(@Nullable Boolean mode) {
    this.isPublic = mode;
    return this;
  }

  /**
   * Define the target class's static modifier presence
   * @param mode Static modifier presence, null means wildcard
   */
  public ClassPredicateBuilder withStatic(@Nullable Boolean mode) {
    this.isStatic = mode;
    return this;
  }

  /////////////////////////////////// Skipping //////////////////////////////////

  /**
   * Define how many matches to skip
   * @param skip Number of matches to skip
   */
  public ClassPredicateBuilder withSkip(int skip) {
    this.skip = skip;
    return this;
  }

  ////////////////////////////////// Retrieval //////////////////////////////////

  @Override
  public @Nullable ClassHandle optional() {
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
  public ClassHandle required() throws Exception {
    return new ClassHandle(targetClass.get(), (c, mc) -> {

      // Static modifier mismatch
      if (isStatic != null && Modifier.isStatic(c.getModifiers()) != isStatic)
        return false;

      // Public modifier mismatch
      if (isPublic != null && Modifier.isPublic(c.getModifiers()) != isPublic)
        return false;

      // Everything matches, while skip > matchCounter, count up
      if (skip > mc)
        return null;

      return true;
    });
  }
}
