package me.blvckbytes.bblibreflect;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibutil.APlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/13/2022

  The central reflection helper endpoint which offers all reflection
  and packet based routines.

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
@AutoConstruct
public class ReflectionHelper implements IReflectionHelper, Listener {

  private final Map<Player, ICustomizableViewer> viewers;
  private final APlugin plugin;
  private final MCReflect reflect;

  public ReflectionHelper(
    @AutoInject APlugin plugin,
    @AutoInject MCReflect reflect
  ) {
    this.plugin = plugin;
    this.reflect = reflect;
    this.viewers = new HashMap<>();
  }

  @Override
  public ICustomizableViewer getViewer(Player p) {
    ICustomizableViewer viewer = viewers.get(p);

    if (viewer != null)
      return viewer;

    viewer = new ICustomizableViewer() {

      @Override
      public boolean cannotRenderHexColors() {
        // TODO: Implement properly
        return true;
      }

      @Override
      public void sendPackets(Object... packets) {
        for (Object packet : packets)
          reflect.sendPacket(p, packet);
      }
    };

    viewers.put(p, viewer);
    return viewer;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Remove after a few ticks to allow for use after quit
    plugin.runTask(() -> viewers.remove(e.getPlayer()), 5);
  }
}
