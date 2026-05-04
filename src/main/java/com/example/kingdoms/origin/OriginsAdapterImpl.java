package com.example.kingdoms.origin;

// ORIGINS-API: All imports below are from the Origins 1.10.0+1.20.1 jar (modCompileOnly).
// If any class name fails to resolve, verify against the actual jar contents.
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OriginsAdapterImpl implements OriginAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OriginsAdapterImpl.class);

    // ORIGINS-API: the main Origins layer used by getCurrentOrigin(); adjust if your datapack differs
    private static final Identifier DEFAULT_LAYER_ID = new Identifier("origins", "origin");
    // ORIGINS-API: standard identifier for the empty/unset origin in Origins
    private static final Identifier EMPTY_ORIGIN_ID  = new Identifier("origins", "empty");
    // ORIGINS-API: registry ID of the Orb of Origin consumable item
    private static final Identifier ORB_ITEM_ID      = new Identifier("origins", "orb_of_origin");

    @Override
    public String getCurrentOrigin(ServerPlayerEntity player) {
        requireOrigins();
        // ORIGINS-API: reads the origin assigned to the default "origins:origin" layer
        return getOriginOnLayer(player, DEFAULT_LAYER_ID.toString());
    }

    @Override
    public String getOriginOnLayer(ServerPlayerEntity player, String layerId) {
        requireOrigins();
        // ORIGINS-API: OriginLayers.getLayer(Identifier) returns null for unknown layers
        OriginLayer layer = OriginLayers.getLayer(new Identifier(layerId));
        if (layer == null) {
            LOGGER.warn("Origin layer '{}' not found in registry", layerId);
            return null;
        }
        OriginComponent component = ModComponents.ORIGIN.get(player);
        Origin origin = component.getOrigin(layer);
        return origin != null ? origin.getIdentifier().toString() : null;
    }

    @Override
    public void assignOrigin(ServerPlayerEntity player, String originId) {
        requireOrigins();
        // ORIGINS-API: sets origin on the default "origins:origin" layer
        OriginLayer layer = OriginLayers.getLayer(DEFAULT_LAYER_ID);
        if (layer == null) {
            LOGGER.warn("Default origin layer not found; cannot assign '{}'", originId);
            return;
        }
        // ORIGINS-API: OriginRegistry.get(Identifier) returns null when the origin is not registered
        Origin origin = OriginRegistry.get(new Identifier(originId));
        if (origin == null) {
            LOGGER.warn("Origin '{}' not found in registry; assignment skipped", originId);
            return;
        }
        OriginComponent component = ModComponents.ORIGIN.get(player);
        component.setOrigin(layer, origin);
        // ORIGINS-API: Cardinal Components sync — sends component data to the client
        ModComponents.ORIGIN.sync(player);
    }

    @Override
    public void assignOriginLayer(ServerPlayerEntity player, String layerId, String originId) {
        requireOrigins();
        // ORIGINS-API: assigns origin to a specific named layer (e.g., "kingdoms_of_origin:office")
        OriginLayer layer = OriginLayers.getLayer(new Identifier(layerId));
        if (layer == null) {
            LOGGER.warn("Layer '{}' not found; cannot assign origin '{}'", layerId, originId);
            return;
        }
        Origin origin = OriginRegistry.get(new Identifier(originId));
        if (origin == null) {
            LOGGER.warn("Origin '{}' not found in registry; cannot assign to layer '{}'", originId, layerId);
            return;
        }
        OriginComponent component = ModComponents.ORIGIN.get(player);
        component.setOrigin(layer, origin);
        ModComponents.ORIGIN.sync(player);
    }

    @Override
    public void clearOriginLayer(ServerPlayerEntity player, String layerId) {
        requireOrigins();
        // ORIGINS-API: clears the layer by assigning the "origins:empty" origin
        OriginLayer layer = OriginLayers.getLayer(new Identifier(layerId));
        if (layer == null) {
            LOGGER.warn("Layer '{}' not found; cannot clear", layerId);
            return;
        }
        Origin empty = OriginRegistry.get(EMPTY_ORIGIN_ID);
        if (empty == null) {
            LOGGER.warn("Empty origin (origins:empty) not found; layer '{}' not cleared", layerId);
            return;
        }
        OriginComponent component = ModComponents.ORIGIN.get(player);
        component.setOrigin(layer, empty);
        ModComponents.ORIGIN.sync(player);
    }

    @Override
    public void giveOrbOfOrigin(ServerPlayerEntity player, int amount) {
        // No Origins runtime check needed — uses the standard Minecraft item registry.
        // The item simply won't exist (returns Items.AIR) if Origins is not loaded.
        Item item = Registries.ITEM.get(ORB_ITEM_ID);
        if (item == Items.AIR) {
            LOGGER.warn("'origins:orb_of_origin' not found in item registry; cannot give orb");
            return;
        }
        player.giveItemStack(new ItemStack(item, amount));
    }

    private static void requireOrigins() {
        if (!FabricLoader.getInstance().isModLoaded("origins")) {
            throw new IllegalStateException(
                    "Origins mod is not loaded at runtime; OriginsAdapterImpl methods cannot be used");
        }
    }
}
