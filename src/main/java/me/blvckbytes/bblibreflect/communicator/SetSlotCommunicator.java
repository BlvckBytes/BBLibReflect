package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketReceiver;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.SetSlotParameter;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Communicates setting slots by packets in either the own inventory or
  the currently viewed top inventory.

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
public class SetSlotCommunicator extends APacketCommunicator<SetSlotParameter> {

  private final Field F_PO_SS__WINDOW_ID, F_PO_SS__STATE_ID_OR_SLOT, F_PO_SS__ITEM;
  private final Method M_CIS__AS_NMS_COPY;
  private final @Nullable Field F_POSS__SLOT;
  private final Class<?> C_PO_SS;

  public SetSlotCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper
  ) throws Exception {
    super(logger, helper);

    Class<?> C_CIS  = requireClass(RClass.CRAFT_ITEM_STACK);
    Class<?> C_IS   = requireClass(RClass.ITEM_STACK);

    C_PO_SS = requireClass(RClass.PACKET_O_SET_SLOT);

    F_PO_SS__WINDOW_ID = requireScalarField(C_PO_SS, int.class, 0, false, false, null);
    F_PO_SS__STATE_ID_OR_SLOT = requireScalarField(C_PO_SS, int.class, 1, false, false, null);
    F_POSS__SLOT             = optionalScalarField(C_PO_SS, int.class, 2, false, false, null);
    F_PO_SS__ITEM = requireScalarField(C_PO_SS, C_IS, 0, false, false, null);

    M_CIS__AS_NMS_COPY = requireNamedMethod(C_CIS, "asNMSCopy", false);
  }

  @Override
  public void sendParameterized(IPacketReceiver receiver, SetSlotParameter parameter) {
    if (!(receiver instanceof ICustomizableViewer))
      throw new IllegalStateException("Cannot send a slot change to a non-viewer receiver.");

    this.setSlot(
      receiver,
      parameter.getItem(),
      parameter.getSlot(),

      // A window id of -2 means own inventory
      parameter.isTop() ? ((ICustomizableViewer) receiver).getCurrentWindowId() : -2
    );
  }

  /**
   * Perform a clientside only slot change within any client-viewed inventory
   * @param receiver Target receiver
   * @param item Item to set
   * @param slot Slot ID to change
   * @param windowId Window ID to affect
   */
  private void setSlot(IPacketReceiver receiver, @Nullable ItemStack item, int slot, int windowId) {
    // Invalid slot
    if (slot < 0 || slot > 35)
      return;

    // Create slot setting packet to move this fake book into the inventory
    try {
      Object poss = helper.createEmptyPacket(C_PO_SS);

      F_PO_SS__WINDOW_ID.set(poss, windowId);
      F_PO_SS__STATE_ID_OR_SLOT.set(poss, 0);

      // If there is no third slot field, the state field becomes the slot field
      (F_POSS__SLOT == null ? F_PO_SS__STATE_ID_OR_SLOT : F_POSS__SLOT).set(poss, slot);

      // Empty slots are encoded as AIR
      if (item == null)
        item = new ItemStack(Material.AIR);

      // Set the item as an NMS copy
      Object craftStack = M_CIS__AS_NMS_COPY.invoke(null, item);
      F_PO_SS__ITEM.set(poss, craftStack);

      receiver.sendPackets(poss);
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
