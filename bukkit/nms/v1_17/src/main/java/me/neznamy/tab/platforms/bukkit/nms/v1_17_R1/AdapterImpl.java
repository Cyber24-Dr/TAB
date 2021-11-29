package me.neznamy.tab.platforms.bukkit.nms.v1_17_R1;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.channel.Channel;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.neznamy.tab.api.ProtocolVersion;
import me.neznamy.tab.api.chat.ChatClickable.EnumClickAction;
import me.neznamy.tab.api.chat.ChatComponentEntity;
import me.neznamy.tab.api.chat.ChatHoverable.EnumHoverAction;
import me.neznamy.tab.api.chat.EnumChatFormat;
import me.neznamy.tab.api.chat.IChatBaseComponent;
import me.neznamy.tab.api.chat.TextColor;
import me.neznamy.tab.api.protocol.PacketPlayOutChat;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo.EnumGamemode;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo.PlayerInfoData;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardDisplayObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardObjective;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardObjective.EnumScoreboardHealthDisplay;
import me.neznamy.tab.api.protocol.PacketPlayOutScoreboardScore;
import me.neznamy.tab.platforms.bukkit.nms.Adapter;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcher;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherItem;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherObject;
import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherRegistry;
import me.neznamy.tab.platforms.bukkit.nms.util.ReflectionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.PlayerUpdate;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public final class AdapterImpl implements Adapter {

    private static final EnumMap<EntityType, Integer> ENTITY_IDS = new EnumMap<>(EntityType.class);

    // ServerboundInteractPacket stuff
    private static final Field INTERACT_ACTION;
    private static final Method INTERACT_ACTION_TYPE;
    private static final Class<?> INTERACT_ACTION_CLASS;
    private static final Class<?> INTERACT_ACTION_TYPE_CLASS;
    private static final Object INTERACT_ACTION_TYPE_INTERACT;
    private static final Field INTERACT_ENTITY_ID;

    // ClientboundSetPlayerTeamPacket stuff
    private static final Field SET_TEAM_PLAYERS;

    // ClientboundAddMobPacket stuff
    private static final Field SPAWN_LIVING_ENTITY_ID;
    private static final Field SPAWN_LIVING_ENTITY_UUID;
    private static final Field SPAWN_LIVING_ENTITY_TYPE;
    private static final Field SPAWN_LIVING_ENTITY_X;
    private static final Field SPAWN_LIVING_ENTITY_Y;
    private static final Field SPAWN_LIVING_ENTITY_Z;

    // ClientboundTeleportEntityPacket stuff
    private static final Field TELEPORT_ENTITY_ID;
    private static final Field TELEPORT_ENTITY_X;
    private static final Field TELEPORT_ENTITY_Y;
    private static final Field TELEPORT_ENTITY_Z;

    // ClientboundMoveEntityPacket stuff
    private static final Field MOVE_ENTITY_ID;

    // ClientboundSetEntityDataPacket stuff
    private static final Field METADATA_ITEMS;

    // Component stuff
    private static final Constructor<Style> STYLE_CONSTRUCTOR;

    static {
        ENTITY_IDS.put(EntityType.ARMOR_STAND, 1);
        ENTITY_IDS.put(EntityType.WITHER, 83);
        try {
            INTERACT_ACTION_CLASS = Class.forName("net.minecraft.network.protocol.game.ServerboundInteractPacket.Action");
            INTERACT_ACTION = ReflectionUtil.getFieldByPositionAndType(ServerboundInteractPacket.class, 0, INTERACT_ACTION_CLASS);
            INTERACT_ACTION_TYPE_CLASS = Class.forName("net.minecraft.network.protocol.game.ServerboundInteractPacket.ActionType");
            INTERACT_ACTION_TYPE = ReflectionUtil.getMethodByType(INTERACT_ACTION_CLASS, INTERACT_ACTION_TYPE_CLASS);
            INTERACT_ACTION_TYPE_INTERACT = INTERACT_ACTION_TYPE_CLASS.getEnumConstants()[0];
            INTERACT_ENTITY_ID = ReflectionUtil.getFieldByPositionAndType(ServerboundInteractPacket.class, 0, int.class);

            SET_TEAM_PLAYERS = ReflectionUtil.getFieldByPositionAndType(ClientboundSetPlayerTeamPacket.class, 0, Collection.class);

            SPAWN_LIVING_ENTITY_ID = ReflectionUtil.getFieldByPositionAndType(ClientboundAddMobPacket.class, 0, int.class);
            SPAWN_LIVING_ENTITY_UUID = ReflectionUtil.getFieldByPositionAndType(ClientboundAddMobPacket.class, 0, UUID.class);
            SPAWN_LIVING_ENTITY_TYPE = ReflectionUtil.getFieldByPositionAndType(ClientboundAddMobPacket.class, 1, int.class);
            SPAWN_LIVING_ENTITY_X = ReflectionUtil.getFieldByPositionAndType(ClientboundAddMobPacket.class, 0, double.class);
            SPAWN_LIVING_ENTITY_Y = ReflectionUtil.getFieldByPositionAndType(ClientboundAddMobPacket.class, 1, double.class);
            SPAWN_LIVING_ENTITY_Z = ReflectionUtil.getFieldByPositionAndType(ClientboundAddMobPacket.class, 2, double.class);

            TELEPORT_ENTITY_ID = ReflectionUtil.getFieldByPositionAndType(ClientboundTeleportEntityPacket.class, 0, int.class);
            TELEPORT_ENTITY_X = ReflectionUtil.getFieldByPositionAndType(ClientboundTeleportEntityPacket.class, 0, double.class);
            TELEPORT_ENTITY_Y = ReflectionUtil.getFieldByPositionAndType(ClientboundTeleportEntityPacket.class, 1, double.class);
            TELEPORT_ENTITY_Z = ReflectionUtil.getFieldByPositionAndType(ClientboundTeleportEntityPacket.class, 2, double.class);

            MOVE_ENTITY_ID = ReflectionUtil.getFieldByPositionAndType(ClientboundMoveEntityPacket.class, 0, int.class);

            METADATA_ITEMS = ReflectionUtil.getFieldByPositionAndType(ClientboundSetEntityDataPacket.class, 0, List.class);

            STYLE_CONSTRUCTOR = Style.class.getDeclaredConstructor(
                    net.minecraft.network.chat.TextColor.class,
                    Boolean.class,
                    Boolean.class,
                    Boolean.class,
                    Boolean.class,
                    Boolean.class,
                    ClickEvent.class,
                    HoverEvent.class,
                    String.class,
                    ResourceLocation.class
            );
        } catch (final Exception exception) {
            throw new RuntimeException("Could not find required NMS classes!", exception);
        }
    }

    private final DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
    private final DataWatcherRegistry dataWatcherRegistry = new DataWatcherRegistryImpl();
    private final Scoreboard emptyScoreboard = new Scoreboard();
    private final LivingEntity dummyEntity = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, server.overworld());

    private final Map<IChatBaseComponent, Component> modernComponentCache = new HashMap<>();
    private final Map<IChatBaseComponent, Component> legacyComponentCache = new HashMap<>();

    @Override
    public DataWatcherRegistry getDataWatcherRegistry() {
        return dataWatcherRegistry;
    }

    @Override
    public DataWatcher adaptDataWatcher(Object dataWatcher) {
        return adaptDataItems(((SynchedEntityData) dataWatcher).getAll());
    }

    public DataWatcher adaptDataItems(final Collection<SynchedEntityData.DataItem<?>> items) {
        final DataWatcher watcher = new DataWatcher();
        for (final SynchedEntityData.DataItem<?> item : items) {
            final DataWatcherObject object = new DataWatcherObject(item.getAccessor().getId(), item.getAccessor().getSerializer());
            watcher.setValue(object, item.getValue());
        }
        return watcher;
    }

    public List<SynchedEntityData.DataItem<?>> adaptDataItems(final DataWatcher watcher) {
        final List<SynchedEntityData.DataItem<?>> items = new ArrayList<>();
        for (final DataWatcherItem item : watcher.getItems()) {
            items.add(new SynchedEntityData.DataItem<>(
                    new EntityDataAccessor<>(item.getType().getPosition(), (EntityDataSerializer<Object>) item.getType().getClassType()),
                    item.getValue()
            ));
        }
        return items;
    }

    public SynchedEntityData adaptDataWatcher(DataWatcher dataWatcher) {
        final SynchedEntityData data = new SynchedEntityData(null);
        for (final DataWatcherItem item : dataWatcher.getItems()) {
            final EntityDataAccessor<Object> accessor = new EntityDataAccessor<>(
                    item.getType().getPosition(),
                    (EntityDataSerializer<Object>) item.getType().getClassType()
            );
            data.set(accessor, item.getValue());
        }
        return data;
    }

    @Override
    public Channel getChannel(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel;
    }

    @Override
    public int getPing(Player player) {
        return ((CraftPlayer) player).getHandle().latency;
    }

    @Override
    public Object getSkin(Player player) {
        return ((CraftPlayer) player).getHandle().getGameProfile().getProperties();
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        ((CraftPlayer) player).getHandle().connection.send((Packet<?>) packet);
    }

    @Override
    public IChatBaseComponent adaptComponent(Object component) {
        if (!(component instanceof Component)) return null; // Paper (Adventure)
        final IChatBaseComponent chat = new IChatBaseComponent(((Component) component).getContents());
        final Style style = ((Component) component).getStyle();
        chat.getModifier().setColor(adaptTextColor(style.getColor()));
        if (style.isBold()) chat.getModifier().setBold(true);
        if (style.isItalic()) chat.getModifier().setItalic(true);
        if (style.isObfuscated()) chat.getModifier().setObfuscated(true);
        if (style.isStrikethrough()) chat.getModifier().setStrikethrough(true);
        if (style.isUnderlined()) chat.getModifier().setUnderlined(true);
        if (style.getClickEvent() != null) {
            chat.getModifier().onClick(
                    EnumClickAction.valueOf(style.getClickEvent().getAction().toString().toUpperCase(Locale.ROOT)),
                    style.getClickEvent().getValue()
            );
        }
        if (style.getHoverEvent() != null) {
            final JsonObject json = style.getHoverEvent().serialize();
            final EnumHoverAction action = EnumHoverAction.valueOf(json.get("action").getAsString().toUpperCase(Locale.ROOT));
            final IChatBaseComponent value = IChatBaseComponent.deserialize(json.get("contents").getAsJsonObject().toString());
            chat.getModifier().onHover(action, value);
        }
        for (final Component sibling : ((Component) component).getSiblings()) {
            chat.addExtra(adaptComponent(sibling));
        }
        return chat;
    }

    @Override
    public boolean isPlayerInfoPacket(Object packet) {
        return packet instanceof ClientboundPlayerInfoPacket;
    }

    @Override
    public boolean isTeamPacket(Object packet) {
        return packet instanceof ClientboundSetPlayerTeamPacket;
    }

    @Override
    public boolean isDisplayObjectivePacket(Object packet) {
        return packet instanceof ClientboundSetDisplayObjectivePacket;
    }

    @Override
    public boolean isObjectivePacket(Object packet) {
        return packet instanceof ClientboundSetObjectivePacket;
    }

    @Override
    public boolean isInteractPacket(Object packet) {
        return packet instanceof ServerboundInteractPacket;
    }

    @Override
    public boolean isMovePacket(Object packet) {
        return packet instanceof ClientboundMoveEntityPacket;
    }

    @Override
    public boolean isHeadLookPacket(Object packet) {
        return packet instanceof ClientboundRotateHeadPacket;
    }

    @Override
    public boolean isTeleportPacket(Object packet) {
        return packet instanceof ClientboundTeleportEntityPacket;
    }

    @Override
    public boolean isSpawnLivingEntityPacket(Object packet) {
        return packet instanceof ClientboundAddMobPacket;
    }

    @Override
    public boolean isSpawnPlayerPacket(Object packet) {
        return packet instanceof ClientboundAddPlayerPacket;
    }

    @Override
    public boolean isDestroyPacket(Object packet) {
        return packet instanceof ClientboundRemoveEntitiesPacket;
    }

    @Override
    public boolean isMetadataPacket(Object packet) {
        return packet instanceof ClientboundSetEntityDataPacket;
    }

    @Override
    public boolean isInteractionAction(Object packet) {
        if (!(packet instanceof ServerboundInteractPacket)) return false;
        // Mojang you suck
        try {
            return INTERACT_ACTION_TYPE.invoke(INTERACT_ACTION.get(packet)) == INTERACT_ACTION_TYPE_INTERACT;
        } catch (final Exception exception) {
            throw new RuntimeException("Could not get action from interaction packet " + packet, exception);
        }
    }

    @Override
    public Collection<String> getTeamPlayers(Object teamPacket) {
        return ((ClientboundSetPlayerTeamPacket) teamPacket).getPlayers();
    }

    @Override
    public void setTeamPlayers(Object teamPacket, Collection<String> players) {
        try {
            SET_TEAM_PLAYERS.set(teamPacket, players);
        } catch (final Exception exception) {
            throw new RuntimeException("Could not set players for team packet " + teamPacket, exception);
        }
    }

    @Override
    public String getTeamName(Object teamPacket) {
        return ((ClientboundSetPlayerTeamPacket) teamPacket).getName();
    }

    @Override
    public Object createChatPacket(Object component, PacketPlayOutChat.ChatMessageType messageType) {
        return new ClientboundChatPacket((Component) component, ChatType.getForIndex((byte) messageType.ordinal()), null);
    }

    @Override
    public Object createPlayerInfoPacket(ProtocolVersion clientVersion, EnumPlayerInfoAction action,
                                         List<PlayerInfoData> players) {
        final ClientboundPlayerInfoPacket packet = new ClientboundPlayerInfoPacket(
                ClientboundPlayerInfoPacket.Action.values()[action.ordinal()],
                new ArrayList<>()
        );
        for (final PlayerInfoData data : players) {
            final GameProfile profile = new GameProfile(data.getUniqueId(), data.getName());
            if (data.getSkin() != null) {
                profile.getProperties().putAll((PropertyMap) data.getSkin());
            }
            final PlayerUpdate update = new PlayerUpdate(
                    profile,
                    data.getLatency(),
                    GameType.byId(data.getGameMode().ordinal() - 1),
                    adaptComponent(data.getDisplayName(), ProtocolVersion.UNKNOWN)
            );
            packet.getEntries().add(update);
        }
        return packet;
    }

    @Override
    public Object createPlayerListHeaderFooterPacket(ProtocolVersion clientVersion, IChatBaseComponent header, IChatBaseComponent footer) {
        return new ClientboundTabListPacket(adaptComponent(header, clientVersion), adaptComponent(footer, clientVersion));
    }

    @Override
    public Object createDisplayObjectivePacket(int slot, String objectiveName) {
        return new ClientboundSetDisplayObjectivePacket(slot, new Objective(null, objectiveName, null, null, null));
    }

    @Override
    public Object createObjectivePacket(int method, String name, Object displayName, EnumScoreboardHealthDisplay renderType) {
        return new ClientboundSetObjectivePacket(new Objective(null, name, null, (Component) displayName, ObjectiveCriteria.RenderType.byId(renderType.name().toLowerCase(Locale.ROOT))), method);
    }

    @Override
    public Object createScorePacket(PacketPlayOutScoreboardScore.Action action, String objectiveName, String player, int score) {
        return new ClientboundSetScorePacket(ServerScoreboard.Method.values()[action.ordinal()], objectiveName, player, score);
    }

    @Override
    public Object createTeamPacket(ProtocolVersion clientVersion, String name, String prefix, String suffix,
                                   String nametagVisibility, String collisionRule, EnumChatFormat color,
                                   Collection<String> players, int method, int options) {
        final PlayerTeam team = new PlayerTeam(emptyScoreboard, name);
        team.setPlayerPrefix(adaptComponent(IChatBaseComponent.optimizedComponent(prefix), clientVersion));
        team.setPlayerSuffix(adaptComponent(IChatBaseComponent.optimizedComponent(suffix), clientVersion));
        team.setColor(ChatFormatting.values()[color.ordinal()]);
        team.setNameTagVisibility(Team.Visibility.byName(nametagVisibility));
        team.setCollisionRule(Team.CollisionRule.byName(collisionRule));
        team.unpackOptions(options);
        team.getPlayers().addAll(players);
        return switch (method) {
            case 0, 2 -> ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, method == 0);
            case 1 -> ClientboundSetPlayerTeamPacket.createRemovePacket(team);
            case 3, 4 -> ClientboundSetPlayerTeamPacket.createPlayerPacket(
                    team,
                    players.stream().findFirst().orElseThrow(),
                    method == 3 ? ClientboundSetPlayerTeamPacket.Action.ADD : ClientboundSetPlayerTeamPacket.Action.REMOVE
            );
            default -> throw new IllegalStateException("Invalid method " + method + " for scoreboard team packet!");
        };
    }

    @Override
    public Object createEntityDestroyPacket(int[] entities) {
        return new ClientboundRemoveEntitiesPacket(entities);
    }

    @Override
    public Object createMetadataPacket(int entityId, DataWatcher metadata) {
        return new ClientboundSetEntityDataPacket(entityId, adaptDataWatcher(metadata), true);
    }

    @Override
    public Object createSpawnLivingEntityPacket(int entityId, UUID uuid, EntityType type, Location location, DataWatcher dataWatcher) {
        final ClientboundAddMobPacket packet = new ClientboundAddMobPacket(dummyEntity);
        try {
            SPAWN_LIVING_ENTITY_ID.setInt(packet, entityId);
            SPAWN_LIVING_ENTITY_UUID.set(packet, uuid);
            SPAWN_LIVING_ENTITY_TYPE.setInt(packet, ENTITY_IDS.get(type));
            SPAWN_LIVING_ENTITY_X.setDouble(packet, location.getX());
            SPAWN_LIVING_ENTITY_Y.setDouble(packet, location.getY());
            SPAWN_LIVING_ENTITY_Z.setDouble(packet, location.getZ());
        } catch (final Exception exception) {
            throw new RuntimeException("Could not set fields for spawn living entity packet " + packet, exception);
        }
        return null;
    }

    @Override
    public Object createTeleportPacket(int entityId, Location location) {
        final ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(dummyEntity);
        try {
            TELEPORT_ENTITY_ID.setInt(packet, entityId);
            TELEPORT_ENTITY_X.setDouble(packet, location.getX());
            TELEPORT_ENTITY_Y.setDouble(packet, location.getY());
            TELEPORT_ENTITY_Z.setDouble(packet, location.getZ());
        } catch (final Exception exception) {
            throw new RuntimeException("Could not set fields for teleport entity packet " + packet, exception);
        }
        return null;
    }

    @Override
    public PacketPlayOutPlayerInfo createPlayerInfoPacket(Object nmsPacket) {
        final ClientboundPlayerInfoPacket packet = (ClientboundPlayerInfoPacket) nmsPacket;
        final EnumPlayerInfoAction action = EnumPlayerInfoAction.valueOf(packet.getAction().toString());
        final List<PlayerInfoData> listData = new ArrayList<>();
        for (final PlayerUpdate update : packet.getEntries()) {
            final EnumGamemode gamemode = EnumGamemode.values()[update.getGameMode().ordinal() + 1];
            final IChatBaseComponent listName = update.getDisplayName() == null ? null : adaptComponent(update.getDisplayName());
            final PropertyMap map = new PropertyMap();
            map.putAll(update.getProfile().getProperties());
            listData.add(new PlayerInfoData(update.getProfile().getName(), update.getProfile().getId(), map, update.getLatency(), gamemode, listName));
        }
        return new PacketPlayOutPlayerInfo(action, listData);
    }

    @Override
    public PacketPlayOutScoreboardObjective createObjectivePacket(Object nmsPacket) {
        final ClientboundSetObjectivePacket packet = (ClientboundSetObjectivePacket) nmsPacket;
        final String displayName = adaptComponent(packet.getDisplayName()).toLegacyText();
        final EnumScoreboardHealthDisplay renderType = EnumScoreboardHealthDisplay.valueOf(packet.getRenderType().toString());
        return new PacketPlayOutScoreboardObjective(packet.getMethod(), packet.getObjectiveName(), displayName, renderType);
    }

    @Override
    public PacketPlayOutScoreboardDisplayObjective createDisplayObjectivePacket(Object nmsPacket) {
        final ClientboundSetDisplayObjectivePacket packet = (ClientboundSetDisplayObjectivePacket) nmsPacket;
        return new PacketPlayOutScoreboardDisplayObjective(packet.getSlot(), packet.getObjectiveName());
    }

    @Override
    public Component adaptComponent(IChatBaseComponent component, ProtocolVersion clientVersion) {
        if (clientVersion.getMinorVersion() >= 16) {
            if (modernComponentCache.containsKey(component)) return modernComponentCache.get(component);
            final Component result = adaptComponent0(component, clientVersion);
            if (modernComponentCache.size() > 10000) modernComponentCache.clear();
            modernComponentCache.put(component, result);
            return result;
        }
        if (legacyComponentCache.containsKey(component)) return legacyComponentCache.get(component);
        final Component result = adaptComponent0(component, clientVersion);
        if (legacyComponentCache.size() > 10000) legacyComponentCache.clear();
        legacyComponentCache.put(component, result);
        return result;
    }

    @Override
    public int getMoveEntityId(Object packet) {
        try {
            return MOVE_ENTITY_ID.getInt(packet);
        } catch (final Exception exception) {
            throw new RuntimeException("Could not get entity ID from move entity packet " + packet, exception);
        }
    }

    @Override
    public int getTeleportEntityId(Object packet) {
        return ((ClientboundTeleportEntityPacket) packet).getId();
    }

    @Override
    public int getPlayerSpawnId(Object packet) {
        return ((ClientboundAddPlayerPacket) packet).getEntityId();
    }

    @Override
    public int[] getDestroyEntities(Object packet) {
        return ((ClientboundRemoveEntitiesPacket) packet).b().toIntArray();
    }

    @Override
    public int getInteractEntityId(Object packet) {
        try {
            return INTERACT_ENTITY_ID.getInt(packet);
        } catch (final Exception exception) {
            throw new RuntimeException("Could not get entity ID from interact packet " + packet, exception);
        }
    }

    @Override
    public DataWatcher getLivingEntityMetadata(Object packet) {
        return adaptDataItems(((ClientboundSetEntityDataPacket) packet).getUnpackedData());
    }

    @Override
    public void setLivingEntityMetadata(Object packet, DataWatcher metadata) {
        try {
            METADATA_ITEMS.set(packet, adaptDataItems(metadata));
        } catch (final Exception exception) {
            throw new RuntimeException("Could not modify metadata items for packet " + packet, exception);
        }
    }

    @Override
    public List<Object> getMetadataEntries(Object packet) {
        return (List<Object>) (List<?>) ((ClientboundSetEntityDataPacket) packet).getUnpackedData();
    }

    @Override
    public int getMetadataSlot(Object item) {
        return ((SynchedEntityData.DataItem<?>) item).getAccessor().getId();
    }

    @Override
    public Object getMetadataValue(Object item) {
        return ((SynchedEntityData.DataItem<?>) item).getValue();
    }

    @Override
    public void setInteractEntityId(Object packet, int entityId) {
        try {
            INTERACT_ENTITY_ID.setInt(packet, entityId);
        } catch (final Exception exception) {
            throw new RuntimeException("Could not set entity ID for interact packet " + packet, exception);
        }
    }

    private static TextColor adaptTextColor(final net.minecraft.network.chat.TextColor color) {
        if (color == null) return null;
        final String name = color.name;
        if (name != null) {
            // legacy code
            return new TextColor(EnumChatFormat.valueOf(name.toUpperCase(Locale.US)));
        } else {
            final int rgb = color.getValue();
            return new TextColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
    }

    private static net.minecraft.network.chat.TextColor adaptTextColor(final TextColor color) {
        if (color == null) return null;
        return net.minecraft.network.chat.TextColor.fromRgb((color.getRed() << 16) + (color.getGreen() << 8) + color.getBlue());
    }

    private static Component adaptComponent0(final IChatBaseComponent component, final ProtocolVersion clientVersion) {
        if (component == null) return null;
        final TextComponent chat = new TextComponent(component.getText());
        final ClickEvent clickEvent = component.getModifier().getClickEvent() == null ? null : new ClickEvent(
                ClickEvent.Action.valueOf(component.getModifier().getClickEvent().getAction().toString().toUpperCase(Locale.ROOT)),
                component.getModifier().getClickEvent().getValue()
        );

        final net.minecraft.network.chat.TextColor color = adaptTextColor(component.getModifier().getColor());
        HoverEvent hoverEvent = null;
        if (component.getModifier().getHoverEvent() != null) {
            final HoverEvent.Action<Object> action = (HoverEvent.Action<Object>) HoverEvent.Action.getByName(component.getModifier().getHoverEvent().getAction().toString().toLowerCase(Locale.ROOT));
            hoverEvent = switch (component.getModifier().getHoverEvent().getAction()) {
                case SHOW_TEXT -> new HoverEvent(action, adaptComponent0(component.getModifier().getHoverEvent().getValue(), clientVersion));
                case SHOW_ENTITY -> action.deserialize(((ChatComponentEntity) component.getModifier().getHoverEvent().getValue()).toJson());
                case SHOW_ITEM -> action.deserializeFromLegacy(adaptComponent0(component.getModifier().getHoverEvent().getValue(), clientVersion));
            };
        }
        final Style style;
        try {
            style = STYLE_CONSTRUCTOR.newInstance(
                    color,
                    component.getModifier().getBold(),
                    component.getModifier().getItalic(),
                    component.getModifier().getUnderlined(),
                    component.getModifier().getStrikethrough(),
                    component.getModifier().getObfuscated(),
                    clickEvent,
                    hoverEvent,
                    null,
                    null
            );
        } catch (final Exception exception) {
            throw new RuntimeException("Failed to construct Style!", exception);
        }
        chat.setStyle(style);
        for (final IChatBaseComponent sibling : component.getExtra()) {
            chat.append(adaptComponent0(sibling, clientVersion));
        }
        return chat;
    }
}
