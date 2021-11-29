package me.neznamy.tab.platforms.bukkit.nms.v1_17_R1;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.channel.Channel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import me.neznamy.tab.api.ProtocolVersion;
import me.neznamy.tab.api.chat.ChatClickable.EnumClickAction;
import me.neznamy.tab.api.chat.ChatHoverable.EnumHoverAction;
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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;

public final class AdapterImpl implements Adapter {

    private static final Stream<Field> SPAWN_LIVING_ENTITY_PACKET_FIELDS = Arrays.stream(ClientboundAddMobPacket.class.getDeclaredFields())
            .peek(field -> field.setAccessible(true));

    private final DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
    private final DataWatcherRegistry dataWatcherRegistry = new DataWatcherRegistryImpl();
    private final Scoreboard emptyScoreboard = new Scoreboard();
    private final LivingEntity dummyEntity = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, server.overworld());

    @Override
    public DataWatcherRegistry getDataWatcherRegistry() {
        return dataWatcherRegistry;
    }

    @Override
    public DataWatcher adaptDataWatcher(Object dataWatcher) {
        return null;
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
        return packet instanceof ServerboundInteractPacket && ((ServerboundInteractPacket) packet).getActionType() == ServerboundInteractPacket.ActionType.INTERACT;
    }

    @Override
    public Collection<String> getTeamPlayers(Object teamPacket) {
        return ((ClientboundSetPlayerTeamPacket) teamPacket).getPlayers();
    }

    @Override
    public void setTeamPlayers(Object teamPacket, Collection<String> players) {
        final Class<?> packetClass = teamPacket.getClass();
        try {
            final Field declaredField = packetClass.getDeclaredField("players");
            declaredField.setAccessible(true);
            declaredField.set(teamPacket, players);
        } catch (final Exception exception) {
            // ignored
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
    public Object createPlayerInfoPacket(ProtocolVersion clientVersion, PacketPlayOutPlayerInfo.EnumPlayerInfoAction action,
                                         List<PacketPlayOutPlayerInfo.PlayerInfoData> players) {
        final ClientboundPlayerInfoPacket packet = new ClientboundPlayerInfoPacket(
                ClientboundPlayerInfoPacket.Action.values()[action.ordinal()],
                new ArrayList<>()
        );
        for (final PacketPlayOutPlayerInfo.PlayerInfoData data : players) {
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
    public Object createObjectivePacket(int method, String name, Object displayName, PacketPlayOutScoreboardObjective.EnumScoreboardHealthDisplay renderType) {
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
        final Class<?> packetClass = ClientboundAddMobPacket.class;
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
    public Component adaptComponent(IChatBaseComponent component, ProtocolVersion clientVersion) {
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
}
