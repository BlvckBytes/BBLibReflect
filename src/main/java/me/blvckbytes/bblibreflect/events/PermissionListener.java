package me.blvckbytes.bblibreflect.events;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibdi.IAutoConstructed;
import me.blvckbytes.bblibreflect.MCReflect;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/25/2022

  Proxies modifying calls to the CraftPlayer's PermissibleBase's HashMap field
  called "permissions" in a non-modifying way, debounces those calls and then
  invokes a local handler routine. Permissions are diffed into separate added and
  removed lists for convenient access, which will be contained in the emitted
  PlayerPermissionsChangedEvent, which also offers all currently active permissions.

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
public class PermissionListener implements Listener, IAutoConstructed {

  // Ticks that need to elapse until the last modifying call is actually routed
  private static final long DEBOUNCE_TICKS = 10;

  private final MCReflect refl;
  private final APlugin plugin;
  private final ILogger logger;

  // Vanilla references of the proxied field for every player
  private final Map<Player, Object> vanillaRefs;

  // The previous permission list (last permission change call) for every player
  private final Map<Player, List<String>> previousPermissions;

  public PermissionListener(
    @AutoInject MCReflect refl,
    @AutoInject APlugin plugin,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.plugin = plugin;
    this.logger = logger;

    this.vanillaRefs = new HashMap<>();
    this.previousPermissions = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void cleanup() {
    // Unproxy all players on unload
    for (Player t : Bukkit.getOnlinePlayers())
      unproxyPermissions(t);
  }

  @Override
  public void initialize() {
    // Proxy all players on load
    for (Player t : Bukkit.getOnlinePlayers())
      proxyPermissions(t);
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler(priority = EventPriority.LOWEST)
  public void onLogin(PlayerLoginEvent e) {
    // Proxy on join
    proxyPermissions(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuit(PlayerQuitEvent e) {
    // Unproxy on quit
    unproxyPermissions(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Fire the {@link PlayerPermissionsChangedEvent} after diffing the player's permission list
   * @param p Target player
   * @param permissions List of currently active permissions
   */
  private void fireEvent(Player p, List<String> permissions) {
    List<String> added = new ArrayList<>();
    List<String> removed = new ArrayList<>();
    List<String> previous = previousPermissions.getOrDefault(p, new ArrayList<>());

    for (String prev : previous) {
      // This permission was owned previously but is missing now
      if (!permissions.contains(prev))
        removed.add(prev);
    }

    for (String curr : permissions) {
      // This permission wasn't owned previously and thus has been added
      if (!previous.contains(curr))
        added.add(curr);
    }

    Bukkit.getPluginManager().callEvent(
      new PlayerPermissionsChangedEvent(p, permissions, added, removed)
    );
  }

  /**
   * Permission change handler, called whenever a player's permissions change
   * @param p Target player
   * @param permissions List of currently active permissions
   */
  private void onPermissionChange(Player p, List<String> permissions) {
    // Handle firing the delta event
    fireEvent(p, permissions);

    // Save these permissions as the previous state
    previousPermissions.put(p, permissions);
  }

  /**
   * Unproxy the permissions field for a given player by reverting
   * back to the vanilla reference
   * @param p Target player
   */
  private void unproxyPermissions(Player p) {
    // Get the vanilla reference from the local map, skip non-proxied players
    Object vanillaRef = vanillaRefs.get(p);
    if (vanillaRef == null)
      return;

    // Restore the vanilla reference
    try {
      Object cp = refl.getCraftPlayer(p);
      refl.setFieldByName(
        refl.getFieldByType(cp, PermissibleBase.class, 0),
        "permissions", vanillaRef
      );

      // Remove the undone ref
      vanillaRefs.remove(p);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Get a list of active permissions from a map of attachments
   * @param permissions Map of attachments
   * @return List of active permissions
   */
  private List<String> getPermissions(Map<String, PermissionAttachmentInfo> permissions) {
    return permissions.values()
      .stream()
      .filter(PermissionAttachmentInfo::getValue)
      .map(PermissionAttachmentInfo::getPermission)
      .collect(Collectors.toList());
  }

  /**
   * Create a new permission-field proxy for a given player
   * @param p Target player
   * @param permissions Vanilla map
   * @return New map that can replace the vanilla list value
   */
  private Object createPermissionProxy(Player p, Map<String, PermissionAttachmentInfo> permissions) {
    // Create a new proxied map
    return Proxy.newProxyInstance(
      permissions.getClass().getClassLoader(),
      new Class[]{ Map.class },

      // Create an anonymous implementation here, since it's pretty basic and too specific
      new InvocationHandler() {

        // Handle of the debounce task used to debounce map call bursts
        private BukkitTask debounceTask = null;
        private long debounceCreation = 0;

        // Lock to synchronize map calls (as I don't know all possible callers)
        private final ReentrantLock lock = new ReentrantLock();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          // Only intercept on adding items (done after clearing when recalculating recursively)
          if (!method.getName().equals("put"))
            return method.invoke(permissions, args);

          if (
            // No debounce task created yet
            debounceTask == null ||
            // Or the previously created task has reached more than half of it's lifespan
            System.currentTimeMillis() - debounceCreation > (DEBOUNCE_TICKS * (1000 / 20 / 2))
          ) {

            // Lock while operating on the local debounce task handle
            lock.lock();

            // Cancel the previous debounce task
            if (debounceTask != null) {
              debounceTask.cancel();
              debounceTask = null;
            }

            // Create a new debounce task
            debounceCreation = System.currentTimeMillis();
            debounceTask = plugin.runTask(() -> {
              onPermissionChange(p, getPermissions(permissions));
              debounceTask = null;
            }, DEBOUNCE_TICKS);

            // Done with operations, unlock
            lock.unlock();
          }

          return method.invoke(permissions, args);
        }
      }
    );
  }

  /**
   * Proxy the permissions field of a given player by setting a
   * read-only (non-modifying) proxy on the permissions map
   * @param p Target player
   */
  @SuppressWarnings("unchecked")
  private void proxyPermissions(Player p) {
    try {
      Object cp = refl.getCraftPlayer(p);
      Object pb = refl.getFieldByType(cp, PermissibleBase.class, 0);
      Map<String, PermissionAttachmentInfo> perms = (Map<String, PermissionAttachmentInfo>) refl.getFieldByName(pb, "permissions");

      // Set field to the proxy reference
      if (refl.setFieldByName(pb, "permissions", createPermissionProxy(p, perms))) {
        // Save the vanilla reference
        this.vanillaRefs.put(p, perms);
      }
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
