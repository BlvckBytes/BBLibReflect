package me.blvckbytes.bblibreflect;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibdi.AutoInjectLate;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
public class ReflectionHelper implements IReflectionHelper {

  private final Class<?> PACKET_DATA_SERIALIZER;

  private final Map<Material, Integer> burningTimes;
  private final Map<RClass, Class<?>> classes;
  private final ILogger logger;

  // Server version information
  @Getter private final String versionStr;
  @Getter private final int[] versionNumbers;
  @Getter private final boolean refactored;

  public ReflectionHelper(
    @AutoInject ILogger logger
  ) throws Exception {
    this.logger = logger;

    this.classes = new HashMap<>();
    this.burningTimes = new HashMap<>();

    this.versionStr = findVersion();
    this.versionNumbers = parseVersion(this.versionStr);
    this.refactored = this.versionNumbers[1] >= 17;

    this.PACKET_DATA_SERIALIZER = getClass(RClass.PACKET_DATA_SERIALIZER);
    if (PACKET_DATA_SERIALIZER == null)
      throw new ClassNotFoundException("Could not find the packet data serializer class.");
  }

  @Override
  public Object createEmptyPacket(Class<?> c) {
    try {
      Constructor<?> packetC = null;
      boolean isPds = false;

      // Loop through all available constructors
      for (Constructor<?> constructor : c.getDeclaredConstructors()) {
        isPds = (
          constructor.getParameterCount() == 1 &&
          constructor.getParameterTypes()[0].equals(PACKET_DATA_SERIALIZER)
        );

        // Found a matching constructor, take and stop looking
        if (constructor.getParameterCount() == 0 || isPds) {
          packetC = constructor;
          packetC.setAccessible(true);
          break;
        }
      }

      // No applicable constructor locatable
      if (packetC == null)
        throw new IllegalStateException("Could not locate a valid packet constructor!");

      // Is a packet data serializer accepting constructor
      if (isPds) {
        // Create a new empty buffer to emptiness read from
        Constructor<?> pdsC = PACKET_DATA_SERIALIZER.getConstructor(ByteBuf.class);
        Object buf = pdsC.newInstance(Unpooled.wrappedBuffer(new byte[1024]));
        return packetC.newInstance(buf);
      }

      // Is a plain default constructor
      return packetC.newInstance();
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  @Override
  public Class<?> getClass(RClass rc) throws ClassNotFoundException {
    Class<?> c = classes.get(rc);

    if (c != null)
      return c;

    c = rc.resolve(refactored, this.versionStr);
    classes.put(rc, c);
    return c;
  }

  @Override
  public Optional<Integer> getBurnTime(Material mat) {
    Integer dur = burningTimes.get(mat);
    if (dur != null)
      return Optional.of(dur);

    try {
      Method newCraftStack = getClass(RClass.CRAFT_ITEM_STACK).getDeclaredMethod("asNewCraftStack", getClass(RClass.ITEM));

      // There's only one static Map getter function within the furnace
      // class, which returns a map of item to burning duration in ticks
      Method lutGetter = Arrays.stream(getClass(RClass.TILE_ENTITY_FURNACE).getDeclaredMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(Map.class))
        .findFirst()
        .orElse(null);

      // Could not access the method
      if (lutGetter == null)
        return Optional.empty();

      // Iterate all entries
      Map<?, ?> lut = (Map<?, ?>) lutGetter.invoke(null);
      for (Map.Entry<?, ?> e : lut.entrySet()) {
        Object craftStack = newCraftStack.invoke(null, e.getKey());
        Material m = (Material) craftStack.getClass().getMethod("getType").invoke(craftStack);

        // Material mismatch, continue
        if (!mat.equals(m))
          continue;

        dur = (Integer) e.getValue();
        burningTimes.put(mat, dur);
        return Optional.of(dur);
      }

      return Optional.empty();
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  /**
   * Find the server's version by looking at craftbukkit's package
   * @return Version part of the package
   */
  private String findVersion() {
    return Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get the major, minor and revision version numbers the server's running on
   * @return [major, minor, revision]
   */
  private int[] parseVersion(String version) {
    String[] data = version.split("_");
    return new int[] {
      Integer.parseInt(data[0].substring(1)), // remove leading v
      Integer.parseInt(data[1]),
      Integer.parseInt(data[2].substring(1)) // Remove leading R
    };
  }
}
