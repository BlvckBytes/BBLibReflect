package me.blvckbytes.bblibreflect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/15/2022

  Keeps track of all classes which are resolved at runtime
  through the help of reflection, in order to handle multiple
  versions of minecraft, as well as the big package refactoring.

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
@AllArgsConstructor
public enum RClass {
  MINECRAFT_SERVER(
    "net.minecraft.server.MinecraftServer",
    "net.minecraft.server.{v}.MinecraftServer"
  ),
  PACKET(
    "net.minecraft.network.protocol.Packet",
    "net.minecraft.server.{v}.Packet"
  ),
  I_CHAT_BASE_COMPONENT(
    "net.minecraft.network.chat.IChatBaseComponent",
    "net.minecraft.server.{v}.NetworkManager"
  ),
  CHAT_SERIALIZER(
    "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer",
    "net.minecraft.server.{v}.IChatBaseComponent$ChatSerializer"
  ),
  CHAT_MESSAGE_TYPE(
    "net.minecraft.network.chat.ChatMessageType",
    "net.minecraft.server.{v}.ChatMessageType"
  ),
  FILTERED_TEXT(
    "net.minecraft.server.network.FilteredText",
    ""
  ),
  MESSAGE_SIGNATURE(
    "net.minecraft.network.chat.MessageSignature",
    ""
  ),
  PLAYER_CHAT_MESSAGE(
    "net.minecraft.network.chat.PlayerChatMessage",
    ""
  ),
  NETWORK_MANAGER(
    "net.minecraft.network.NetworkManager",
    "net.minecraft.server.{v}.NetworkManager"
  ),
  QUEUED_PACKET(
    "net.minecraft.network.NetworkManager$QueuedPacket",
    "net.minecraft.server.{v}.NetworkManager$QueuedPacket"
  ),
  SERVER_CONNECTION(
    "net.minecraft.server.network.ServerConnection",
    "net.minecraft.server.{v}.ServerConnection"
  ),
  PLAYER_CONNECTION(
    "net.minecraft.server.network.PlayerConnection",
    "net.minecraft.server.{v}.PlayerConnection"
  ),
  PLAYER_LIST(
    "net.minecraft.server.players.PlayerList",
    "net.minecraft.server.{v}.PlayerList"
  ),
  RESOURCE_KEY(
    "net.minecraft.resources.ResourceKey",
    ""
  ),
  PACKET_DATA_SERIALIZER(
    "net.minecraft.network.PacketDataSerializer",
    "net.minecraft.server.{v}.PacketDataSerializer"
  ),
  ITEM(
    "net.minecraft.world.item.Item",
    "net.minecraft.server.{v}.Item"
  ),
  ITEM_STACK(
    "net.minecraft.world.item.ItemStack",
    "net.minecraft.server.{v}.ItemStack"
  ),
  GENERIC_ATTRIBUTES(
    "net.minecraft.world.entity.ai.attributes.GenericAttributes",
    "net.minecraft.server.{v}.GenericAttributes"
  ),
  ATTRIBUTE_BASE(
    "net.minecraft.world.entity.ai.attributes.AttributeBase",
    "net.minecraft.server.{v}.AttributeBase"
  ),
  CHAT_COMPONENT_TEXT(
    "net.minecraft.network.chat.ChatComponentText",
    "net.minecraft.server.{v}.ChatComponentText"
  ),
  PACKET_O_OPEN_WINDOW(
    "net.minecraft.network.protocol.game.PacketPlayOutOpenWindow",
    "net.minecraft.server.{v}.PacketPlayOutOpenWindow"
  ),
  PACKET_O_SET_SLOT(
    "net.minecraft.network.protocol.game.PacketPlayOutSetSlot",
    "net.minecraft.server.{v}.PacketPlayOutSetSlot"
  ),
  PACKET_O_PLAYER_INFO(
    "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo",
    "net.minecraft.server.{v}.PacketPlayOutPlayerInfo"
  ),
  ENUM_PLAYER_INFO_ACTION(
    "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo$EnumPlayerInfoAction",
    "net.minecraft.server.{v}.PacketPlayOutPlayerInfo$EnumPlayerInfoAction"
  ),
  PLAYER_INFO_DATA(
    "net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo$PlayerInfoData",
    "net.minecraft.server.{v}.PacketPlayOutPlayerInfo$PlayerInfoData"
  ),
  PACKET_O_CHAT(
    "net.minecraft.network.protocol.game.PacketPlayOutChat",
    "net.minecraft.server.{v}.PacketPlayOutChat"
  ),
  PACKET_O_WINDOW_DATA(
    "net.minecraft.network.protocol.game.PacketPlayOutWindowData",
    "net.minecraft.server.{v}.PacketPlayOutWindowData"
  ),
  PACKET_O_TITLE(
    "",
    "net.minecraft.server.{v}.PacketPlayOutTitle"
  ),
  PACKET_O_LOGIN(
    "net.minecraft.network.protocol.login.PacketLoginOutSuccess",
    "net.minecraft.server.{v}.PacketLoginOutSuccess"
  ),
  PACKET_O_KEEP_ALIVE(
    "net.minecraft.network.protocol.game.PacketPlayOutKeepAlive",
    "net.minecraft.server.{v}.PacketPlayOutKeepAlive"
  ),
  ENUM_TITLE_ACTION(
    "",
    "net.minecraft.server.{v}.PacketPlayOutTitle$EnumTitleAction"
  ),
  CLIENTBOUND_TITLES_ANIMATION(
    "net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket",
    ""
  ),
  CLIENTBOUND_TITLE_SET(
    "net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket",
    ""
  ),
  CLIENTBOUND_SUBTITLE_SET(
    "net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket",
    ""
  ),
  CLIENTBOUND_SYSTEM_CHAT_PACKET(
    "net.minecraft.network.protocol.game.ClientboundSystemChatPacket",
    ""
  ),
  PACKET_I_B_EDIT(
    "net.minecraft.network.protocol.game.PacketPlayInBEdit",
    "net.minecraft.server.{v}.PacketPlayInBEdit"
  ),
  PACKET_I_SET_CREATIVE_SLOT(
    "net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot",
    "net.minecraft.server.{v}.PacketPlayInSetCreativeSlot"
  ),
  PACKET_I_ITEM_NAME(
    "net.minecraft.network.protocol.game.PacketPlayInItemName",
    "net.minecraft.server.{v}.PacketPlayInItemName"
  ),
  PACKET_I_HANDSHAKE(
    "net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol",
    "net.minecraft.server.{v}.PacketHandshakingInSetProtocol"
  ),
  PACKET_I_KEEP_ALIVE(
    "net.minecraft.network.protocol.game.PacketPlayInKeepAlive",
    "net.minecraft.server.{v}.PacketPlayInKeepAlive"
  ),
  PACKET_I_CHAT(
    "net.minecraft.network.protocol.game.PacketPlayInChat",
    "net.minecraft.server.{v}.PacketPlayInChat"
  ),
  TILE_ENTITY_FURNACE(
    "net.minecraft.world.level.block.entity.TileEntityFurnace",
    "net.minecraft.server.{v}.TileEntityFurnace"
  ),
  NBT_TAG_LIST(
    "net.minecraft.nbt.NBTTagList",
    "net.minecraft.server.{v}.NBTTagList"
  ),
  NBT_TAG_COMPOUND(
    "net.minecraft.nbt.NBTTagCompound",
    "net.minecraft.server.{v}.NBTTagCompound"
  ),
  NBT_TAG_STRING(
    "net.minecraft.nbt.NBTTagString",
    "net.minecraft.server.{v}.NBTTagString"
  ),
  NBT_TAG_INT(
    "net.minecraft.nbt.NBTTagInt",
    "net.minecraft.server.{v}.NBTTagInt"
  ),
  NBT_TAG_LONG(
    "net.minecraft.nbt.NBTTagLong",
    "net.minecraft.server.{v}.NBTTagLong"
  ),
  NBT_TAG_INT_ARRAY(
    "net.minecraft.nbt.NBTTagIntArray",
    "net.minecraft.server.{v}.NBTTagIntArray"
  ),
  NBT_TAG_DOUBLE(
    "net.minecraft.nbt.NBTTagDouble",
    "net.minecraft.server.{v}.NBTTagDouble"
  ),
  NBT_BASE(
    "net.minecraft.nbt.NBTBase",
    "net.minecraft.server.{v}.NBTBase"
  ),
  ENTITY_HUMAN(
    "net.minecraft.world.entity.player.EntityHuman",
    "net.minecraft.server.{v}.EntityHuman"
  ),
  ENTITY_PLAYER(
    "net.minecraft.server.level.EntityPlayer",
    "net.minecraft.server.{v}.EntityPlayer"
  ),
  CONTAINER(
    "net.minecraft.world.inventory.Container",
    "net.minecraft.server.{v}.Container"
  ),
  CRAFT_ITEM_STACK(
    "org.bukkit.craftbukkit.{v}.inventory.CraftItemStack",
    "org.bukkit.craftbukkit.{v}.inventory.CraftItemStack"
  ),
  CRAFT_SERVER(
    "org.bukkit.craftbukkit.{v}.CraftServer",
    "org.bukkit.craftbukkit.{v}.CraftServer"
  ),
  CRAFT_META_ITEM(
    "org.bukkit.craftbukkit.{v}.inventory.CraftMetaItem",
    "org.bukkit.craftbukkit.{v}.inventory.CraftMetaItem"
  ),
  CRAFT_PLAYER(
    "org.bukkit.craftbukkit.{v}.entity.CraftPlayer",
    "org.bukkit.craftbukkit.{v}.entity.CraftPlayer"
  ),
  ENUM_GAME_MODE(
    "net.minecraft.world.level.EnumGamemode",
    "net.minecraft.server.{v}.EnumGamemode"
  ),
  PROFILE_PUBLIC_KEY(
    "net.minecraft.world.entity.player.ProfilePublicKey",
    ""
  )
  ;

  private final String afterRefactor, beforeRefactor;
  private static final Map<RClass, ClassHandle> cache;

  static {
    cache = new HashMap<>();
  }

  public ClassHandle resolve(boolean afterRefactor, String version) throws ClassNotFoundException {
    ClassHandle res = cache.get(this);

    // Respond with cache result
    if (res != null)
      return res;

    // Load class and then cache
    res = ClassHandle.of(
      Class.forName((afterRefactor ? this.afterRefactor : this.beforeRefactor).replace("{v}", version))
    );

    cache.put(this, res);
    return res;
  }
}
