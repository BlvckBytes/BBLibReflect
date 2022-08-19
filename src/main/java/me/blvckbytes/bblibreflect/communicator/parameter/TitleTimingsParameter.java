package me.blvckbytes.bblibreflect.communicator.parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/16/2022

  A parameter used to define with what timings to display titles and subtitles.

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
@Setter
@Getter
@AllArgsConstructor
public class TitleTimingsParameter implements ICommunicatorParameter {

  // Time in ticks to take while fading the title in
  private int fadeIn;

  // Time in ticks to display the title
  private int duration;

  // Time in ticks to take while fading the title out
  private int fadeOut;

}
