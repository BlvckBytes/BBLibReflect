package me.blvckbytes.bblibreflect.communicator;

import lombok.AllArgsConstructor;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibdi.IAutoConstructed;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.parameter.BookEditParameter;
import me.blvckbytes.bblibreflect.communicator.parameter.SetSlotParameter;
import me.blvckbytes.bblibreflect.handle.AFieldHandle;
import me.blvckbytes.bblibreflect.handle.AMethodHandle;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/14/2022

  Communicates and manages adding a fake book item to the player's inventory which
  can be edited and fires the completion callback as soon as that book has been signed.

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
public class BookEditCommunicator extends APacketCommunicator<BookEditParameter> implements IPacketModifier, Listener, IAutoConstructed {

  private final APlugin plugin;
  private final SetSlotCommunicator setSlot;
  private final Map<Player, BookEditRequest> requests;

  private final @Nullable AFieldHandle F_PI_BE_LINES, F_PI_BE_ITEM;
  private final Class<?> C_PI_SCS, C_PI_BE;
  private final AMethodHandle M_CIS__AS_BUKKIT_COPY;

  public BookEditCommunicator(
    @AutoInject ILogger logger,
    @AutoInject APlugin plugin,
    @AutoInject IReflectionHelper helper,
    @AutoInject SetSlotCommunicator setSlot,
    @AutoInject IPacketInterceptor interceptor
  ) throws Exception {
    super(logger, helper);

    Class<?> C_CIS  = requireClass(RClass.CRAFT_ITEM_STACK);
    Class<?> C_IS   = requireClass(RClass.ITEM_STACK);

    C_PI_SCS = requireClass(RClass.PACKET_I_SET_CREATIVE_SLOT);
    C_PI_BE  = requireClass(RClass.PACKET_I_B_EDIT);

    M_CIS__AS_BUKKIT_COPY = requireNamedMethod(C_CIS, "asBukkitCopy", false);

    F_PI_BE_LINES = optionalCollectionField(C_PI_BE, List.class, String.class, 0, false, false, null);
    F_PI_BE_ITEM  = optionalCollectionField(C_PI_BE, List.class, C_IS, 0, false, false, null);

    if (F_PI_BE_LINES == null && F_PI_BE_ITEM == null)
      throw new IllegalStateException("Need one of the two fields.");

    this.plugin = plugin;
    this.setSlot = setSlot;
    this.requests = Collections.synchronizedMap(new HashMap<>());

    interceptor.register(this, ModificationPriority.HIGH);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//


  @Override
  public void sendParameterized(IPacketReceiver receiver, BookEditParameter parameter) {
    Player p = parameter.getPlayer();

    // Cancel any previous requests
    undoFakeHand(p, false);

    // Create a new book to set at the player's selected slot
    ItemStack book = new ItemStack(Material.WRITABLE_BOOK, 1);

    // Apply all pages
    BookMeta bookMeta = (BookMeta) book.getItemMeta();
    if (bookMeta != null) {
      for (String page : parameter.getPages())
        bookMeta.addPage(page);
      bookMeta.setAuthor(p.getName());
      book.setItemMeta(bookMeta);
    }

    plugin.runTask(() -> {
      // Set the book as a fake slot item
      int slot = p.getInventory().getHeldItemSlot();
      setSlot.sendParameterized(receiver, new SetSlotParameter(book, (slot + 36) % 36, false));

      // Register the request
      this.requests.put(p, new BookEditRequest(receiver, parameter, book, slot));

      // Set the cancel hook, if applicable
      if (parameter.getCancelHook() != null) {
        parameter.getCancelHook().accept(() -> {
          requests.remove(p);
          undoFakeHand(p, false);
        });
      }
    });
  }

  @Override
  public void cleanup() {
    // Cancel all open book requests on unload
    for (Player p : this.requests.keySet())
      undoFakeHand(p, true);
  }

  @Override
  public void initialize() {}

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();

    // No active request
    if (!requests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to leave the server
    undoFakeHand(p, true);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();

    // Not a fake book request
    BookEditRequest req = requests.get(p);
    if (req == null)
      return;

    // Cancel the event for the server so nothing actually happens
    e.setCancelled(true);

    // Re-set the slot back to the fake item after the game loop ticked
    // as the client will now have noticed and changed it back
    plugin.runTask(() -> {
      setSlot.sendParameterized(req.receiver, new SetSlotParameter(req.fakeItem, (req.slot + 36) % 36, false));
    });
  }

  @EventHandler
  public void onInvOpen(InventoryOpenEvent e) {
    if (!(e.getPlayer() instanceof Player))
      return;

    Player p = (Player) e.getPlayer();

    // No active request
    if (!requests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and open the inventory
    undoFakeHand(p, true);
  }

  @EventHandler
  public void onInvClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player))
      return;

    Player p = (Player) e.getWhoClicked();

    // No active request
    if (!requests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and clicks in their inventory
    e.setCancelled(true);
    undoFakeHand(p, true);
  }

  @EventHandler
  public void onHotbarSelect(PlayerItemHeldEvent e) {
    Player p = e.getPlayer();

    // No active request
    if (!requests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and selects another slot
    e.setCancelled(true);
    undoFakeHand(p, true);
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent e) {
    Player p = e.getPlayer();

    // No active request
    if (!requests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and drop the book
    e.setCancelled(true);
    undoFakeHand(p, true);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Object modifyIncoming(IPacketReceiver receiver, Object incoming) {
    UUID u = receiver.getUuid();

    // Not a player
    if (u == null)
      return incoming;

    // Identify the sending player
    Player p = Bukkit.getPlayer(u);

    // Not a player
    if (p == null)
      return incoming;

    // Player is in creative mode and tried to override a slot
    // This could possibly turn the fake item into a real item, cancel and exit
    if (C_PI_SCS.isInstance(incoming)) {
      // This player has no active request, let the packet through
      if (!requests.containsKey(p))
        return incoming;

      // Cancel this packet and exit
      undoFakeHand(p, true);
      return null;
    }

    // Is not a book edit packet
    if (!(C_PI_BE.isInstance(incoming)))
      return incoming;

    try {
      bookEditReceived(p, extractPages(incoming));
      undoFakeHand(p, false);
    } catch (Exception e) {
      logger.logError(e);
    }

    return incoming;
  }

  @Override
  public Object modifyOutgoing(IPacketReceiver receiver, Object outgoing) {
    return outgoing;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Extract the pages of the book edit packet
   * @param packet Book edit packet
   * @return List of pages
   */
  @SuppressWarnings("unchecked")
  private List<String> extractPages(Object packet) throws Exception {
    // Try to get the list of strings directly (newer versions)
    if (F_PI_BE_LINES != null)
      return (List<String>) F_PI_BE_LINES.get(packet);

    // Cannot be null since LINES is, due to check in constructor
    assert F_PI_BE_ITEM != null;

    // Get the ItemStack otherwise and extract the page contents from that
    Object craftItem = F_PI_BE_ITEM.get(packet);
    ItemStack item = (ItemStack) M_CIS__AS_BUKKIT_COPY.invoke(null, craftItem);
    BookMeta meta = (BookMeta) item.getItemMeta();
    return meta == null ? new ArrayList<>() : meta.getPages();
  }

  /**
   * Called whenever a fake book edit packet has been received
   * @param p Target player
   * @param pages Typed out pages
   */
  private void bookEditReceived(Player p, @Nullable List<String> pages) {
    BookEditRequest request = requests.remove(p);

    // No request!
    if (request == null)
      return;

    // Call the handler in a try-catch block to avoid exceptions from
    // disturbing local control flow
    try {
      // Synchronize this callback
      plugin.runTask(() -> request.parameter.getSubmit().accept(pages));
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Undo a player's fake hand item by re-sending the real hand from server state
   * @param p Target player
   * @param isCancel Whether or not this is a cancel call
   */
  private void undoFakeHand(Player p, boolean isCancel) {
    p.updateInventory();

    // Just has been cancelled
    if (isCancel)
      bookEditReceived(p, null);
  }

  /**
   * Internal state class, wraps further parameters of a book edit request
   */
  @AllArgsConstructor
  private static class BookEditRequest {
    IPacketReceiver receiver;
    BookEditParameter parameter;
    ItemStack fakeItem;
    int slot;
  }
}
