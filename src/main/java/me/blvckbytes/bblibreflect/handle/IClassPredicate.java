package me.blvckbytes.bblibreflect.handle;

import java.lang.reflect.Method;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/20/2022

  A predicate function to search through a list of classes and find a match.

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
@FunctionalInterface
public interface IClassPredicate {

  /**
   * Tests whether a given class matches the requirements
   * @param c Class in question
   * @param counter Counter, increases by one whenever the predicate yields null
   * @return True if matching, false otherwise, null to increase the passed counter once
   */
  Boolean matches(Class<?> c, int counter);

}
