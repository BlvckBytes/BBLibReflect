package me.blvckbytes.bblibreflect;

import me.blvckbytes.bblibreflect.communicator.*;
import me.blvckbytes.bblibreflect.communicator.parameter.ICommunicatorParameter;
import me.blvckbytes.bblibreflect.handle.ClassHandle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 08/20/2022

  The central communicator registry, which looks just like the combination of an
  incoming as well as an outgoing packet communicator from the outside but routes
  requests to the managing communicators under the hood. This way of implementing
  it is just done for convenience sake and to not have to request communicators
  separately.

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
public class PacketCommunicatorRegistry implements IPacketCommunicatorRegistry {

  // Mapping communicator parameter types to their managing communicator
  private final Map<Class<?>, IPacketInCommunicator<ICommunicatorParameter>> inCommunicators;
  private final Map<Class<?>, IPacketOutCommunicator<ICommunicatorParameter>> outCommunicators;

  // Mapping communicator packet types to their managing communicator
  private final Map<ClassHandle, IPacketInCommunicator<ICommunicatorParameter>> inParsers;
  private final Map<ClassHandle, IPacketOutCommunicator<ICommunicatorParameter>> outParsers;

  public PacketCommunicatorRegistry() {
    this.inCommunicators = new HashMap<>();
    this.outCommunicators = new HashMap<>();
    this.inParsers = new HashMap<>();
    this.outParsers = new HashMap<>();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerCommunicator(IPacketCommunicator<? extends ICommunicatorParameter> communicator) {
    if (communicator instanceof IPacketInCommunicator<?>) {
      inCommunicators.put(communicator.getParameterType(), (IPacketInCommunicator<ICommunicatorParameter>) communicator);
      inParsers.put(communicator.getPacketType(), (IPacketInCommunicator<ICommunicatorParameter>) communicator);
      return;
    }

    if (communicator instanceof IPacketOutCommunicator<?>) {
      outCommunicators.put(communicator.getParameterType(), (IPacketOutCommunicator<ICommunicatorParameter>) communicator);
      outParsers.put(communicator.getPacketType(), (IPacketOutCommunicator<ICommunicatorParameter>) communicator);
      return;
    }

    throw new IllegalArgumentException("Unknown packet communicator type provided!");
  }

  @Override
  public CommunicatorResult sendToReceiver(ICommunicatorParameter parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.sendToReceiver(parameter, receiver, done);
  }

  @Override
  public CommunicatorResult sendToReceivers(ICommunicatorParameter parameter, Collection<? extends IPacketReceiver> receivers, @Nullable Runnable done) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.sendToReceivers(parameter, receivers, done);
  }

  @Override
  public CommunicatorResult sendToViewer(ICommunicatorParameter parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.sendToViewer(parameter, viewer, done);
  }

  @Override
  public CommunicatorResult sendToViewers(ICommunicatorParameter parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.sendToViewers(parameter, viewers, done);
  }

  @Override
  public CommunicatorResult sendToPlayer(ICommunicatorParameter parameter, Player player, @Nullable Runnable done) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.sendToPlayer(parameter, player, done);
  }

  @Override
  public CommunicatorResult sendToPlayers(ICommunicatorParameter parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.sendToPlayers(parameter, players, done);
  }

  @Override
  public CommunicatorResult receiveFromReceiver(ICommunicatorParameter parameter, IPacketReceiver receiver, @Nullable Runnable done) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.receiveFromReceiver(parameter, receiver, done);
  }

  @Override
  public CommunicatorResult receiveFromReceivers(ICommunicatorParameter parameter, Collection<? extends IPacketReceiver> receivers, @Nullable Runnable done) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.receiveFromReceivers(parameter, receivers, done);
  }

  @Override
  public CommunicatorResult receiveFromViewer(ICommunicatorParameter parameter, ICustomizableViewer viewer, @Nullable Runnable done) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.receiveFromViewer(parameter, viewer, done);
  }

  @Override
  public CommunicatorResult receiveFromViewers(ICommunicatorParameter parameter, Collection<? extends ICustomizableViewer> viewers, @Nullable Runnable done) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.receiveFromViewers(parameter, viewers, done);
  }

  @Override
  public CommunicatorResult receiveFromPlayer(ICommunicatorParameter parameter, Player player, @Nullable Runnable done) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.receiveFromPlayer(parameter, player, done);
  }

  @Override
  public CommunicatorResult receiveFromPlayers(ICommunicatorParameter parameter, Collection<? extends Player> players, @Nullable Runnable done) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inCommunicators.get(parameter.getClass());

    if (communicator == null)
      return CommunicatorResult.UNKNOWN_PARAMETER_TYPE;

    return communicator.receiveFromPlayers(parameter, players, done);
  }

  @Override
  public @Nullable ICommunicatorParameter parseIncoming(Object packet) {
    IPacketInCommunicator<ICommunicatorParameter> communicator = inParsers.get(ClassHandle.of(packet.getClass()));

    if (communicator == null)
      return null;

    return communicator.parseIncoming(packet);
  }

  @Override
  public @Nullable ICommunicatorParameter parseOutgoing(Object packet) {
    IPacketOutCommunicator<ICommunicatorParameter> communicator = outParsers.get(ClassHandle.of(packet.getClass()));

    if (communicator == null)
      return null;

    return communicator.parseOutgoing(packet);
  }

  @Override
  public Class<ICommunicatorParameter> getParameterType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClassHandle getPacketType() {
    throw new UnsupportedOperationException();
  }
}
