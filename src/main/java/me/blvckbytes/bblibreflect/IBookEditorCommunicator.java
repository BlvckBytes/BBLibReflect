package me.blvckbytes.bblibreflect;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Communicates managing a book editor GUI and retrieving the entered text.

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
public interface IBookEditorCommunicator {

  /**
   * Create a new book editor GUI request and retrieve it's pages after committing
   * @param p Target player
   * @param pages Initial pages to display
   * @param submit Book pages after committing, null on cancel
   * @return Success state
   */
  boolean initBookEditor(Player p, List<String> pages, Consumer<List<String>> submit);

  /**
   * Quit an existing book editor session silently
   * @param p Target player
   */
  void quitBookEditor(Player p);
}
