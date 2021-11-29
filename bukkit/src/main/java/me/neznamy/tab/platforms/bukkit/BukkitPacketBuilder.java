package me.neznamy.tab.platforms.bukkit;

import me.neznamy.tab.api.ProtocolVersion;
import me.neznamy.tab.api.chat.IChatBaseComponent;
import me.neznamy.tab.api.protocol.PacketBuilder;
import me.neznamy.tab.api.protocol.PacketPlayOutBoss;
import me.neznamy.tab.api.protocol.PacketPlayOutBoss.Action;
import me.neznamy.tab.api.protocol.PacketPlayOutChat;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerListHeaderFooter;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardDisplayObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardScore;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardTeam;
import me.neznamy.tab.platforms.bukkit.nms.AdapterProvider;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcher;
import me.neznamy.tab.platforms.bukkit.nms.packet.PacketPlayOutEntityDestroy;
import me.neznamy.tab.platforms.bukkit.nms.packet.PacketPlayOutEntityMetadata;
import me.neznamy.tab.platforms.bukkit.nms.packet.PacketPlayOutEntityTeleport;
import me.neznamy.tab.platforms.bukkit.nms.packet.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class BukkitPacketBuilder extends PacketBuilder {

	/**
	 * Constructs new instance
	 */
	public BukkitPacketBuilder() {
		buildMap.put(PacketPlayOutEntityMetadata.class, (packet, version) -> build((PacketPlayOutEntityMetadata)packet));
		buildMap.put(PacketPlayOutEntityTeleport.class, (packet, version) -> build((PacketPlayOutEntityTeleport)packet));
		buildMap.put(PacketPlayOutEntityDestroy.class, (packet, version) -> build((PacketPlayOutEntityDestroy)packet));
		buildMap.put(PacketPlayOutSpawnEntityLiving.class, (packet, version) -> build((PacketPlayOutSpawnEntityLiving)packet));
	}

	@Override
	public Object build(PacketPlayOutBoss packet, ProtocolVersion clientVersion) throws IllegalAccessException {
		if (AdapterProvider.getMinorVersion() >= 9 || clientVersion.getMinorVersion() >= 9) {
			//1.9+ server or client, handled by bukkit api or ViaVersion
			return packet;
		}
		//<1.9 client and server
		return buildBossPacketEntity(packet, clientVersion);
	}

	@Override
	public Object build(PacketPlayOutChat packet, ProtocolVersion clientVersion) {
		return AdapterProvider.get().createChatPacket(
				AdapterProvider.get().adaptComponent(packet.getMessage(), clientVersion),
				packet.getType()
		);
	}

	@Override
	public Object build(PacketPlayOutPlayerInfo packet, ProtocolVersion clientVersion) throws ReflectiveOperationException {
		return AdapterProvider.get().createPlayerInfoPacket(clientVersion, packet.getAction(), packet.getEntries());
	}

	@Override
	public Object build(PacketPlayOutPlayerListHeaderFooter packet, ProtocolVersion clientVersion) {
		return AdapterProvider.get().createPlayerListHeaderFooterPacket(clientVersion, packet.getHeader(), packet.getFooter());
	}

	@Override
	public Object build(PacketPlayOutScoreboardDisplayObjective packet, ProtocolVersion clientVersion) {
		return AdapterProvider.get().createDisplayObjectivePacket(packet.getSlot(), packet.getObjectiveName());
	}

	@Override
	public Object build(PacketPlayOutScoreboardObjective packet, ProtocolVersion clientVersion) {
		String displayName = clientVersion.getMinorVersion() < 13 ? cutTo(packet.getDisplayName(), 32) : packet.getDisplayName();
		return AdapterProvider.get().createObjectivePacket(
				packet.getMethod(),
				packet.getObjectiveName(),
				AdapterProvider.get().adaptComponent(IChatBaseComponent.optimizedComponent(displayName), clientVersion),
				packet.getRenderType()
		);
	}

	@Override
	public Object build(PacketPlayOutScoreboardScore packet, ProtocolVersion clientVersion) {
		return AdapterProvider.get().createScorePacket(packet.getAction(), packet.getObjectiveName(), packet.getPlayer(), packet.getScore());
	}

	@Override
	public Object build(PacketPlayOutScoreboardTeam packet, ProtocolVersion clientVersion) {
		String prefix = packet.getPlayerPrefix();
		String suffix = packet.getPlayerSuffix();
		if (clientVersion.getMinorVersion() < 13) {
			prefix = cutTo(prefix, 16);
			suffix = cutTo(suffix, 16);
		}
		return AdapterProvider.get().createTeamPacket(
				clientVersion,
				packet.getName(),
				prefix,
				suffix,
				packet.getNametagVisibility(),
				packet.getCollisionRule(),
				packet.getColor(),
				packet.getPlayers(),
				packet.getMethod(),
				packet.getOptions()
		);
	}

	public Object build(PacketPlayOutEntityDestroy packet) {
		return AdapterProvider.get().createEntityDestroyPacket(packet.getEntities());
	}

	public Object build(PacketPlayOutEntityMetadata packet) {
		return AdapterProvider.get().createMetadataPacket(packet.getEntityId(), packet.getDataWatcher());
	}

	public Object build(PacketPlayOutSpawnEntityLiving packet) throws IllegalAccessException {
		return AdapterProvider.get().createSpawnLivingEntityPacket(
				packet.getEntityId(),
				packet.getUniqueId(),
				packet.getEntityType(),
				packet.getLocation(),
				packet.getDataWatcher()
		);
	}

	public Object build(PacketPlayOutEntityTeleport packet) throws IllegalAccessException {
		return AdapterProvider.get().createTeleportPacket(packet.getEntityId(), packet.getLocation());
	}
	
	@Override
	public PacketPlayOutPlayerInfo readPlayerInfo(Object nmsPacket, ProtocolVersion clientVersion) throws IllegalAccessException {
		return AdapterProvider.get().createPlayerInfoPacket(nmsPacket);
	}

	@Override
	public PacketPlayOutScoreboardObjective readObjective(Object nmsPacket, ProtocolVersion clientVersion) throws IllegalAccessException {
		return AdapterProvider.get().createObjectivePacket(nmsPacket);
	}

	@Override
	public PacketPlayOutScoreboardDisplayObjective readDisplayObjective(Object nmsPacket, ProtocolVersion clientVersion) throws IllegalAccessException {
		return AdapterProvider.get().createDisplayObjectivePacket(nmsPacket);
	}

	private Object buildBossPacketEntity(PacketPlayOutBoss packet, ProtocolVersion clientVersion) throws IllegalAccessException {
		if (packet.getOperation() == Action.UPDATE_STYLE) return null; //nothing to do here

		int entityId = packet.getId().hashCode();
		if (packet.getOperation() == Action.REMOVE) {
			return build(new PacketPlayOutEntityDestroy(entityId));
		}
		DataWatcher w = new DataWatcher();
		if (packet.getOperation() == Action.UPDATE_PCT || packet.getOperation() == Action.ADD) {
			float health = 300*packet.getPct();
			if (health == 0) health = 1;
			w.helper().setHealth(health);
		}
		if (packet.getOperation() == Action.UPDATE_NAME || packet.getOperation() == Action.ADD) {
			w.helper().setCustomName(packet.getName(), clientVersion);
		}
		if (packet.getOperation() == Action.ADD) {
			w.helper().setEntityFlags((byte) 32);
			return build(new PacketPlayOutSpawnEntityLiving(entityId, null, EntityType.WITHER, new Location(null, 0,0,0), w));
		} else {
			return build(new PacketPlayOutEntityMetadata(entityId, w));
		}
	}
}
