package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public class PacketEvent implements Event {
    private final Object packet;
    private boolean cancelled;

    public PacketEvent(Object packet) {
        this.packet = packet;
    }

    public Object getPacket() {
        return packet;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancelEvent() {
        this.cancelled = true;
    }
}