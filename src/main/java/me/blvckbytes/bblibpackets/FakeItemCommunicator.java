package me.blvckbytes.bblibpackets;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Creates all packets in regard to setting fake items in the player's inventory.
*/
@AutoConstruct
public class FakeItemCommunicator implements IFakeItemCommunicator {

  private final Function<Player, Integer> widAccessor;
  private final MCReflect refl;
  private final ILogger logger;

  public FakeItemCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
    ) {
    this.refl = refl;
    this.logger = logger;
    this.widAccessor = refl.getWidAccess();
  }

  @Override
  public boolean setFakeTopInventorySlot(Player p, @Nullable ItemStack is, int slot) {
    int openWid = this.widAccessor.apply(p);

    // Currently not viewing any inventories
    if (openWid == 0)
      return false;

    return setFakeSlot(p, is, slot, openWid);
  }

  @Override
  public boolean setFakeInventorySlot(Player p, @Nullable ItemStack is, int slot) {
    // Window id of -2 means inv
    return setFakeSlot(p, is, slot, -2);
  }

  /**
   * Perform a clientside only slot change within any player-viewed inventory
   * @param p Target player
   * @param is ItemStack to set
   * @param slot Slot ID to change
   * @param wid Window ID to affect
   * @return Success state
   */
  private boolean setFakeSlot(Player p, @Nullable ItemStack is, int slot, Integer wid) {
    // Invalid slot
    if (slot < 0 || slot > 35)
      return false;

    // Create slot setting packet to move this fake book into the inventory
    try {
      Object poss = refl.createPacket(refl.getReflClass(ReflClass.PACKET_O_SET_SLOT));

      refl.setFieldByType(poss, int.class, wid, 0); // Window ID
      refl.setFieldByType(poss, int.class, 0, 1); // State ID (leave at zero for now)
      if (!refl.setFieldByType(poss, int.class, slot, 2)) // Slot (set at 1 if there are only two fields)
        refl.setFieldByType(poss, int.class, slot, 1);

      if (is == null)
        is = new ItemStack(Material.AIR);

      // Convert the bukkit item stack to a craft item stack and set the corresponding field
      Class<?> cisC = refl.getClassBKT("inventory.CraftItemStack");
      Object cis = refl.findMethodByName(cisC, "asNMSCopy", ItemStack.class).invoke(null, is);
      refl.setFieldByType(poss, refl.getReflClass(ReflClass.ITEM_STACK), cis, 0);

      return refl.sendPacket(p, poss);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }
}
