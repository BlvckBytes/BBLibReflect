package me.blvckbytes.bblibreflect.communicator.parameter;

import com.mojang.authlib.GameProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.component.IComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  A parameter used to define which player info update to perform when using
  the PlayerInfoCommunicator.

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
public class PlayerInfoParameter {

  @Setter
  @Getter
  public static class Entry {

    // Name displayed in the tab list, null means reset
    private @Nullable IComponent name;

    // Player represented by this entry
    // If this field is provided, the following values
    // will be inferred: entityId, gameMode and profile
    private @Nullable Player player;

    // Latency of the entity being updated
    private @Nullable Integer latency;

    // Game mode of the entity being updated
    private @Nullable EnumGameMode gameMode;

    // Game profile of the entity being updated
    private @Nullable GameProfile profile;

    /**
     * Used to add or remove a player by using {@link PlayerInfoAction#ADD_PLAYER}
     * or {@link PlayerInfoAction#REMOVE_PLAYER} and inferring all other values
     * @param player Target player
     */
    public Entry(@NonNull Player player) {
      this.player = player;
    }

    /**
     * Used to update a player's name by using {@link PlayerInfoAction#UPDATE_DISPLAY_NAME}
     * @param player Target player
     * @param name Name to set, null means reset
     */
    public Entry(@NonNull Player player, @Nullable IComponent name) {
      this.player = player;
      this.name = name;
    }

    /**
     * Used to update a player's game mode by using {@link PlayerInfoAction#UPDATE_GAME_MODE}
     * @param player Target player
     * @param gameMode Game mode to set
     */
    public Entry(@NonNull Player player, @NonNull EnumGameMode gameMode) {
      this.player = player;
      this.gameMode = gameMode;
    }

    /**
     * Used to update a player's latency by using {@link PlayerInfoAction#UPDATE_LATENCY}
     * @param player Target player
     * @param latency Latency to set
     */
    public Entry(@NonNull Player player, int latency) {
      this.player = player;
      this.latency = latency;
    }

    /**
     * Used to add an entity by using {@link PlayerInfoAction#ADD_PLAYER}
     * @param profile Game profile of the entity
     * @param name Name to set, null means reset
     * @param gameMode Game mode of the entity
     * @param latency Latency of the entity
     */
    public Entry(
      @NonNull GameProfile profile,
      @Nullable IComponent name,
      @NonNull EnumGameMode gameMode,
      int latency
    ) {
      this.profile = profile;
      this.name = name;
      this.gameMode = gameMode;
      this.latency = latency;
    }

    /**
     * Used to update an entity's name by using {@link PlayerInfoAction#UPDATE_DISPLAY_NAME}
     * @param profile Game profile of the target entity
     * @param name Name to set, null means reset
     */
    public Entry(@NonNull GameProfile profile, @Nullable IComponent name) {
      this.profile = profile;
      this.name = name;
    }

    /**
     * Used to update an entity's game mode by using {@link PlayerInfoAction#UPDATE_GAME_MODE}
     * @param profile Game profile of the target entity
     * @param gameMode Game mode to set
     */
    public Entry(@NonNull GameProfile profile, @NonNull EnumGameMode gameMode) {
      this.profile = profile;
      this.gameMode = gameMode;
    }

    /**
     * Used to update an entity's latency by using {@link PlayerInfoAction#UPDATE_LATENCY}
     * @param profile Game profile of the target entity
     * @param latency Latency to set
     */
    public Entry(@NonNull GameProfile profile, int latency) {
      this.profile = profile;
      this.latency = latency;
    }

    /**
     * Used to remove an entity by using {@link PlayerInfoAction#REMOVE_PLAYER}
     * @param profile Game profile of the target entity
     */
    public Entry(@NonNull GameProfile profile) {
      this.profile = profile;
    }

    /**
     * Resolve the applicative game profile value
     * @param M_CRAFT_PLAYER__GET_PROFILE Method to extract the game profile from
     *                                    a {@link Player} when applied on said reference.
     */
    public GameProfile resolveGameProfile(
      MethodHandle M_CRAFT_PLAYER__GET_PROFILE
    ) throws IllegalStateException, InvocationTargetException, IllegalAccessException {

      if (profile != null)
        return profile;

      if (player != null)
        return (GameProfile) M_CRAFT_PLAYER__GET_PROFILE.invoke(player);

      throw new IllegalStateException("Cannot resolve game profile due to a lack of parameters.");
    }

    /**
     * Resolve the applicative latency value
     * @param interceptor Interceptor used to resolve latency values from players
     */
    public int resolveLatency(IPacketInterceptor interceptor) throws IllegalStateException {
      if (latency != null)
        return latency;

      if (player != null)
        return interceptor.getPlayerAsViewer(player).getPing();

      throw new IllegalStateException("Cannot resolve latency due to a lack of parameters.");
    }

    /**
     * Resolve the applicative game mode value
     */
    public EnumGameMode resolveGameMode() throws IllegalStateException {
      if (gameMode != null)
        return gameMode;

      if (player != null)
        return EnumGameMode.getFromPlayer(player);

      throw new IllegalStateException("Cannot resolve game mode due to a lack of parameters.");
    }
  }

  // Dictates which action to dispatch and also decides on
  // which of the optional parameters are needed at a time.
  private PlayerInfoAction action;

  // List of entries to perform that specified action on
  private List<Entry> entries;

}
