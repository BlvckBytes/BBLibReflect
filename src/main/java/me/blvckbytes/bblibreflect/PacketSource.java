package me.blvckbytes.bblibreflect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Represents the source of a packet, which includes the network manager
  instance as well as a way to send packets programmatically.
*/
@Getter
@AllArgsConstructor
public class PacketSource {
  private Object manager;
  private Consumer<Object> send;
}
