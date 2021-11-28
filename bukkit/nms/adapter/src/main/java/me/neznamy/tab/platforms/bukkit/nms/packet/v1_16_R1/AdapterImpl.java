package me.neznamy.tab.platforms.bukkit.nms.v1_16_R1;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;

import io.netty.channel.Channel;
import me.neznamy.tab.api.ProtocolVersion;
import me.neznamy.tab.api.chat.EnumChatFormat;
import me.neznamy.tab.api.chat.IChatBaseComponent;
import me.neznamy.tab.api.chat.TextColor;
import me.neznamy.tab.api.protocol.PacketPlayOutChat;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardDisplayObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardScore;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcher;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherRegistry;
import me.neznamy.tab.platforms.bukkit.nms.Adapter;
import me.neznamy.tab.shared.TAB;

@SuppressWarnings({"rawtypes", "unchecked"})

public final class AdapterImpl implements Adapter {

	//base
	private final Class<?> Packet = getNMSClass("Packet");
	private final Class<?> EnumChatFormat = getNMSClass("EnumChatFormat");
	private final Class<?> EntityPlayer = getNMSClass("EntityPlayer");
	private final Class<?> Entity = getNMSClass("Entity");
	private final Class<?> EntityLiving = getNMSClass("EntityLiving");
	private final Class<?> NetworkManager = getNMSClass("NetworkManager");
	private final Class<?> PlayerConnection = getNMSClass("PlayerConnection");
	private final Constructor<?> newEntityArmorStand = getNMSClass("EntityArmorStand").getConstructor(getNMSClass("World"), double.class, double.class, double.class);
	private final Field PING = EntityPlayer.getField("ping");
	private final Field PLAYER_CONNECTION = EntityPlayer.getField("playerConnection");
	private final Field NETWORK_MANAGER = PlayerConnection.getField("networkManager");
	private final Field CHANNEL = NetworkManager.getField("channel");
	private final Method getHandle = Class.forName("org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer").getMethod("getHandle");
	private final Method sendPacket = PlayerConnection.getMethod("sendPacket", Packet);
	private final Method getProfile = getNMSClass("EntityHuman").getMethod("getProfile");
	private final Method World_getHandle = Class.forName("org.bukkit.craftbukkit.v1_16_R1.CraftWorld").getMethod("getHandle");

	private final Enum[] EnumChatFormat_values = getEnumValues(EnumChatFormat);

	//chat
	private final Class<?> ChatBaseComponent = getNMSClass("ChatBaseComponent");
	private final Class<?> ChatComponentText = getNMSClass("ChatComponentText");
	private final Class<?> ChatModifier = getNMSClass("ChatModifier");
	private final Class<?> ChatHoverable = getNMSClass("ChatHoverable");
	private final Class<?> ChatClickable = getNMSClass("ChatClickable");
	private final Class<?> EnumClickAction = getNMSClass("EnumClickAction");
	private final Class<?> EnumHoverAction = getNMSClass("ChatHoverable$EnumHoverAction");
	private final Class<?> IChatBaseComponent = getNMSClass("IChatBaseComponent");
	private final Class<?> ChatHexColor = getNMSClass("ChatHexColor");
	private final Class<?> MinecraftKey = getNMSClass("MinecraftKey");
	private final Constructor<?> newChatComponentText = ChatComponentText.getConstructor(String.class);
	private final Constructor<?> newChatClickable = ChatClickable.getConstructor(EnumClickAction, String.class);
	private final Constructor<?> newChatModifier = setAccessible(ChatModifier.getDeclaredConstructor(ChatHexColor, Boolean.class, Boolean.class, Boolean.class, 
			Boolean.class, Boolean.class, ChatClickable, ChatHoverable, String.class, MinecraftKey));
	private final Constructor<?> newChatHoverable = ChatHoverable.getConstructor(EnumHoverAction, Object.class);
	private final Field ChatBaseComponent_extra = setAccessible(ChatBaseComponent.getDeclaredField("siblings"));
	private final Field ChatBaseComponent_modifier = setAccessible(ChatBaseComponent.getDeclaredField("d"));
	private final Field ChatComponentText_text = setAccessible(ChatComponentText.getDeclaredField("e"));
	private final Field ChatClickable_action = setAccessible(ChatClickable.getDeclaredField("a"));
	private final Field ChatClickable_value = setAccessible(ChatClickable.getDeclaredField("b"));
	private final Field ChatModifier_color = setAccessible(ChatModifier.getDeclaredField("color"));
	private final Field ChatModifier_bold = setAccessible(ChatModifier.getDeclaredField("bold"));
	private final Field ChatModifier_italic = setAccessible(ChatModifier.getDeclaredField("italic"));
	private final Field ChatModifier_underlined = setAccessible(ChatModifier.getDeclaredField("underlined"));
	private final Field ChatModifier_strikethrough = setAccessible(ChatModifier.getDeclaredField("strikethrough"));
	private final Field ChatModifier_obfuscated = setAccessible(ChatModifier.getDeclaredField("obfuscated"));
	private final Field ChatModifier_clickEvent = setAccessible(ChatModifier.getDeclaredField("clickEvent"));
	private final Field ChatModifier_hoverEvent = setAccessible(ChatModifier.getDeclaredField("hoverEvent"));
	private final Field ChatHexColor_name = setAccessible(ChatHexColor.getDeclaredField("name"));
	private final Field ChatHexColor_rgb = setAccessible(ChatHexColor.getDeclaredField("rgb"));
	private final Method ChatBaseComponent_addSibling = ChatBaseComponent.getMethod("addSibling", IChatBaseComponent);
	private final Method ChatHexColor_ofInt = ChatHexColor.getMethod("a", int.class);
	private final Method ChatHexColor_ofString = ChatHexColor.getMethod("a", String.class);
	private final Method ChatHoverable_getAction = ChatHoverable.getMethod("a");
	private final Method ChatHoverable_getValue = ChatHoverable.getMethod("a", EnumHoverAction);
	private final Method ChatHoverable_serialize = ChatHoverable.getMethod("a", JsonObject.class);
	private final Method EnumHoverAction_getByName = EnumHoverAction.getMethod("a", String.class);
	private final Method EnumHoverAction_fromJson = EnumHoverAction.getMethod("a", JsonElement.class);
	private final Method EnumHoverAction_fromLegacyComponent = EnumHoverAction.getMethod("a", IChatBaseComponent.class);

	//PacketPlayOutChat
	private final Class<?> PacketPlayOutChat = getNMSClass("PacketPlayOutChat");
	private final Class<?> ChatMessageType = getNMSClass("ChatMessageType");
	private final Constructor<?> newPacketPlayOutChat = PacketPlayOutChat.getConstructor(IChatBaseComponent, ChatMessageType, UUID.class);;
	private final Enum[] ChatMessageType_values = getEnumValues(ChatMessageType);

	//DataWatcher
	private final Class<?> DataWatcher = getNMSClass("DataWatcher");
	private final Class<?> DataWatcherObject = getNMSClass("DataWatcherObject");
	private final Class<?> DataWatcherItem = getNMSClass("DataWatcher$Item");
	private final Class<?> DataWatcherRegistry = getNMSClass("DataWatcherRegistry");
	private final Class<?> DataWatcherSerializer = getNMSClass("DataWatcherSerializer");
	private final Constructor<?> newDataWatcher = DataWatcher.getConstructor(Entity);
	private final Constructor<?> newDataWatcherObject = DataWatcherObject.getConstructor(int.class, DataWatcherSerializer);
	private final Method DataWatcherItem_getType = DataWatcherItem.getMethod("a");
	private final Method DataWatcherItem_getValue = DataWatcherItem.getMethod("b");
	private final Method DataWatcherObject_getSlot = DataWatcherObject.getMethod("a");
	private final Method DataWatcherObject_getSerializer = DataWatcherObject.getMethod("b");
	private final Method DataWatcher_REGISTER = DataWatcher.getMethod("register", DataWatcherObject, Object.class);
	private final DataWatcherRegistry dataWatcherRegistry = new DataWatcherRegistryImpl();

	//PacketPlayOutSpawnEntityLiving
	private final Class<?> PacketPlayOutSpawnEntityLiving = getNMSClass("PacketPlayOutSpawnEntityLiving");
	private final Constructor<?> newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor();
	private final Field PacketPlayOutSpawnEntityLiving_ENTITYID = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("a"));
	private final Field PacketPlayOutSpawnEntityLiving_UUID = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("b"));
	private final Field PacketPlayOutSpawnEntityLiving_ENTITYTYPE = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("c"));
	private final Field PacketPlayOutSpawnEntityLiving_X = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("d"));
	private final Field PacketPlayOutSpawnEntityLiving_Y = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("e"));
	private final Field PacketPlayOutSpawnEntityLiving_Z = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("f"));
	private final Field PacketPlayOutSpawnEntityLiving_YAW = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("j"));
	private final Field PacketPlayOutSpawnEntityLiving_PITCH = setAccessible(PacketPlayOutSpawnEntityLiving.getDeclaredField("k"));

	//PacketPlayOutEntityTeleport
	private final Class<?> PacketPlayOutEntityTeleport = getNMSClass("PacketPlayOutEntityTeleport");
	private final Constructor<?> newPacketPlayOutEntityTeleport = PacketPlayOutEntityTeleport.getConstructor();
	private final Field PacketPlayOutEntityTeleport_ENTITYID = setAccessible(PacketPlayOutEntityTeleport.getDeclaredField("a"));
	private final Field PacketPlayOutEntityTeleport_X = setAccessible(PacketPlayOutEntityTeleport.getDeclaredField("b"));
	private final Field PacketPlayOutEntityTeleport_Y = setAccessible(PacketPlayOutEntityTeleport.getDeclaredField("c"));
	private final Field PacketPlayOutEntityTeleport_Z = setAccessible(PacketPlayOutEntityTeleport.getDeclaredField("d"));
	private final Field PacketPlayOutEntityTeleport_YAW = setAccessible(PacketPlayOutEntityTeleport.getDeclaredField("e"));
	private final Field PacketPlayOutEntityTeleport_PITCH = setAccessible(PacketPlayOutEntityTeleport.getDeclaredField("f"));

	//PacketPlayOutPlayerListHeaderFooter
	private final Class<?> PacketPlayOutPlayerListHeaderFooter = getNMSClass("PacketPlayOutPlayerListHeaderFooter");
	private final Constructor<?> newPacketPlayOutPlayerListHeaderFooter = PacketPlayOutPlayerListHeaderFooter.getConstructor();
	private final Field PacketPlayOutPlayerListHeaderFooter_HEADER = PacketPlayOutPlayerListHeaderFooter.getField("header");
	private final Field PacketPlayOutPlayerListHeaderFooter_FOOTER = PacketPlayOutPlayerListHeaderFooter.getField("footer");

	//other entity packets
	private final Class<?> PacketPlayInUseEntity = getNMSClass("PacketPlayInUseEntity");
	private final Class<?> EnumEntityUseAction = getNMSClass("PacketPlayInUseEntity$EnumEntityUseAction");
	private final Class<?> PacketPlayOutEntity = getNMSClass("PacketPlayOutEntity");
	private final Class<?> PacketPlayOutEntityDestroy = getNMSClass("PacketPlayOutEntityDestroy");
	private final Class<?> PacketPlayOutEntityLook = getNMSClass("PacketPlayOutEntity$PacketPlayOutEntityLook");
	private final Class<?> PacketPlayOutEntityMetadata = getNMSClass("PacketPlayOutEntityMetadata");
	private final Class<?> PacketPlayOutNamedEntitySpawn = getNMSClass("PacketPlayOutNamedEntitySpawn");
	private final Constructor<?> newPacketPlayOutEntityDestroy = PacketPlayOutEntityDestroy.getConstructor(int[].class);
	private final Constructor<?> newPacketPlayOutEntityMetadata = PacketPlayOutEntityMetadata.getConstructor(int.class, DataWatcher, boolean.class);
	private final Field PacketPlayInUseEntity_ENTITY = setAccessible(PacketPlayInUseEntity.getDeclaredField("a"));
	private final Field PacketPlayInUseEntity_ACTION = setAccessible(PacketPlayInUseEntity.getDeclaredField("action"));
	private final Field PacketPlayOutEntity_ENTITYID = setAccessible(PacketPlayOutEntity.getDeclaredField("a"));
	private final Field PacketPlayOutEntityDestroy_ENTITIES = setAccessible(PacketPlayOutEntityDestroy.getDeclaredFields()[0]);
	private final Field PacketPlayOutEntityMetadata_LIST = setAccessible(PacketPlayOutEntityMetadata.getDeclaredField("b"));
	private final Field PacketPlayOutNamedEntitySpawn_ENTITYID = setAccessible(PacketPlayOutNamedEntitySpawn.getDeclaredField("a"));

	//PacketPlayOutPlayerInfo
	private final Class<?> PacketPlayOutPlayerInfo = getNMSClass("PacketPlayOutPlayerInfo");
	private final Class<?> EnumPlayerInfoAction = getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
	private final Class<?> PlayerInfoData = getNMSClass("PacketPlayOutPlayerInfo$PlayerInfoData");
	private final Class<?> EnumGamemode = getNMSClass("EnumGamemode");
	private final Constructor<?> newPacketPlayOutPlayerInfo = PacketPlayOutPlayerInfo.getConstructor(EnumPlayerInfoAction, Iterable.class);
	private final Constructor<?> newPlayerInfoData = PlayerInfoData.getConstructor(PacketPlayOutPlayerInfo, GameProfile.class, int.class, EnumGamemode, IChatBaseComponent);
	private final Field PacketPlayOutPlayerInfo_ACTION = setAccessible(PacketPlayOutPlayerInfo.getDeclaredField("a"));
	private final Field PacketPlayOutPlayerInfo_PLAYERS = setAccessible(PacketPlayOutPlayerInfo.getDeclaredField("b"));
	private final Method PlayerInfoData_getProfile = PlayerInfoData.getMethod("a");
	private final Method PlayerInfoData_getLatency = PlayerInfoData.getMethod("b");
	private final Method PlayerInfoData_getGamemode = PlayerInfoData.getMethod("c");
	private final Method PlayerInfoData_getDisplayName = PlayerInfoData.getMethod("d");
	private final Enum[] EnumPlayerInfoAction_values = getEnumValues(EnumPlayerInfoAction);
	private final Enum[] EnumGamemode_values = getEnumValues(EnumGamemode);

	//scoreboard objectives
	private final Class<?> PacketPlayOutScoreboardDisplayObjective = getNMSClass("PacketPlayOutScoreboardDisplayObjective");
	private final Class<?> PacketPlayOutScoreboardObjective = getNMSClass("PacketPlayOutScoreboardObjective");
	private final Class<?> Scoreboard = getNMSClass("Scoreboard");
	private final Class<?> PacketPlayOutScoreboardScore = getNMSClass("PacketPlayOutScoreboardScore");
	private final Class<?> ScoreboardObjective = getNMSClass("ScoreboardObjective");
	private final Class<?> ScoreboardScore = getNMSClass("ScoreboardScore");
	private final Class<?> IScoreboardCriteria = getNMSClass("IScoreboardCriteria");
	private final Class<?> EnumScoreboardHealthDisplay = getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay");
	private final Class<?> EnumScoreboardAction = getNMSClass("ScoreboardServer$Action");
	private final Constructor<?> newScoreboardObjective = ScoreboardObjective.getConstructors()[0];
	private final Constructor<?> newScoreboard = Scoreboard.getConstructor();
	private final Constructor<?> newScoreboardScore = ScoreboardScore.getConstructor(Scoreboard, ScoreboardObjective, String.class);
	private final Constructor<?> newPacketPlayOutScoreboardDisplayObjective = PacketPlayOutScoreboardDisplayObjective.getConstructor(int.class, ScoreboardObjective);
	private final Constructor<?> newPacketPlayOutScoreboardObjective = PacketPlayOutScoreboardObjective.getConstructor(ScoreboardObjective, int.class);
	private final Constructor<?> newPacketPlayOutScoreboardScore = PacketPlayOutScoreboardScore.getConstructor(EnumScoreboardAction, String.class, String.class, int.class);
	private final Field PacketPlayOutScoreboardDisplayObjective_POSITION = setAccessible(PacketPlayOutScoreboardDisplayObjective.getDeclaredField("a"));
	private final Field PacketPlayOutScoreboardDisplayObjective_OBJECTIVENAME = setAccessible(PacketPlayOutScoreboardDisplayObjective.getDeclaredField("b"));
	private final Field PacketPlayOutScoreboardObjective_OBJECTIVENAME = setAccessible(PacketPlayOutScoreboardObjective.getField("a"));
	private final Field PacketPlayOutScoreboardObjective_DISPLAYNAME = setAccessible(PacketPlayOutScoreboardObjective.getField("b"));
	private final Field PacketPlayOutScoreboardObjective_RENDERTYPE = setAccessible(PacketPlayOutScoreboardObjective.getField("c"));
	private final Field PacketPlayOutScoreboardObjective_METHOD = setAccessible(PacketPlayOutScoreboardObjective.getField("d"));
	private final Field IScoreboardCriteria_DUMMY = IScoreboardCriteria.getDeclaredField("DUMMY");
	private final Method ScoreboardScore_setScore = ScoreboardScore.getMethod("setScore", int.class);
	private final Enum[] EnumScoreboardHealthDisplay_values = getEnumValues(EnumScoreboardHealthDisplay);
	private final Enum[] EnumScoreboardAction_values = getEnumValues(EnumScoreboardAction);

	//PacketPlayOutScoreboardTeam
	private final Class<?> PacketPlayOutScoreboardTeam = getNMSClass("PacketPlayOutScoreboardTeam");
	private final Class<?> ScoreboardTeam = getNMSClass("ScoreboardTeam");
	private final Class<?> EnumNameTagVisibility = getNMSClass("ScoreboardTeamBase$EnumNameTagVisibility");
	private final Class<?> EnumTeamPush = getNMSClass("ScoreboardTeamBase$EnumTeamPush");
	private final Constructor<?> newScoreboardTeam = ScoreboardTeam.getConstructor(Scoreboard, String.class);
	private final Constructor<?> newPacketPlayOutScoreboardTeam = PacketPlayOutScoreboardTeam.getConstructor(ScoreboardTeam, int.class);
	private final Field PacketPlayOutScoreboardTeam_NAME = setAccessible(PacketPlayOutScoreboardTeam.getDeclaredField("a"));
	private final Field PacketPlayOutScoreboardTeam_PLAYERS = setAccessible(PacketPlayOutScoreboardTeam.getDeclaredField("h"));
	private final Method ScoreboardTeam_getPlayerNameSet = ScoreboardTeam.getMethod("getPlayerNameSet");
	private final Method ScoreboardTeam_setNameTagVisibility = ScoreboardTeam.getMethod("setNameTagVisibility", EnumNameTagVisibility);
	private final Method ScoreboardTeam_setCollisionRule = ScoreboardTeam.getMethod("setCollisionRule", EnumTeamPush);
	private final Method ScoreboardTeam_setPrefix = ScoreboardTeam.getMethod("setPrefix", IChatBaseComponent);
	private final Method ScoreboardTeam_setSuffix = ScoreboardTeam.getMethod("setSuffix", IChatBaseComponent);
	private final Method ScoreboardTeam_setColor = ScoreboardTeam.getMethod("setColor", EnumChatFormat);
	private final Method ScoreboardTeam_setAllowFriendlyFire = ScoreboardTeam.getMethod("setAllowFriendlyFire", boolean.class);
	private final Method ScoreboardTeam_setCanSeeFriendlyInvisibles = ScoreboardTeam.getMethod("setCanSeeFriendlyInvisibles", boolean.class);;
	private final Enum[] EnumNameTagVisibility_values = getEnumValues(EnumNameTagVisibility);
	private final Enum[] EnumTeamPush_values = getEnumValues(EnumTeamPush);


	public AdapterImpl() throws ReflectiveOperationException {

	}

	private Class<?> getNMSClass(String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server.v1_16_R1." + name);
	}

	private Enum[] getEnumValues(Class<?> enumClass) {
		if (enumClass == null) throw new IllegalArgumentException("Class cannot be null");
		if (!enumClass.isEnum()) throw new IllegalArgumentException(enumClass.getName() + " is not an enum class");
		try {
			return (Enum[]) enumClass.getMethod("values").invoke(null);
		} catch (ReflectiveOperationException e) {
			//this should never happen
			TAB.getInstance().getErrorManager().criticalError("Failed to load enum constants of " + enumClass.getName(), e);
			return new Enum[0];
		}
	}

	public void setField(Object obj, Field field, Object value) throws IllegalAccessException {
		field.set(obj, value);
	}

	public <T extends AccessibleObject> T setAccessible(T o) {
		o.setAccessible(true);
		return o;
	}

	@Override
	public DataWatcherRegistry getDataWatcherRegistry() {
		return dataWatcherRegistry;
	}

	@Override
	public DataWatcher adaptDataWatcher(Object dataWatcher) throws ReflectiveOperationException {
		DataWatcher watcher = new DataWatcher();
		List<Object> items = (List<Object>) dataWatcher.getClass().getMethod("c").invoke(dataWatcher);
		if (items != null) {
			for (Object item : items) {
				Object nmsObject = DataWatcherItem_getType.invoke(item);
				me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherItem w = 
						new me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherItem(
								new me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherObject(
										(int) DataWatcherObject_getSlot.invoke(nmsObject), 
										DataWatcherObject_getSerializer.invoke(nmsObject)), DataWatcherItem_getValue.invoke(item));
				watcher.setValue(w.getType(), w.getValue());
			}
		}
		return watcher;
	}

	@Override
	public Channel getChannel(Player player) throws ReflectiveOperationException {
		return (Channel) CHANNEL.get(NETWORK_MANAGER.get(PLAYER_CONNECTION.get(getHandle.invoke(player))));
	}

	@Override
	public int getPing(Player player) throws ReflectiveOperationException {
		return PING.getInt(getHandle.invoke(player));
	}

	@Override
	public Object getSkin(Player player) throws ReflectiveOperationException {
		return ((GameProfile) getProfile.invoke(getHandle.invoke(player))).getProperties();
	}

	@Override
	public void sendPacket(Player player, Object packet) throws ReflectiveOperationException {
		sendPacket.invoke(PLAYER_CONNECTION.get(getHandle.invoke(player)), packet);
	}

	@Override
	public IChatBaseComponent adaptComponent(Object component) throws ReflectiveOperationException {
		if (!ChatComponentText.isInstance(component)) return null; //paper
		IChatBaseComponent chat = new IChatBaseComponent((String) ChatComponentText_text.get(component));
		Object modifier = ChatBaseComponent_modifier.get(component);
		if (modifier != null) {
			chat.getModifier().setColor(fromNMSColor(ChatModifier_color.get(modifier)));
			chat.getModifier().setBold((Boolean) ChatModifier_bold.get(modifier));
			chat.getModifier().setItalic((Boolean) ChatModifier_italic.get(modifier));
			chat.getModifier().setObfuscated((Boolean) ChatModifier_obfuscated.get(modifier));
			chat.getModifier().setStrikethrough((Boolean) ChatModifier_strikethrough.get(modifier));
			chat.getModifier().setUnderlined((Boolean) ChatModifier_underlined.get(modifier));
			Object clickEvent = ChatModifier_clickEvent.get(modifier);
			if (clickEvent != null) {
				chat.getModifier().onClick(me.neznamy.tab.api.chat.ChatClickable.EnumClickAction.valueOf(ChatClickable_action.get(clickEvent).toString().toUpperCase()), (String) ChatClickable_value.get(clickEvent));
			}
			Object hoverEvent = ChatModifier_hoverEvent.get(modifier);
			if (hoverEvent != null) {
				//does not support show_item on 1.16+
				JsonObject json = (JsonObject) ChatHoverable_serialize.invoke(hoverEvent);
				me.neznamy.tab.api.chat.ChatHoverable.EnumHoverAction action = me.neznamy.tab.api.chat.ChatHoverable.EnumHoverAction.valueOf(json.get("action").getAsString().toUpperCase());
				IChatBaseComponent value = me.neznamy.tab.api.chat.IChatBaseComponent.deserialize(json.get("contents").getAsJsonObject().toString());
				chat.getModifier().onHover(action, value);
			}
		}
		for (Object extra : (List<Object>) ChatBaseComponent_extra.get(component)) {
			chat.addExtra(adaptComponent(extra));
		}
		return chat;
	}
	
	private TextColor fromNMSColor(Object color) throws ReflectiveOperationException {
		if (color == null) return null;
		String name = (String) ChatHexColor_name.get(color);
		if (name != null) {
			//legacy code
			return new TextColor(me.neznamy.tab.api.chat.EnumChatFormat.valueOf(name.toUpperCase(Locale.US)));
		} else {
			int rgb = (int) ChatHexColor_rgb.get(color);
			return new TextColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
		}
	}

	@Override
	public boolean isPlayerInfoPacket(Object packet) {
		return PacketPlayOutPlayerInfo.isInstance(packet);
	}

	@Override
	public boolean isTeamPacket(Object packet) {
		return PacketPlayOutScoreboardTeam.isInstance(packet);
	}

	@Override
	public boolean isDisplayObjectivePacket(Object packet) {
		return PacketPlayOutScoreboardDisplayObjective.isInstance(packet);
	}

	@Override
	public boolean isObjectivePacket(Object packet) {
		return PacketPlayOutScoreboardObjective.isInstance(packet);
	}

	@Override
	public boolean isInteractPacket(Object packet) {
		return PacketPlayInUseEntity.isInstance(packet);
	}

	@Override
	public boolean isMovePacket(Object packet) {
		return PacketPlayOutEntity.isInstance(packet);
	}

	@Override
	public boolean isHeadLookPacket(Object packet) {
		return PacketPlayOutEntityLook.isInstance(packet);
	}

	@Override
	public boolean isTeleportPacket(Object packet) {
		return PacketPlayOutEntityTeleport.isInstance(packet);
	}

	@Override
	public boolean isSpawnLivingEntityPacket(Object packet) {
		return PacketPlayOutSpawnEntityLiving.isInstance(packet);
	}

	@Override
	public boolean isSpawnPlayerPacket(Object packet) {
		return PacketPlayOutNamedEntitySpawn.isInstance(packet);
	}

	@Override
	public boolean isDestroyPacket(Object packet) {
		return PacketPlayOutEntityDestroy.isInstance(packet);
	}

	@Override
	public boolean isMetadataPacket(Object packet) {
		return PacketPlayOutEntityMetadata.isInstance(packet);
	}

	@Override
	public boolean isInteractionAction(Object packet) throws ReflectiveOperationException {
		return PacketPlayInUseEntity_ACTION.get(packet).toString().equals("INTERACT");
	}

	@Override
	public Collection<String> getTeamPlayers(Object teamPacket) throws ReflectiveOperationException {
		return (Collection<String>) PacketPlayOutScoreboardTeam_PLAYERS.get(teamPacket);
	}

	@Override
	public void setTeamPlayers(Object teamPacket, Collection<String> players) throws ReflectiveOperationException {
		PacketPlayOutScoreboardTeam_PLAYERS.set(teamPacket, players);
	}

	@Override
	public String getTeamName(Object teamPacket) throws ReflectiveOperationException {
		return (String) PacketPlayOutScoreboardTeam_NAME.get(teamPacket);
	}

	@Override
	public Object createChatPacket(Object component, PacketPlayOutChat.ChatMessageType messageType) throws ReflectiveOperationException {
		return newPacketPlayOutChat.newInstance(component, ChatMessageType_values[messageType.ordinal()], UUID.randomUUID());
	}

	@Override
	public Object createPlayerInfoPacket(PacketPlayOutPlayerInfo.EnumPlayerInfoAction action, List<PacketPlayOutPlayerInfo.PlayerInfoData> players, ProtocolVersion clientVersion) throws ReflectiveOperationException {
		Object nmsPacket = newPacketPlayOutPlayerInfo.newInstance(EnumPlayerInfoAction_values[action.ordinal()], Array.newInstance(EntityPlayer, 0));
		List<Object> items = new ArrayList<>();
		for (me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo.PlayerInfoData data : players) {
			GameProfile profile = new GameProfile(data.getUniqueId(), data.getName());
			if (data.getSkin() != null) profile.getProperties().putAll((PropertyMap) data.getSkin());
			List<Object> parameters = new ArrayList<>();
			if (newPlayerInfoData.getParameterCount() == 5) {
				parameters.add(nmsPacket);
			}
			parameters.add(profile);
			parameters.add(data.getLatency());
			parameters.add(data.getGameMode() == null ? null : EnumGamemode_values[EnumGamemode_values.length-me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo.EnumGamemode.values().length+data.getGameMode().ordinal()]); //not_set was removed in 1.17
			parameters.add(data.getDisplayName() == null ? null : adaptComponent(data.getDisplayName(), clientVersion));
			items.add(newPlayerInfoData.newInstance(parameters.toArray()));
		}
		setField(nmsPacket, PacketPlayOutPlayerInfo_PLAYERS, items);
		return nmsPacket;
	}

	@Override
	public Object createPlayerListHeaderFooterPacket(IChatBaseComponent header, IChatBaseComponent footer, ProtocolVersion clientVersion) throws ReflectiveOperationException {
		Object nmsPacket = newPacketPlayOutPlayerListHeaderFooter.newInstance();
		setField(nmsPacket, PacketPlayOutPlayerListHeaderFooter_HEADER, adaptComponent(header, clientVersion));
		setField(nmsPacket, PacketPlayOutPlayerListHeaderFooter_FOOTER, adaptComponent(footer, clientVersion));
		return nmsPacket;
	}

	@Override
	public Object createDisplayObjectivePacket(int slot, String objectiveName) throws ReflectiveOperationException {
		return newPacketPlayOutScoreboardDisplayObjective.newInstance(slot, newScoreboardObjective.newInstance(null, objectiveName, null, newChatComponentText.newInstance(""), null));
	}

	@Override
	public Object createObjectivePacket(int method, String name, Object displayName, PacketPlayOutScoreboardObjective.EnumScoreboardHealthDisplay renderType) throws ReflectiveOperationException {
		return newPacketPlayOutScoreboardObjective.newInstance(newScoreboardObjective.newInstance(null, name, null, 
				displayName, renderType == null ? null : EnumScoreboardHealthDisplay_values[renderType.ordinal()]), method);
	}

	@Override
	public Object createScorePacket(PacketPlayOutScoreboardScore.Action action, String objectiveName, String player, int score) {
		return null;
	}

	@Override
	public Object createTeamPacket(String name, String prefix, String suffix, String nametagVisibility, String collisionRule, EnumChatFormat color, Collection<String> players, int method, int options) {
		return null;
	}

	@Override
	public Object createEntityDestroyPacket(int[] entities) {
		return null;
	}

	@Override
	public Object createMetadataPacket(int entityId, DataWatcher metadata) {
		return null;
	}

	@Override
	public Object createSpawnLivingEntityPacket(int entityId, UUID uuid, EntityType type, Location location, DataWatcher dataWatcher) {
		return null;
	}

	@Override
	public Object createTeleportPacket(int entityId, Location location) {
		return null;
	}

	@Override
	public PacketPlayOutPlayerInfo createPlayerInfoPacket(Object nmsPacket) {
		return null;
	}

	@Override
	public PacketPlayOutScoreboardObjective createObjectivePacket(Object nmsPacket) {
		return null;
	}

	@Override
	public PacketPlayOutScoreboardDisplayObjective createDisplayObjectivePacket(Object nmsPacket) {
		return null;
	}

	@Override
	public Object adaptComponent(IChatBaseComponent component, ProtocolVersion clientVersion) {
		return null;
	}

	@Override
	public int getMoveEntityId(Object packet) {
		return 0;
	}

	@Override
	public int getTeleportEntityId(Object packet) {
		return 0;
	}

	@Override
	public int getPlayerSpawnId(Object packet) {
		return 0;
	}

	@Override
	public int[] getDestroyEntities(Object packet) {
		return new int[0];
	}

	@Override
	public int getInteractEntityId(Object packet) {
		return 0;
	}

	@Override
	public DataWatcher getLivingEntityMetadata(Object packet) {
		return null;
	}

	@Override
	public void setLivingEntityMetadata(Object packet, DataWatcher metadata) {

	}

	@Override
	public List<Object> getMetadataEntries(Object packet) {
		return null;
	}

	@Override
	public int getMetadataSlot(Object item) {
		return 0;
	}

	@Override
	public Object getMetadataValue(Object item) {
		return null;
	}

	@Override
	public void setInteractEntityId(Object packet, int entityId) {

	}
}
