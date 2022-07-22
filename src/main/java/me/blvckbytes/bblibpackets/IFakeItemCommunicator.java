package me.blvckbytes.bblibpackets;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Communicates setting items in the player's inventory in a fake manner, so
  that the server doesn't handle them and they're only virtual.
*/
public interface IFakeItemCommunicator {

  /**
   * Perform a clientside only slot change within the players viewed top inventory
   * @param p Target player
   * @param is ItemStack to set
   * @param slot Slot ID to change
   * @return Success state
   */
  boolean setFakeTopInventorySlot(Player p, @Nullable ItemStack is, int slot);

  /**
   * Perform a clientside only slot change within the players own inventory
   * @param p Target player
   * @param is ItemStack to set
   * @param slot Slot ID to change
   * @return Success state
   */
  boolean setFakeInventorySlot(Player p, @Nullable ItemStack is, int slot);

}
