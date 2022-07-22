package me.blvckbytes.bblibpackets;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/15/2022

  Keeps track of all classes which are resolved at runtime
  through the help of reflection, in order to handle multiple
  versions of minecraft, as well as the big package refactoring.
*/
@Getter
@AllArgsConstructor
public enum ReflClass {
  MINECRAFT_SERVER(
    "net.minecraft.server.MinecraftServer",
    "net.minecraft.server.{v}.MinecraftServer"
  ),
  PACKET(
    "net.minecraft.network.protocol.Packet",
    "net.minecraft.server.{v}.Packet"
  ),
  NETWORK_MANAGER(
    "net.minecraft.network.NetworkManager",
    "net.minecraft.server.{v}.NetworkManager"
  ),
  SERVER_CONNECTION(
    "net.minecraft.server.network.ServerConnection",
    "net.minecraft.server.{v}.ServerConnection"
  ),
  PLAYER_CONNECTION(
    "net.minecraft.server.network.PlayerConnection",
    "net.minecraft.server.{v}.PlayerConnection"
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
  CHAT_MESSAGE_TYPE(
    "net.minecraft.network.chat.ChatMessageType",
    "net.minecraft.server.{v}.ChatMessageType"
  ),
  PACKET_O_SET_SLOT(
    "net.minecraft.network.protocol.game.PacketPlayOutSetSlot",
    "net.minecraft.server.{v}.PacketPlayOutSetSlot"
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
  CONTAINER(
    "net.minecraft.world.inventory.Container",
    "net.minecraft.server.{v}.Container"
  )
  ;

  private final String afterRefactor, beforeRefactor;

  public Class<?> resolve(boolean afterRefactor, String version) throws ClassNotFoundException {
    return Class.forName((afterRefactor ? this.afterRefactor : this.beforeRefactor).replace("{v}", version));
  }
}
