package me.blvckbytes.bblibreflect;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.AllArgsConstructor;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/17/2022

  Relays a channels I/O through modifier lists and drops packets if they
  have been terminated by a modifier.

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
@AllArgsConstructor
public class InterceptedChannelDuplexHandler extends ChannelDuplexHandler {

  private final Object nm;
  private final ILogger logger;
  private final @Nullable UUID u;
  private final List<Tuple<IPacketModifier, ModificationPriority>> globalModifiers;
  private final Map<UUID, ArrayList<Tuple<IPacketModifier, ModificationPriority>>> specificModifiers;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    PacketSource ps = new PacketSource(nm, pack -> {
      try {
        super.channelRead(ctx, pack);
      } catch (Exception e) {
        logger.logError(e);
      }
    });

    // Not a packet, not interested
    if (!(msg.getClass().getName().contains("Packet"))) {
      super.channelRead(ctx, msg);
      return;
    }

    // Ensure exceptions don't ruin the pipe
    try {
      // Run through all global modifiers
      for (Tuple<IPacketModifier, ModificationPriority> modifier : globalModifiers) {
        msg = modifier.getA().modifyIncoming(u, ps, msg);

        // Packet has been terminated
        if (msg == null)
          return;
      }

      // Run through all specific modifiers
      ArrayList<Tuple<IPacketModifier, ModificationPriority>> specifics = specificModifiers.get(u);
      if (specifics != null) {
        for (Tuple<IPacketModifier, ModificationPriority> modifier : specifics) {
          msg = modifier.getA().modifyIncoming(u, ps, msg);

          // Packet has been terminated
          if (msg == null)
            return;
        }
      }
    } catch (Exception e) {
      logger.logError(e);
    }

    // Relay modified packet
    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    // Not a packet, not interested
    if (!(msg.getClass().getName().contains("Packet"))) {
      super.write(ctx, msg, promise);
      return;
    }

    // Ensure exceptions don't ruin the pipe
    try {
      // Run through all global modifiers
      for (Tuple<IPacketModifier, ModificationPriority> modifier : globalModifiers) {
        msg = modifier.getA().modifyOutgoing(u, nm, msg);

        // Packet has been terminated
        if (msg == null)
          return;
      }

      // Run through all specific modifiers
      ArrayList<Tuple<IPacketModifier, ModificationPriority>> specifics = specificModifiers.get(u);
      if (specifics != null) {
        for (Tuple<IPacketModifier, ModificationPriority> modifier : specifics) {
          msg = modifier.getA().modifyOutgoing(u, nm, msg);

          // Packet has been terminated
          if (msg == null)
            return;
        }
      }
    } catch (Exception e) {
      logger.logError(e);
    }

    // Relay modified packet
    super.write(ctx, msg, promise);
  }
}
