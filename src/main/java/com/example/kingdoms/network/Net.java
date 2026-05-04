package com.example.kingdoms.network;

import com.example.kingdoms.KingdomsPlugin;
import net.minecraft.util.Identifier;

public final class Net {
    private Net() {}

    public static final Identifier REQUEST_ORIGIN = new Identifier(KingdomsPlugin.MOD_ID, "request_origin");
    public static final Identifier SHOW_ORIGIN    = new Identifier(KingdomsPlugin.MOD_ID, "show_origin");
}
