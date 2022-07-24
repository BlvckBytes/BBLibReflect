package me.blvckbytes.bblibpackets;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/17/2022

  Wraps all parameters a book edit request requires to be processed.
*/
@Getter
@AllArgsConstructor
public class BookEditRequest {
  private ItemStack fakeItem;
  private int fakeSlot;
  private Consumer<@Nullable List<String>> callback;
}
