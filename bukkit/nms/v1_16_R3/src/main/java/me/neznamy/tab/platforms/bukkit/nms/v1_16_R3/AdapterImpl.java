package me.neznamy.tab.platforms.bukkit.nms.v1_16_R3;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.channel.Channel;
import me.neznamy.tab.api.ProtocolVersion;
import me.neznamy.tab.api.chat.ChatComponentEntity;
import me.neznamy.tab.api.chat.EnumChatFormat;
import me.neznamy.tab.api.chat.IChatBaseComponent;
import me.neznamy.tab.api.chat.TextColor;
import me.neznamy.tab.api.protocol.PacketPlayOutChat;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardDisplayObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardScore;
import me.neznamy.tab.platforms.bukkit.nms.Adapter;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcher;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherItem;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherRegistry;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.*;

public final class AdapterImpl implements Adapter {

    //PacketPlayOutScoreboardTeam
    private final Field PacketPlayOutScoreboardTeam_NAME = setAccessible(PacketPlayOutScoreboardTeam.class.getDeclaredField("a"));
    private final Field PacketPlayOutScoreboardTeam_PLAYERS = setAccessible(PacketPlayOutScoreboardTeam.class.getDeclaredField("h"));

    //PacketPlayOutPlayerInfo
    private final Field PacketPlayOutPlayerInfo_ACTION = setAccessible(net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.class.getDeclaredField("a"));
    private final Field PacketPlayOutPlayerInfo_PLAYERS = setAccessible(net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.class.getDeclaredField("b"));

    //PacketPlayOutSpawnEntityLiving
    private final Field PacketPlayOutSpawnEntityLiving_ENTITYID = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("a"));
    private final Field PacketPlayOutSpawnEntityLiving_UUID = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("b"));
    private final Field PacketPlayOutSpawnEntityLiving_ENTITYTYPE = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("c"));
    private final Field PacketPlayOutSpawnEntityLiving_X = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("d"));
    private final Field PacketPlayOutSpawnEntityLiving_Y = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("e"));
    private final Field PacketPlayOutSpawnEntityLiving_Z = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("f"));
    private final Field PacketPlayOutSpawnEntityLiving_YAW = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("j"));
    private final Field PacketPlayOutSpawnEntityLiving_PITCH = setAccessible(PacketPlayOutSpawnEntityLiving.class.getDeclaredField("k"));

    //PacketPlayOutEntityTeleport
    private final Field PacketPlayOutEntityTeleport_ENTITYID = setAccessible(PacketPlayOutEntityTeleport.class.getDeclaredField("a"));
    private final Field PacketPlayOutEntityTeleport_X = setAccessible(PacketPlayOutEntityTeleport.class.getDeclaredField("b"));
    private final Field PacketPlayOutEntityTeleport_Y = setAccessible(PacketPlayOutEntityTeleport.class.getDeclaredField("c"));
    private final Field PacketPlayOutEntityTeleport_Z = setAccessible(PacketPlayOutEntityTeleport.class.getDeclaredField("d"));
    private final Field PacketPlayOutEntityTeleport_YAW = setAccessible(PacketPlayOutEntityTeleport.class.getDeclaredField("e"));
    private final Field PacketPlayOutEntityTeleport_PITCH = setAccessible(PacketPlayOutEntityTeleport.class.getDeclaredField("f"));

    //PacketPlayOutScoreboardObjective
    private final Field PacketPlayOutScoreboardObjective_OBJECTIVENAME = setAccessible(net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardObjective.class.getDeclaredField("a"));
    private final Field PacketPlayOutScoreboardObjective_METHOD = setAccessible(net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardObjective.class.getDeclaredField("d"));

    //PacketPlayOutScoreboardDisplayObjective
    private final Field PacketPlayOutScoreboardDisplayObjective_POSITION = setAccessible(net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardDisplayObjective.class.getDeclaredField("a"));
    private final Field PacketPlayOutScoreboardDisplayObjective_OBJECTIVENAME = setAccessible(net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardDisplayObjective.class.getDeclaredField("b"));

    private final Field PacketPlayInUseEntity_ENTITY = setAccessible(PacketPlayInUseEntity.class.getDeclaredField("a"));
    private final Field PacketPlayOutEntity_ENTITYID = setAccessible(PacketPlayOutEntity.class.getDeclaredField("a"));
    private final Field PacketPlayOutEntityDestroy_ENTITIES = setAccessible(PacketPlayOutEntityDestroy.class.getDeclaredField("a"));
    private final Field PacketPlayOutNamedEntitySpawn_ENTITYID = setAccessible(PacketPlayOutNamedEntitySpawn.class.getDeclaredField("a"));
    private final Field PacketPlayOutEntityMetadata_LIST = setAccessible(PacketPlayOutEntityMetadata.class.getDeclaredField("b"));

    private final Map<IChatBaseComponent, net.minecraft.server.v1_16_R3.IChatBaseComponent> componentCacheModern = new HashMap<>();
    private final Map<IChatBaseComponent, net.minecraft.server.v1_16_R3.IChatBaseComponent> componentCacheLegacy = new HashMap<>();

    private final DataWatcherRegistry dataWatcherRegistry = new DataWatcherRegistryImpl();
    private final Scoreboard dummyScoreboard = new Scoreboard();
    private final ChatComponentText dummyComponent = new ChatComponentText("");
    private final EnumMap<EntityType, Integer> entityIds = new EnumMap<>(EntityType.class);

    public AdapterImpl() throws ReflectiveOperationException {
        entityIds.put(EntityType.ARMOR_STAND, 1);
        entityIds.put(EntityType.WITHER, 83);
    }

    private <T extends AccessibleObject> T setAccessible(T o) {
        o.setAccessible(true);
        return o;
    }

    private void setField(Object obj, Field field, Object value) throws IllegalAccessException {
        field.set(obj, value);
    }

    @Override
    public DataWatcherRegistry getDataWatcherRegistry() {
        return dataWatcherRegistry;
    }

    @Override
    public DataWatcher adaptDataWatcher(Object dataWatcher) {
        DataWatcher watcher = new DataWatcher();
        List<net.minecraft.server.v1_16_R3.DataWatcher.Item<?>> items = ((net.minecraft.server.v1_16_R3.DataWatcher)dataWatcher).c();
        if (items != null) {
            for (net.minecraft.server.v1_16_R3.DataWatcher.Item<?> item : items) {
                watcher.setValue(new me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherObject(item.a().a(), item.a().b()), item.b());
            }
        }
        return watcher;
    }

    @Override
    public Channel getChannel(Player player) {
        return ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;
    }

    @Override
    public int getPing(Player player) {
        return ((CraftPlayer)player).getHandle().ping;
    }

    @Override
    public Object getSkin(Player player) {
        return ((CraftPlayer)player).getHandle().getProfile().getProperties();
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket((Packet<?>) packet);
    }

    @Override
    public IChatBaseComponent adaptComponent(Object component) {
        IChatBaseComponent chat = new IChatBaseComponent(((net.minecraft.server.v1_16_R3.IChatBaseComponent)component).getText());
        ChatModifier modifier = ((net.minecraft.server.v1_16_R3.IChatBaseComponent)component).getChatModifier();
        if (modifier != null) {
            chat.getModifier().setColor(modifier.getColor() == null ? null : TextColor.fromString(modifier.getColor().b()));
            chat.getModifier().setBold(modifier.isBold());
            chat.getModifier().setItalic(modifier.isItalic());
            chat.getModifier().setObfuscated(modifier.isRandom());
            chat.getModifier().setStrikethrough(modifier.isStrikethrough());
            chat.getModifier().setUnderlined(modifier.isUnderlined());
            ChatClickable clickEvent = modifier.getClickEvent();
            if (clickEvent != null) {
                chat.getModifier().onClick(me.neznamy.tab.api.chat.ChatClickable.EnumClickAction.valueOf(clickEvent.a().toString().toUpperCase()), clickEvent.b());
            }
            ChatHoverable hoverEvent = modifier.getHoverEvent();
            if (hoverEvent != null) {
                //does not support show_item on 1.16+
                JsonObject json = hoverEvent.b();
                me.neznamy.tab.api.chat.ChatHoverable.EnumHoverAction action = me.neznamy.tab.api.chat.ChatHoverable.EnumHoverAction.valueOf(json.get("action").getAsString().toUpperCase());
                IChatBaseComponent value = IChatBaseComponent.deserialize(json.get("contents").getAsJsonObject().toString());
                chat.getModifier().onHover(action, value);
            }
        }
        for (net.minecraft.server.v1_16_R3.IChatBaseComponent extra : ((net.minecraft.server.v1_16_R3.IChatBaseComponent)component).getSiblings()) {
            chat.addExtra(adaptComponent(extra));
        }
        return chat;
    }

    @Override
    public boolean isPlayerInfoPacket(Object packet) {
        return packet instanceof net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo;
    }

    @Override
    public boolean isTeamPacket(Object packet) {
        return packet instanceof PacketPlayOutScoreboardTeam;
    }

    @Override
    public boolean isDisplayObjectivePacket(Object packet) {
        return packet instanceof net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardDisplayObjective;
    }

    @Override
    public boolean isObjectivePacket(Object packet) {
        return packet instanceof net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardObjective;
    }

    @Override
    public boolean isInteractPacket(Object packet) {
        return packet instanceof PacketPlayInUseEntity;
    }

    @Override
    public boolean isMovePacket(Object packet) {
        return packet instanceof PacketPlayOutEntity;
    }

    @Override
    public boolean isHeadLookPacket(Object packet) {
        return packet instanceof PacketPlayOutEntityHeadRotation;
    }

    @Override
    public boolean isTeleportPacket(Object packet) {
        return packet instanceof PacketPlayOutEntityTeleport;
    }

    @Override
    public boolean isSpawnLivingEntityPacket(Object packet) {
        return packet instanceof PacketPlayOutSpawnEntityLiving;
    }

    @Override
    public boolean isSpawnPlayerPacket(Object packet) {
        return packet instanceof PacketPlayOutNamedEntitySpawn;
    }

    @Override
    public boolean isDestroyPacket(Object packet) {
        return packet instanceof PacketPlayOutEntityDestroy;
    }

    @Override
    public boolean isMetadataPacket(Object packet) {
        return packet instanceof PacketPlayOutEntityMetadata;
    }

    @Override
    public boolean isInteractionAction(Object packet) {
        return ((PacketPlayInUseEntity)packet).b() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT;
    }

    @Override
    public Collection<String> getTeamPlayers(Object teamPacket) throws ReflectiveOperationException {
        return (Collection<String>) PacketPlayOutScoreboardTeam_PLAYERS.get(teamPacket);
    }

    @Override
    public void setTeamPlayers(Object teamPacket, Collection<String> players) throws ReflectiveOperationException {
        setField(teamPacket, PacketPlayOutScoreboardTeam_PLAYERS, players);
    }

    @Override
    public String getTeamName(Object teamPacket) throws ReflectiveOperationException {
        return (String) PacketPlayOutScoreboardTeam_NAME.get(teamPacket);
    }

    @Override
    public Object createChatPacket(Object component, PacketPlayOutChat.ChatMessageType messageType) {
        return new net.minecraft.server.v1_16_R3.PacketPlayOutChat((net.minecraft.server.v1_16_R3.IChatBaseComponent) component,
                ChatMessageType.values()[messageType.ordinal()], UUID.randomUUID());
    }

    @Override
    public Object createPlayerInfoPacket(ProtocolVersion clientVersion, PacketPlayOutPlayerInfo.EnumPlayerInfoAction action,
                                         List<PacketPlayOutPlayerInfo.PlayerInfoData> players) throws ReflectiveOperationException {
        net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo nmsPacket = new net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo(
                net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.EnumPlayerInfoAction.values()[action.ordinal()]);
        List<Object> items = new ArrayList<>();
        for (me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo.PlayerInfoData data : players) {
            GameProfile profile = new GameProfile(data.getUniqueId(), data.getName());
            if (data.getSkin() != null) profile.getProperties().putAll((PropertyMap) data.getSkin());
            items.add(nmsPacket.new PlayerInfoData(profile, data.getLatency(),
                    data.getGameMode() == null ? null : EnumGamemode.values()[data.getGameMode().ordinal()],
                    data.getDisplayName() == null ? null : adaptComponent(data.getDisplayName(), clientVersion)));
        }
        setField(nmsPacket, PacketPlayOutPlayerInfo_PLAYERS, items);
        return nmsPacket;
    }

    @Override
    public Object createPlayerListHeaderFooterPacket(ProtocolVersion clientVersion, IChatBaseComponent header, IChatBaseComponent footer) {
        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
        packet.header = adaptComponent(header, clientVersion);
        packet.footer = adaptComponent(footer, clientVersion);
        return packet;
    }

    @Override
    public Object createDisplayObjectivePacket(int slot, String objectiveName) {
        return new net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardDisplayObjective(slot, new ScoreboardObjective(null, objectiveName, null, dummyComponent, null));
    }

    @Override
    public Object createObjectivePacket(int method, String name, Object displayName, PacketPlayOutScoreboardObjective.EnumScoreboardHealthDisplay renderType) {
        return new net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardObjective(new ScoreboardObjective(null, name, null,
                (net.minecraft.server.v1_16_R3.IChatBaseComponent) displayName,
                renderType == null ? null : IScoreboardCriteria.EnumScoreboardHealthDisplay.values()[renderType.ordinal()]), method);
    }

    @Override
    public Object createScorePacket(PacketPlayOutScoreboardScore.Action action, String objectiveName, String player, int score) {
        return new net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardScore(ScoreboardServer.Action.values()[action.ordinal()],
                objectiveName, player, score);
    }

    @Override
    public Object createTeamPacket(ProtocolVersion clientVersion, String name, String prefix, String suffix, String nameTagVisibility,
                                   String collisionRule, EnumChatFormat color, Collection<String> players, int method, int options) {
        ScoreboardTeam team = new ScoreboardTeam(dummyScoreboard, name);
        team.getPlayerNameSet().addAll(players);
        team.setAllowFriendlyFire((options & 0x1) > 0);
        team.setCanSeeFriendlyInvisibles((options & 0x2) > 0);
        if (prefix != null) team.setPrefix(adaptComponent(IChatBaseComponent.optimizedComponent(prefix), clientVersion));
        if (suffix != null) team.setSuffix(adaptComponent(IChatBaseComponent.optimizedComponent(suffix), clientVersion));
        EnumChatFormat format = color != null ? color : EnumChatFormat.lastColorsOf(prefix);
        team.setColor(net.minecraft.server.v1_16_R3.EnumChatFormat.values()[format.ordinal()]);
        team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.a(nameTagVisibility));
        team.setCollisionRule(ScoreboardTeamBase.EnumTeamPush.a(collisionRule));
        return new PacketPlayOutScoreboardTeam(team, method);
    }

    @Override
    public Object createEntityDestroyPacket(int[] entities) {
        return new PacketPlayOutEntityDestroy(entities);
    }

    @Override
    public Object createMetadataPacket(int entityId, DataWatcher metadata) {
        net.minecraft.server.v1_16_R3.DataWatcher nmsWatcher = new net.minecraft.server.v1_16_R3.DataWatcher(null);
        for (DataWatcherItem item : metadata.getItems()) {
            DataWatcherObject position = new DataWatcherObject(item.getType().getPosition(), (DataWatcherSerializer) item.getType().getClassType());
            nmsWatcher.register(position, item.getValue());
        }
        return new PacketPlayOutEntityMetadata(entityId, nmsWatcher, true);
    }

    @Override
    public Object createSpawnLivingEntityPacket(int entityId, UUID uuid, EntityType type, Location location, DataWatcher dataWatcher) throws IllegalAccessException {
        PacketPlayOutSpawnEntityLiving nmsPacket = new PacketPlayOutSpawnEntityLiving();
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_ENTITYID, entityId);
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_UUID, uuid);
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_ENTITYTYPE, entityIds.get(type));
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_X, location.getX());
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_Y, location.getY());
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_Z, location.getZ());
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_YAW, (byte)(location.getYaw() * 256.0f / 360.0f));
        setField(nmsPacket, PacketPlayOutSpawnEntityLiving_PITCH, (byte)(location.getPitch() * 256.0f / 360.0f));
        return nmsPacket;
    }

    @Override
    public Object createTeleportPacket(int entityId, Location location) throws IllegalAccessException {
        Object nmsPacket;
        nmsPacket = new PacketPlayOutEntityTeleport();
        setField(nmsPacket, PacketPlayOutEntityTeleport_ENTITYID, entityId);
        setField(nmsPacket, PacketPlayOutEntityTeleport_X, location.getX());
        setField(nmsPacket, PacketPlayOutEntityTeleport_Y, location.getY());
        setField(nmsPacket, PacketPlayOutEntityTeleport_Z, location.getZ());
        setField(nmsPacket, PacketPlayOutEntityTeleport_YAW, (byte) (location.getYaw()/360*256));
        setField(nmsPacket, PacketPlayOutEntityTeleport_PITCH, (byte) (location.getPitch()/360*256));
        return nmsPacket;
    }

    @Override
    public PacketPlayOutPlayerInfo createPlayerInfoPacket(Object nmsPacket) throws IllegalAccessException {
        PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.valueOf(PacketPlayOutPlayerInfo_ACTION.get(nmsPacket).toString());
        List<PacketPlayOutPlayerInfo.PlayerInfoData> listData = new ArrayList<>();
        for (Object nmsData : (List<?>) PacketPlayOutPlayerInfo_PLAYERS.get(nmsPacket)) {
            EnumGamemode nmsGameMode = ((net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.PlayerInfoData)nmsData).c();
            GameProfile profile = ((net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.PlayerInfoData)nmsData).a();
            net.minecraft.server.v1_16_R3.IChatBaseComponent nmsComponent = ((net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.PlayerInfoData)nmsData).d();
            PropertyMap map = new PropertyMap();
            map.putAll(profile.getProperties());
            listData.add(new PacketPlayOutPlayerInfo.PlayerInfoData(profile.getName(), profile.getId(), map,
                    ((net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo.PlayerInfoData)nmsData).b(),
                    nmsGameMode == null ? null : PacketPlayOutPlayerInfo.EnumGamemode.valueOf(nmsGameMode.toString()),
                    nmsComponent == null ? null : adaptComponent(nmsComponent)));
        }
        return new PacketPlayOutPlayerInfo(action, listData);
    }

    @Override
    public PacketPlayOutScoreboardObjective createObjectivePacket(Object nmsPacket) throws IllegalAccessException {
        String objective = (String) PacketPlayOutScoreboardObjective_OBJECTIVENAME.get(nmsPacket);
        int method = PacketPlayOutScoreboardObjective_METHOD.getInt(nmsPacket);
        return new PacketPlayOutScoreboardObjective(method, objective, null, null);
    }

    @Override
    public PacketPlayOutScoreboardDisplayObjective createDisplayObjectivePacket(Object nmsPacket) throws IllegalAccessException {
        return new PacketPlayOutScoreboardDisplayObjective(
                PacketPlayOutScoreboardDisplayObjective_POSITION.getInt(nmsPacket),
                (String) PacketPlayOutScoreboardDisplayObjective_OBJECTIVENAME.get(nmsPacket)
        );
    }

    @Override
    public net.minecraft.server.v1_16_R3.IChatBaseComponent adaptComponent(IChatBaseComponent component, ProtocolVersion clientVersion) {
        if (component == null) return null;
        net.minecraft.server.v1_16_R3.IChatBaseComponent obj;
        if (clientVersion.getMinorVersion() >= 16) {
            if (componentCacheModern.containsKey(component)) return componentCacheModern.get(component);
            obj = adaptComponent0(component, clientVersion);
            if (componentCacheModern.size() > 10000) componentCacheModern.clear();
            componentCacheModern.put(component, obj);
        } else {
            if (componentCacheLegacy.containsKey(component)) return componentCacheLegacy.get(component);
            obj = adaptComponent0(component, clientVersion);
            if (componentCacheLegacy.size() > 10000) componentCacheLegacy.clear();
            componentCacheLegacy.put(component, obj);
        }
        return obj;
    }

    //separate method to prevent extras counting cpu again due to recursion and finally showing higher usage than real
    private net.minecraft.server.v1_16_R3.IChatBaseComponent adaptComponent0(IChatBaseComponent component, ProtocolVersion clientVersion) {
        ChatComponentText chat = new ChatComponentText(component.getText());
        ChatClickable clickEvent = component.getModifier().getClickEvent() == null ? null :
                new ChatClickable(ChatClickable.EnumClickAction.valueOf(component.getModifier().getClickEvent().getAction().toString().toUpperCase()),
                        component.getModifier().getClickEvent().getValue());
        ChatHexColor color = null;
        if (component.getModifier().getColor() != null) {
            if (clientVersion.getMinorVersion() >= 16) {
                color = ChatHexColor.a((component.getModifier().getColor().getRed() << 16) + (component.getModifier().getColor().getGreen() << 8) + component.getModifier().getColor().getBlue());
            } else {
                color = ChatHexColor.a(component.getModifier().getColor().getLegacyColor().toString().toLowerCase());
            }
        }
        ChatHoverable hoverEvent = null;
        if (component.getModifier().getHoverEvent() != null) {
            ChatHoverable.EnumHoverAction nmsAction = ChatHoverable.EnumHoverAction.a(component.getModifier().getHoverEvent().getAction().toString().toLowerCase());
            switch (component.getModifier().getHoverEvent().getAction()) {
                case SHOW_TEXT:
                    hoverEvent = new ChatHoverable(nmsAction, adaptComponent0(component.getModifier().getHoverEvent().getValue(), clientVersion));
                    break;
                case SHOW_ENTITY:
                    hoverEvent = nmsAction.a(((ChatComponentEntity) component.getModifier().getHoverEvent().getValue()).toJson());
                    break;
                case SHOW_ITEM:
                    hoverEvent = nmsAction.a(adaptComponent0(component.getModifier().getHoverEvent().getValue(), clientVersion));
                    break;
                default:
                    break;
            }
        }
        chat.getChatModifier().setColor(color);
        chat.getChatModifier().setBold(component.getModifier().isBold());
        chat.getChatModifier().setItalic(component.getModifier().isItalic());
        chat.getChatModifier().setUnderline(component.getModifier().isUnderlined());
        chat.getChatModifier().setStrikethrough(component.getModifier().isStrikethrough());
        chat.getChatModifier().setRandom(component.getModifier().getObfuscated());
        chat.getChatModifier().setChatClickable(clickEvent);
        chat.getChatModifier().setChatHoverable(hoverEvent);
        for (IChatBaseComponent extra : component.getExtra()) {
            chat.addSibling(adaptComponent0(extra, clientVersion));
        }
        return chat;
    }

    @Override
    public int getMoveEntityId(Object packet) throws IllegalAccessException {
        return PacketPlayOutEntity_ENTITYID.getInt(packet);
    }

    @Override
    public int getTeleportEntityId(Object packet) throws IllegalAccessException {
        return PacketPlayOutEntityTeleport_ENTITYID.getInt(packet);
    }

    @Override
    public int getPlayerSpawnId(Object packet) throws IllegalAccessException {
        return PacketPlayOutNamedEntitySpawn_ENTITYID.getInt(packet);
    }

    @Override
    public int[] getDestroyEntities(Object packet) throws IllegalAccessException {
        return (int[]) PacketPlayOutEntityDestroy_ENTITIES.get(packet);
    }

    @Override
    public int getInteractEntityId(Object packet) throws IllegalAccessException {
        return PacketPlayInUseEntity_ENTITY.getInt(packet);
    }

    @Override
    public DataWatcher getLivingEntityMetadata(Object packet) {
        return null; //removed in 1.15
    }

    @Override
    public void setLivingEntityMetadata(Object packet, DataWatcher metadata) {
        //removed in 1.15
    }

    @Override
    public List<Object> getMetadataEntries(Object packet) throws IllegalAccessException {
        return (List<Object>) PacketPlayOutEntityMetadata_LIST.get(packet);
    }

    @Override
    public int getMetadataSlot(Object item) {
        return ((net.minecraft.server.v1_16_R3.DataWatcher.Item)item).a().a();
    }

    @Override
    public Object getMetadataValue(Object item) {
        return ((net.minecraft.server.v1_16_R3.DataWatcher.Item)item).b();
    }

    @Override
    public void setInteractEntityId(Object packet, int entityId) throws IllegalAccessException {
        setField(packet, PacketPlayInUseEntity_ENTITY, entityId);
    }
}