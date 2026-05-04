package com.example.kingdoms.network;

import com.example.kingdoms.origin.OriginAdapter;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public final class ServerNetworking {
    private ServerNetworking() {}

    public static void register(OriginAdapter adapter) {
        ServerPlayNetworking.registerGlobalReceiver(Net.REQUEST_ORIGIN, (server, player, handler, buf, responseSender) ->
            server.execute(() -> {
                String originId = "";
                if (adapter != null) {
                    String current = adapter.getCurrentOrigin(player);
                    if (current != null) originId = current;
                }
                PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
                out.writeString(originId);
                ServerPlayNetworking.send(player, Net.SHOW_ORIGIN, out);
            }));
    }
}
