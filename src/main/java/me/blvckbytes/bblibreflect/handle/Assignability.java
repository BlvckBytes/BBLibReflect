package me.blvckbytes.bblibreflect.handle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Represents all assignability modes when it comes to comparing two types.

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
public enum Assignability {

  /*
    Definition of assignability:

    // Let there be two types
    TypeA fieldA;
    TypeB fieldB;

    // If fieldB can be >assigned to< fieldA, TypeB is assignable to TypeA
    fieldA = fieldB;

    // If fieldA can be >assigned to< fieldB, TypeA is assignable to TypeB
    fieldB = fieldA;
   */

  // Can the target's (searched) type be assigned to the type's type?
  TARGET_TO_TYPE,

  // Can the type's type be assigned to the target's (searched) type?
  TYPE_TO_TARGET,

  // Assignability is to be ignored
  NONE
  ;

}
