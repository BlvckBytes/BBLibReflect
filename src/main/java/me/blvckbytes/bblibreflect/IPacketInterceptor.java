package me.blvckbytes.bblibreflect;

import org.bukkit.entity.Player;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Public interfaces to handle the registration process of a packet modifier.

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
public interface IPacketInterceptor {

  // TODO: Inject packet into incoming stream

  /**
   * Register a new modifier for all players
   * @param modifier Packet modifier to register
   * @param priority Priority of this modifier
   */
  void register(IPacketModifier modifier, ModificationPriority priority);

  /**
   * Unregister an existing modifier for all players
   * @param modifier Packet modifier to unregister
   */
  void unregister(IPacketModifier modifier);

  /**
   * Check if a modifier is already registered
   * @param modifier Modifier to check
   * @return True if registered, false otherwise
   */
  boolean isRegistered(IPacketModifier modifier);

  /**
   * Register a new modifier for a specific player
   * @param target Player to target with this modifier
   * @param modifier Packet modifier to register
   * @param priority Priority of this modifier
   */
  void registerSpecific(UUID target, IPacketModifier modifier, ModificationPriority priority);

  /**
   * Unegister an existing modifier for a specific player
   * @param target Player targetted with this modifier
   * @param modifier Packet modifier to unregister
   */
  void unregisterSpecific(UUID target, IPacketModifier modifier);

  /**
   * Check if a specific modifier is already registered
   * @param target Player targetted with this modifier
   * @param modifier Modifier to check
   * @return True if registered, false otherwise
   */
  boolean isRegisteredSpecific(UUID target, IPacketModifier modifier);

  /**
   * Get a player as a wrapped customizable viewer
   * @param p Target player
   * @return ICustomizableViewer instance
   */
  ICustomizableViewer getPlayerAsViewer(Player p);

}
