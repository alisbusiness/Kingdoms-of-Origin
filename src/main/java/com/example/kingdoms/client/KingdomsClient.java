package com.example.kingdoms.client;

import com.example.kingdoms.network.Net;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public final class KingdomsClient implements ClientModInitializer {

    private static KeyBinding viewOriginKey;

    @Override
    public void onInitializeClient() {
        viewOriginKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kingdoms_of_origin.view_origin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.kingdoms_of_origin"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (viewOriginKey.wasPressed()) {
                if (client.player == null) continue;
                ClientPlayNetworking.send(Net.REQUEST_ORIGIN, new PacketByteBuf(Unpooled.buffer()));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Net.SHOW_ORIGIN, (client, handler, buf, sender) -> {
            String originId = buf.readString();
            client.execute(() -> client.setScreen(new OriginInfoScreen(originId)));
        });

        ClientPlayNetworking.registerGlobalReceiver(Net.OPEN_LAW_BOOK, (client, handler, buf, sender) -> {
            var stack = buf.readItemStack();
            client.execute(() -> client.setScreen(new BookScreen(new BookScreen.WrittenBookContents(stack))));
        });
    }
}
