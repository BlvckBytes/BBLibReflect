package me.blvckbytes.bblibreflect;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.ConstructorHandle;
import me.blvckbytes.bblibreflect.handle.EnumHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.UnsafeSupplier;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

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

  // Size in bytes of the fake byte buffer size used to create
  // zero-ed packets if there's no empty default constructor
  private static final int FAKE_BUF_SIZE = 1024;

  private final ClassHandle C_PACKET_DATA_SERIALIZER;
  private final MethodHandle M_CIS__AS_NEW_CRAFT_STACK, M_FURNACE__GET_LUT, M_CIS__GET_TYPE;

  private final Map<ClassHandle, UnsafeSupplier<Object>> packetConstructors;
  private final Map<Material, Integer> burningTimes;
  private final Map<RClass, ClassHandle> classes;
  private final ILogger logger;

  private final ByteBuf byteBuf;
  private final Object packetDataSerializer;

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
    this.packetConstructors = new HashMap<>();

    this.versionStr = findVersion();
    this.versionNumbers = parseVersion(this.versionStr);
    this.refactored = this.versionNumbers[1] >= 17;

    ClassHandle C_ITEM = getClass(RClass.ITEM);
    ClassHandle C_CIS = getClass(RClass.CRAFT_ITEM_STACK);
    ClassHandle C_TEF = getClass(RClass.TILE_ENTITY_FURNACE);

    C_PACKET_DATA_SERIALIZER = getClass(RClass.PACKET_DATA_SERIALIZER);
    M_CIS__AS_NEW_CRAFT_STACK = C_CIS.locateMethod().withName("asNewCraftStack").withParameters(C_ITEM).withStatic(true).required();
    M_CIS__GET_TYPE = C_CIS.locateMethod().withName("getType").withReturnType(Material.class).required();

    M_FURNACE__GET_LUT = C_TEF.locateMethod()
      .withReturnType(Map.class)
      .withReturnGeneric(C_ITEM)
      .withReturnGeneric(Integer.class)
      .withStatic(true)
      .required();

    ConstructorHandle CTOR_PACKET_DATA_SERIALIZER = C_PACKET_DATA_SERIALIZER.locateConstructor().withParameters(ByteBuf.class).required();

    // Create a single serializer on top of a byte buffer which itself is stateless
    // to be reused whenever packets are to be created (rewinding the buffer first)
    this.byteBuf = Unpooled.wrappedBuffer(new byte[FAKE_BUF_SIZE]);
    this.packetDataSerializer = CTOR_PACKET_DATA_SERIALIZER.newInstance(this.byteBuf);
  }

  @Override
  public Object createEmptyPacket(ClassHandle c) {
    try {
      UnsafeSupplier<Object> creator = packetConstructors.get(c);

      // Constructor not yet cached
      if (creator == null) {

        // Try to use the empty default constructor
        ConstructorHandle empty = c.locateConstructor().optional();

        ConstructorHandle constructor = (
          // That didn't yield anything, now require there to
          // be a packet data serializer constructor
          empty == null ?
            c.locateConstructor().withParameters(C_PACKET_DATA_SERIALIZER).required() :
            empty
        );

        // Empty default constructor
        if (constructor.getParameterCount() == 0)
          creator = constructor::newInstance;

        // Packet data serializer constructor
        else {
          creator = () -> {
            // TODO: Think about a way to cache packet instances and re-use them (thread safe!)
            synchronized (byteBuf) {
              // Rewind the buffer and create a new zero-ed packet
              byteBuf.setIndex(0, FAKE_BUF_SIZE);
              return constructor.newInstance(packetDataSerializer);
            }
          };
        }

        // Store in cache
        packetConstructors.put(c, creator);
      }

      return creator.get();
    } catch (Exception e) {
      logger.logError(e);
      return null;
    }
  }

  @Override
  public ClassHandle getClass(RClass rc) throws ClassNotFoundException {
    ClassHandle c = classes.get(rc);

    if (c != null)
      return c;

    c = new ClassHandle(rc.resolve(refactored, this.versionStr));
    classes.put(rc, c);
    return c;
  }

  @Override
  public @Nullable ClassHandle getClassOptional(RClass rc) {
    try {
      return getClass(rc);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  public @Nullable EnumHandle getEnumOptional(RClass rc) {
    try {
      return getClass(rc).asEnum();
    } catch (ClassNotFoundException | IllegalStateException e) {
      return null;
    }
  }

  @Override
  public Optional<Integer> getBurnTime(Material mat) {
    Integer dur = burningTimes.get(mat);
    if (dur != null)
      return Optional.of(dur);

    try {
      // Iterate all entries
      Map<?, ?> lut = (Map<?, ?>) M_FURNACE__GET_LUT.invoke(null);
      for (Map.Entry<?, ?> e : lut.entrySet()) {
        Object craftStack = M_CIS__AS_NEW_CRAFT_STACK.invoke(null, e.getKey());
        Material m = (Material) M_CIS__GET_TYPE.invoke(craftStack);

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
