package me.blvckbytes.bblibreflect.communicator;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import me.blvckbytes.bblibreflect.handle.Assignability;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.UnsafeSupplier;
import me.blvckbytes.bblibutil.component.IComponent;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  The base of all packet communicator implementations. Holds commonly used handles,
  the packet class that's managed, whether a viewer is required as well as all
  required injected dependency references. Implements the communicator API in a
  very generally applicative way so inheritors don't have to, but may.

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
public abstract class APacketCommunicator<T extends ICommunicatorParameter> {

  protected final ILogger logger;
  protected final IReflectionHelper helper;
  protected final IPacketInterceptor interceptor;

  protected final FieldHandle F_CRAFT_SERVER__MINECRAFT_SERVER;

  protected final MethodHandle M_CHAT_SERIALIZER__FROM_JSON, M_CRAFT_ITEM_STACK__AS_NMS_COPY,
    M_CRAFT_PLAYER__GET_PROFILE, M_CRAFT_PLAYER__GET_HANDLE ;

  protected final ClassHandle C_CHAT_SERIALIZER, C_BASE_COMPONENT, C_CRAFT_ITEM_STACK, C_ITEM_STACK,
    C_CRAFT_PLAYER, C_CRAFT_SERVER, C_MINECRAFT_SERVER, C_ENTITY_PLAYER;

  protected final Object O_MINECRAFT_SERVER;

  @Getter
  private final ClassHandle packetType;

  @Getter
  private final boolean requiresViewer;

  public APacketCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    UnsafeSupplier<ClassHandle> packetType
  ) throws Exception {
    this(logger, helper, interceptor, requiresViewer, packetType.get());
  }

  public APacketCommunicator(
    ILogger logger,
    IReflectionHelper helper,
    IPacketInterceptor interceptor,
    boolean requiresViewer,
    ClassHandle packetType
  ) throws Exception {
    this.logger = logger;
    this.helper = helper;
    this.interceptor = interceptor;
    this.requiresViewer = requiresViewer;
    this.packetType = packetType;

    C_CHAT_SERIALIZER  = helper.getClass(RClass.CHAT_SERIALIZER);
    C_BASE_COMPONENT   = helper.getClass(RClass.I_CHAT_BASE_COMPONENT);
    C_CRAFT_ITEM_STACK = helper.getClass(RClass.CRAFT_ITEM_STACK);
    C_ITEM_STACK       = helper.getClass(RClass.ITEM_STACK);
    C_CRAFT_PLAYER     = helper.getClass(RClass.CRAFT_PLAYER);
    C_ENTITY_PLAYER    = helper.getClass(RClass.ENTITY_PLAYER);
    C_CRAFT_SERVER     = helper.getClass(RClass.CRAFT_SERVER);
    C_MINECRAFT_SERVER = helper.getClass(RClass.MINECRAFT_SERVER);

    M_CHAT_SERIALIZER__FROM_JSON = C_CHAT_SERIALIZER.locateMethod()
      .withParameters(JsonElement.class)
      .withReturnType(C_BASE_COMPONENT, false, Assignability.TYPE_TO_TARGET)
      .withStatic(true)
      .required();

    M_CRAFT_ITEM_STACK__AS_NMS_COPY = C_CRAFT_ITEM_STACK.locateMethod()
      .withName("asNMSCopy")
      .withStatic(true)
      .required();

    M_CRAFT_PLAYER__GET_PROFILE = C_CRAFT_PLAYER.locateMethod()
      .withName("getProfile")
      .withReturnType(GameProfile.class)
      .required();

    M_CRAFT_PLAYER__GET_HANDLE = C_CRAFT_PLAYER.locateMethod().withName("getHandle").required();

    F_CRAFT_SERVER__MINECRAFT_SERVER = C_CRAFT_SERVER.locateField()
      .withType(C_MINECRAFT_SERVER, false, Assignability.TYPE_TO_TARGET)
      .required();

    O_MINECRAFT_SERVER = F_CRAFT_SERVER__MINECRAFT_SERVER.get(Bukkit.getServer());
  }

  //=========================================================================//
  //                         Required Implementations                        //
  //=========================================================================//

  /**
   * Create a new zeroed out base packet
   * @param parameter Parameter to create off of
   * @return Zeroed out base packet
   * @throws Exception Internal errors
   */
  protected abstract Object createBasePacket(T parameter) throws Exception;

  /**
   * Personalize a previously created base packet for a specific viewer. If the implementation
   * does not support personalization, throw a {@link UnsupportedOperationException}.
   * @param packet Packet to personalize
   * @param parameter Parameter to personalize with
   * @param viewer Viewer to personalize for
   * @throws Exception Internal errors
   */
  protected abstract void personalizeBasePacket(Object packet, T parameter, ICustomizableViewer viewer) throws Exception;

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get the entity player reference of a bukkit player
   * @param p Target player
   * @return Entity player reference
   */
  protected Object getEntityPlayer(Player p) throws Exception {
    return M_CRAFT_PLAYER__GET_HANDLE.invoke(p);
  }

  /**
   * Create a new empty packet instance of the communicator's
   * managed packet type
   * @return Packet instance ready to use
   */
  protected Object createPacket() throws Exception {
    return helper.createEmptyPacket(packetType);
  }

  /**
   * Transform an internal component into a IChatBaseComponent
   * while also customizing it for a specific viewer
   * @param component Component to transform
   * @param viewer Viewer which is viewing this component, optional
   * @return Transformed component
   * @throws Exception Internal errors
   */
  public Object componentToBaseComponent(IComponent component, @Nullable ICustomizableViewer viewer) throws Exception {
    return M_CHAT_SERIALIZER__FROM_JSON.invoke(null, component.toJson(viewer == null || viewer.cannotRenderHexColors()));
  }

  /**
   * Transform an internal component into it's json string representation
   * @param component Component to transform
   * @param viewer Viewer which is viewing this component, optional
   * @return Component as a string
   */
  public String componentToJsonString(IComponent component, @Nullable ICustomizableViewer viewer) {
    return component.toJson(viewer == null || viewer.cannotRenderHexColors()).toString();
  }
}

