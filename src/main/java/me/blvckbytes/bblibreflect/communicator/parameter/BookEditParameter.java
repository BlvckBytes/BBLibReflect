package me.blvckbytes.bblibreflect.communicator.parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  A parameter used to define a book edit session as well as it's events.

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
@Getter
@Setter
@AllArgsConstructor
public class BookEditParameter implements ICommunicatorParameter {

  // Target player
  private Player player;

  // Pages to display initially
  private List<String> pages;

  // Callback for submitting the book edit
  private Consumer<List<String>> submit;

  // Optional Callback hook for cancelling the request prematurely
  private @Nullable Consumer<Runnable> cancelHook;

}
