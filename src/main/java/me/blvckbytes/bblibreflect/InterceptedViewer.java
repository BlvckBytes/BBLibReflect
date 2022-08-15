package me.blvckbytes.bblibreflect;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.bblibreflect.handle.MethodHandle;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/15/2022

  A viewer who's actions are intercepted and fields are updated by the PacketInterceptor.

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
@Setter
@Getter
public class InterceptedViewer implements ICustomizableViewer {

  private final Channel channel;
  private final Object networkManager;
  private final ILogger logger;
  private final MethodHandle M_NETWORK_MANAGER__SEND_PACKET;

  private @Nullable UUID uuid;
  private int clientVersion;
  private int currentWindowId;

  public InterceptedViewer(
    Channel channel,
    Object networkManager,
    ILogger logger,
    MethodHandle M_NETWORK_MANAGER__SEND_PACKET
  ) {
    this.M_NETWORK_MANAGER__SEND_PACKET = M_NETWORK_MANAGER__SEND_PACKET;
    this.channel = channel;
    this.networkManager = networkManager;
    this.logger = logger;
  }

  @Override
  public void sendPacket(Object packet, @Nullable Runnable sent) {
    // The player's channel is no longer open, don't invoke
    // the sending method as it would otherwise throw.
    // This check is extremely cheap, as it usually just checks a socket bitmask.
    if (!channel.isOpen()) {
      // Release the packet resource again, if applicable
      if (sent != null)
        sent.run();
      return;
    }

    try {
      M_NETWORK_MANAGER__SEND_PACKET.invoke(
        networkManager, packet,

        // Wrap the plain runnable in a generic future listener
        sent == null ? null : (GenericFutureListener<Future<Void>>) voidFuture -> sent.run()
      );
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  @Override
  public boolean cannotRenderHexColors() {
    // 721: 1.16-pre1, should be the first to support HEX
    return clientVersion < 721;
  }
}
