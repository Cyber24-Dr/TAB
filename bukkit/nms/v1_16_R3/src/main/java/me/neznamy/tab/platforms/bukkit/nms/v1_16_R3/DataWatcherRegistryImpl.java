package me.neznamy.tab.platforms.bukkit.nms.v1_16_R3;

import me.neznamy.tab.platforms.bukkit.nms.datawatcher.DataWatcherRegistry;

public final class DataWatcherRegistryImpl implements DataWatcherRegistry {

    @Override
    public Object getBoolean() {
        return net.minecraft.server.v1_16_R3.DataWatcherRegistry.i;
    }

    @Override
    public Object getByte() {
        return net.minecraft.server.v1_16_R3.DataWatcherRegistry.a;
    }

    @Override
    public Object getInteger() {
        return net.minecraft.server.v1_16_R3.DataWatcherRegistry.b;
    }

    @Override
    public Object getFloat() {
        return net.minecraft.server.v1_16_R3.DataWatcherRegistry.c;
    }

    @Override
    public Object getString() {
        return net.minecraft.server.v1_16_R3.DataWatcherRegistry.d;
    }

    @Override
    public Object getOptionalComponent() {
        return net.minecraft.server.v1_16_R3.DataWatcherRegistry.f;
    }
}
