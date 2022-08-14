package me.blvckbytes.bblibreflect.handle;

import java.lang.reflect.Field;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  A predicate function to search through a list of fields and find a match.

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
public interface IFieldPredicate {

  /**
   * Field matching predecate, used to decide what field to take
   * @param f Field reference
   * @param isInsideSuperclass If it occurs inside of a superclass
   * @param totalTypeCount How many times that field has been counted
   * @return null means completely doesn't match, false will increase the type count and true will take that field
   */
  Boolean matches(Field f, boolean isInsideSuperclass, int totalTypeCount);

}
