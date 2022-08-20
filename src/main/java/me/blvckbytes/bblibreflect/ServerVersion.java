package me.blvckbytes.bblibreflect;

import lombok.Getter;
import org.bukkit.Bukkit;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/19/2022

  Lists all minecraft server versions which have the possibility of being
  supported within features this library.

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
public enum ServerVersion {

  // 1.7.x
  V1_7,
  V1_7_1,
  V1_7_2,
  V1_7_3,
  V1_7_4,
  V1_7_5,
  V1_7_6,
  V1_7_7,
  V1_7_8,
  V1_7_9,
  V1_7_10,

  // 1.8.x
  V1_8,
  V1_8_1,
  V1_8_2,
  V1_8_3,
  V1_8_4,
  V1_8_5,
  V1_8_6,
  V1_8_7,
  V1_8_8,
  V1_8_9,

  // 1.9.x
  V1_9,
  V1_9_1,
  V1_9_2,
  V1_9_3,
  V1_9_4,

  // 1.10.x
  V1_10,
  V1_10_1,
  V1_10_2,

  // 1.11.x
  V1_11,
  V1_11_1,
  V1_11_2,

  // 1.12.x
  V1_12,
  V1_12_1,
  V1_12_2,

  // 1.13.x
  V1_13,
  V1_13_1,
  V1_13_2,

  // 1.14.x
  V1_14,
  V1_14_1,
  V1_14_2,
  V1_14_3,
  V1_14_4,

  // 1.15.x
  V1_15,
  V1_15_1,
  V1_15_2,

  // 1.16.x
  V1_16,
  V1_16_1,
  V1_16_2,
  V1_16_3,
  V1_16_4,
  V1_16_5,

  // 1.17.x
  V1_17,
  V1_17_1,

  // 1.18.x
  V1_18,
  V1_18_1,
  V1_18_2,

  // 1.19.x
  V1_19,
  V1_19_1,
  V1_19_2,
  ;

  private static final ServerVersion[] values;

  @Getter private static final ServerVersion first, current, latest;

  static {
    // Cache values for quick access
    values = values();

    // Find first and last versions
    first = values[0];
    latest = values[values.length - 1];

    // Parse current version
    current = parseCurrentVersion();
  }

  /**
   * Checks whether this version is greater than or equal to another version
   * @param other Version to compare against
   * @return True if greater or equal, false if smaller
   */
  public boolean greaterThanOrEqual(ServerVersion other) {
    return ordinal() >= other.ordinal();
  }

  /**
   * Checks whether this version is greater than another version
   * @param other Version to compare against
   * @return True if greater, false if smaller or equal
   */
  public boolean greaterThan(ServerVersion other) {
    return ordinal() > other.ordinal();
  }

  /**
   * Checks whether this version is less than another version
   * @param other Version to compare against
   * @return True if less, false if greater or equal
   */
  public boolean lessThan(ServerVersion other) {
    return ordinal() < other.ordinal();
  }

  /**
   * Checks whether this version is less than or equal to another version
   * @param other Version to compare against
   * @return True if less or equal, false if greater
   */
  public boolean lessThanOrEqual(ServerVersion other) {
    return ordinal() <= other.ordinal();
  }

  /**
   * Try to parse the current version based on craftbukkit's package name
   * @return Parsed version or latest, if the version could not be parsed
   */
  private static ServerVersion parseCurrentVersion() {
    try {
      return ServerVersion.valueOf(
        Bukkit.getServer().getClass().getName().split("\\.")[3]
          // Remove revision prefix from patch version
          .replaceAll("_R(\\d+)", "_$1")
          .toUpperCase()
      );
    } catch (Exception e) {
      return latest;
    }
  }
}
