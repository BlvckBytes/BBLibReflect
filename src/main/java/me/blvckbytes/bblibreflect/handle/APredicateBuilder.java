package me.blvckbytes.bblibreflect.handle;

import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  The base of all predicate builder implementations.

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
public abstract class APredicateBuilder<T> {

  /**
   * Get the predicate's result and return null if it couldn't be located
   */
  public abstract @Nullable T optional();

  /**
   * Get the predicate's result and require that it's not null
   * @throws Exception Not found exception if the result could not be located
   */
  public abstract T required() throws Exception;

}
