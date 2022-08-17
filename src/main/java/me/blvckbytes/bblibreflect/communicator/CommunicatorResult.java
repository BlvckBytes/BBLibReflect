package me.blvckbytes.bblibreflect.communicator;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/17/2022

  Represents all possible results of invoking a communicator's routine.

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
public enum CommunicatorResult {

  // Operation succeeded with no issues
  SUCCESS,

  // A runtime reflection error occurred
  REFLECTION_ERROR,

  // This operation requires an ICustomizableViewer,
  // as some of the parameter's data is customizable
  VIEWER_REQUIRED,

  // There is no communicator registered which
  // matches the requested parameter type
  UNKNOWN_PARAMETER_TYPE

}
