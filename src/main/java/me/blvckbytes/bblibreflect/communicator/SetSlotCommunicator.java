package me.blvckbytes.bblibreflect.communicator;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.SetSlotParameter;
import me.blvckbytes.bblibreflect.handle.FieldHandle;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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
  private final MethodHandle M_CIS__AS_NMS_COPY;
  private final @Nullable FieldHandle F_POSS__SLOT;
  private final ClassHandle C_PO_SS;

  public SetSlotCommunicator(
    @AutoInject ILogger logger,
    @AutoInject IReflectionHelper helper,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper, interceptor);

    ClassHandle C_CIS  = helper.getClass(RClass.CRAFT_ITEM_STACK);
    ClassHandle C_IS   = helper.getClass(RClass.ITEM_STACK);

    C_PO_SS = helper.getClass(RClass.PACKET_O_SET_SLOT);

    F_PO_SS__WINDOW_ID        = C_PO_SS.locateField().withType(int.class).required();
    F_PO_SS__STATE_ID_OR_SLOT = C_PO_SS.locateField().withType(int.class).withSkip(1).required();
    F_POSS__SLOT              = C_PO_SS.locateField().withType(int.class).withSkip(2).optional();
    F_PO_SS__ITEM             = C_PO_SS.locateField().withType(C_IS).required();

    M_CIS__AS_NMS_COPY = C_CIS.locateMethod().withName("asNMSCopy").withStatic(true).required();
  }

  @Override
  public CommunicatorResult sendToViewer(SetSlotParameter parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    try {
      Object packet = createPacket(parameter);
      F_PO_SS__WINDOW_ID.set(packet, parameter.isTop() ? viewer.getCurrentWindowId() : -2);
      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public CommunicatorResult sendToViewers(SetSlotParameter parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    try {
      Object packet = createPacket(parameter);

      sendPacketsToReceivers(viewers, done, (viewer, subDone) -> {
        F_PO_SS__WINDOW_ID.set(packet, parameter.isTop() ? viewer.getCurrentWindowId() : -2);
        sendPacketsToReceiver(viewer, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public CommunicatorResult sendToPlayer(SetSlotParameter parameter, Player player, @Nullable Runnable done) {
    return sendToViewer(parameter, interceptor.getPlayerAsViewer(player), done);
  }

  @Override
  public CommunicatorResult sendToPlayers(SetSlotParameter parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    try {
      Object packet = createPacket(parameter);

      sendPacketsToReceivers(players, done, (player, subDone) -> {
        ICustomizableViewer viewer = interceptor.getPlayerAsViewer(player);
        F_PO_SS__WINDOW_ID.set(packet, parameter.isTop() ? viewer.getCurrentWindowId() : -2);
        sendPacketsToReceiver(viewer, subDone, packet);
      });

      return CommunicatorResult.SUCCESS;
    } catch (Exception e) {
      logger.logError(e);
      return CommunicatorResult.REFLECTION_ERROR;
    }
  }

  @Override
  public Class<SetSlotParameter> getParameterType() {
    return SetSlotParameter.class;
  }

  /**
   * Create the packet base with the slot as well as the item set.
   * @param parameter Parameter to extract the information from
   * @return Constructed packet
   */
  private Object createPacket(SetSlotParameter parameter) throws Exception {
    // Create slot setting packet to move this fake book into the inventory
    Object poss = helper.createEmptyPacket(C_PO_SS);

    F_PO_SS__STATE_ID_OR_SLOT.set(poss, 0);

    // If there is no third slot field, the state field becomes the slot field
    (F_POSS__SLOT == null ? F_PO_SS__STATE_ID_OR_SLOT : F_POSS__SLOT).set(poss, parameter.getSlot());

    // Empty slots are encoded as AIR
    ItemStack item = parameter.getItem();
    if (item == null)
      item = new ItemStack(Material.AIR);

    // Set the item as an NMS copy
    Object craftStack = M_CIS__AS_NMS_COPY.invoke(null, item);
    F_PO_SS__ITEM.set(poss, craftStack);

    return poss;
  }
}
