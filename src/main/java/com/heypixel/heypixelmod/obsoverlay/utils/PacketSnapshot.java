package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.network.protocol.Packet;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;

public class PacketSnapshot {
    public final Packet<?> packet;
    public final EventType type;
    public final long timestamp;

    public PacketSnapshot(Packet<?> packet, EventType type, long timestamp) {
        this.packet = packet;
        this.type = type;
        this.timestamp = timestamp;
    }
}