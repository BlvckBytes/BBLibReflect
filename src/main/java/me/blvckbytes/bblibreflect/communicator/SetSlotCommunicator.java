package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.ICustomizableViewer;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibreflect.RClass;
import me.blvckbytes.bblibreflect.communicator.parameter.SetSlotParameter;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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

  private final FieldHandle F_PO_SS__WINDOW_ID, F_PO_SS__STATE_ID_OR_SLOT, F_PO_SS__ITEM;
  private final @Nullable FieldHandle F_POSS__SLOT;

  public SetSlotCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor, true, helper.getClass(RClass.PACKET_O_SET_SLOT));

    F_PO_SS__WINDOW_ID        = getPacketType().locateField().withType(int.class).required();
    F_PO_SS__STATE_ID_OR_SLOT = getPacketType().locateField().withType(int.class).withSkip(1).required();
    F_POSS__SLOT              = getPacketType().locateField().withType(int.class).withSkip(2).optional();
    F_PO_SS__ITEM             = getPacketType().locateField().withType(C_ITEM_STACK).required();
  }

  @Override
  protected Object createBasePacket(SetSlotParameter parameter) throws Exception {
    Object packet = createPacket();

    // FIXME: Is a state of zero really okay?
    F_PO_SS__STATE_ID_OR_SLOT.set(packet, 0);

    // If there is no third slot field, the state field becomes the slot field
    (F_POSS__SLOT == null ? F_PO_SS__STATE_ID_OR_SLOT : F_POSS__SLOT).set(packet, parameter.getSlot());

    // No personalized item, set ahead of time for all viewers
    if (parameter.getPersonalizedItem() == null)
      setItem(packet, parameter.getItem());

    return packet;
  }

  @Override
  protected void personalizeBasePacket(Object packet, SetSlotParameter parameter, ICustomizableViewer viewer) throws Exception {
    // Set personal window ID
    F_PO_SS__WINDOW_ID.set(packet, parameter.isTop() ? viewer.getCurrentWindowId() : -2);

    // Set personalized item, if applicable
    if (parameter.getPersonalizedItem() != null)
      setItem(packet, parameter.getItem());
  }

  @Override
  public Class<SetSlotParameter> getParameterType() {
    return SetSlotParameter.class;
  }

  /**
   * Sets the item field on a packet to a concrete value
   * @param packet Packet to modify
   * @param item Item value to set
   */
  private void setItem(Object packet, @Nullable ItemStack item) throws Exception {
    // Empty slots are encoded as AIR
    if (item == null)
      item = new ItemStack(Material.AIR);

    // Set the item as an NMS copy
    Object craftStack = M_CRAFT_ITEM_STACK__AS_NMS_COPY.invoke(null, item);
    F_PO_SS__ITEM.set(packet, craftStack);
  }

  @Override
  public @Nullable SetSlotParameter parseOutgoing(Object packet) {
    // TODO: Implement
    throw new UnsupportedOperationException();
  }
}
