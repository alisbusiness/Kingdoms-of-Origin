package com.example.kingdoms.perk;

import com.example.kingdoms.KingdomsPlugin;
import com.example.kingdoms.origin.OfficeService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PerkService {
    private final KingdomsPlugin plugin;
    private final OfficeService officeService;
    private final Random random = new Random();
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    private final Set<UUID> lowHealthCooldown = ConcurrentHashMap.newKeySet();
    private int tickCounter;

    public PerkService(KingdomsPlugin plugin, OfficeService officeService) {
        this.plugin = plugin;
        this.officeService = officeService;
        reloadActive();
    }

    public void registerEvents() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) onBreakBlock(sp, state);
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) onAttack(sp, entity);
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity sp) onDamaged(sp, source.getAttacker(), amount);
            return true;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) onUseItem(sp, hand);
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) onUseBlock(sp, world.getBlockState(hitResult.getBlockPos()).getBlock());
            return ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof VillagerEntity) {
                plugin.getTreasuryService().collectTax(sp, "trade", 1);
            }
            return ActionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    public void reloadActive() {
        active.clear();
        active.addAll(PerkRegistry.parseIds(officeService.getActivePerks()));
    }

    public boolean isActive(String id) {
        return active.contains(id);
    }

    public List<String> activeIds() {
        return List.copyOf(active);
    }

    public int remainingPoints(List<String> selected) {
        return PerkRegistry.validateBudget(selected).remaining();
    }

    public ApplyResult activate(ServerPlayerEntity ruler, List<String> rawIds) throws SQLException {
        List<String> ids = rawIds.stream().map(PerkRegistry::normalize).distinct().toList();
        PerkRegistry.BudgetResult budget = PerkRegistry.validateBudget(ids);
        if (!budget.valid()) return new ApplyResult(false, String.join(" ", budget.errors()));
        if (!ruler.getUuidAsString().equals(officeService.getRuler())) {
            return new ApplyResult(false, "Only the king may enact policies.");
        }

        String encoded = PerkRegistry.serialize(ids);
        officeService.setActivePerks(encoded);
        reloadActive();
        comparePromises(ruler, ids);
        MinecraftServer server = ruler.getServer();
        if (server != null) {
            server.getPlayerManager().broadcast(Text.literal(
                ruler.getName().getString() + " enacted " + ids.size() + " kingdom policies. " +
                    budget.remaining() + " Policy Points remain."
            ).formatted(Formatting.GOLD), false);
        }
        return new ApplyResult(true, "Policies enacted. Remaining Policy Points: " + budget.remaining());
    }

    private void comparePromises(ServerPlayerEntity ruler, List<String> enacted) throws SQLException {
        String officeId = plugin.getConfig().office().id();
        var electionOpt = plugin.getPersistence().elections().findMostRecentCompletedByOfficeId(officeId);
        if (electionOpt.isEmpty()) return;
        long electionId = electionOpt.get().getId();
        var candidateOpt = plugin.getPersistence().candidates().findByElectionAndPlayer(electionId, ruler.getUuidAsString());
        if (candidateOpt.isEmpty()) return;
        Set<String> enactedSet = new HashSet<>(enacted);
        for (String promise : PerkRegistry.parseIds(candidateOpt.get().getPromisedPerks())) {
            boolean honored = enactedSet.contains(promise);
            plugin.getPersistence().trust().adjust(ruler.getUuidAsString(), honored ? 2 : -8);
            plugin.getPersistence().trust().addHistory(ruler.getUuidAsString(), electionId, promise, PerkRegistry.serialize(enacted), honored);
            if (!honored && ruler.getServer() != null) {
                PerkDefinition perk = PerkRegistry.find(promise).orElse(null);
                String name = perk != null ? perk.name() : promise;
                ruler.getServer().getPlayerManager().broadcast(Text.literal(
                    "Broken promise: " + ruler.getName().getString() + " promised " + name + " but did not enact it."
                ).formatted(Formatting.RED), false);
            }
        }
    }

    private void onBreakBlock(ServerPlayerEntity player, BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            plugin.getTreasuryService().collectTax(player, "resource", 1);
        }
        if (isActive("stone_covenant") && block == Blocks.STONE && player.getY() < 32) effect(player, StatusEffects.HASTE, 240, 0);
        if (isActive("deep_levy") && isOre(block) && block.getName().getString().contains("Deepslate")) {
            effect(player, StatusEffects.HASTE, 200, 1);
            xp(player, 1);
        }
        if (isActive("minted_overtime") && isOre(block) && random.nextInt(5) == 0) xp(player, 3);
        if (isActive("iron_mandate") && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) && random.nextInt(3) == 0) {
            drop(player, block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE ? "minecraft:raw_copper" : "minecraft:raw_iron", 1);
        }
        if (isActive("charcoal_charter") && isLog(block) && random.nextInt(4) == 0) drop(player, "minecraft:charcoal", 1);
        if (isActive("timber_quota") && isLog(block) && random.nextInt(10) == 0) {
            drop(player, "minecraft:stick", 2);
            effect(player, StatusEffects.HASTE, 300, 1);
        }
        if (isActive("green_commons") && block.getName().getString().contains("Leaves") && random.nextInt(5) == 0) drop(player, "minecraft:apple", 1);
        if (isActive("breadline") && isCrop(block)) player.getHungerManager().add(1, 0.2f);
        if (isActive("canal_act") && player.isWet()) effect(player, StatusEffects.DOLPHINS_GRACE, 160, 0);
        if (isActive("quarry_whistle") && (block == Blocks.COAL_ORE || block == Blocks.REDSTONE_ORE || block == Blocks.LAPIS_ORE)) {
            player.getWorld().getPlayers().stream().filter(p -> p.distanceTo(player) < 12).forEach(p -> effect((ServerPlayerEntity) p, StatusEffects.HASTE, 160, 0));
        }
        if (isActive("blacksmith_contract") && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) {
            ItemStack tool = player.getMainHandStack();
            if (tool.isDamaged()) tool.setDamage(Math.max(0, tool.getDamage() - 1));
        }
    }

    private void onAttack(ServerPlayerEntity player, Entity target) {
        if (target instanceof HostileEntity) {
            if (isActive("peoples_blade")) effect(player, StatusEffects.STRENGTH, 200, 0);
            if (isActive("monster_bounty") && !isActive("draft_notice")) xp(player, 1);
            if (isActive("war_bonds")) xp(player, 1);
        }
        if (isActive("private_armory") && isKing(player)) effect(player, StatusEffects.STRENGTH, 80, 1);
    }

    private void onDamaged(ServerPlayerEntity player, Entity attacker, float amount) {
        if (isActive("shield_wall_decree") && player.isBlocking()) effect(player, StatusEffects.RESISTANCE, 120, 0);
        if (isActive("last_stand_clause") && player.getHealth() <= 10.0f && lowHealthCooldown.add(player.getUuid())) {
            effect(player, StatusEffects.RESISTANCE, 160, 1);
            player.getServer().execute(() -> {});
        }
        if (isActive("border_watch") && attacker != null && attacker.getType().getName().getString().contains("Arrow")) effect(player, StatusEffects.SPEED, 160, 0);
        if (isActive("stone_shelters")) effect(player, StatusEffects.RESISTANCE, 160, 0);
        if (isActive("powder_inspection")) {
            effect(player, StatusEffects.FIRE_RESISTANCE, 160, 0);
            effect(player, StatusEffects.RESISTANCE, 160, 0);
        }
    }

    private void onUseItem(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (isActive("siege_rations") && stack.isFood()) {
            effect(player, StatusEffects.STRENGTH, 200, 0);
            player.getHungerManager().add(-1, 0);
        }
        if (isActive("festival_law") && (stack.isOf(Items.CAKE) || stack.isOf(Items.COOKIE) || stack.isOf(Items.PUMPKIN_PIE))) {
            effect(player, StatusEffects.JUMP_BOOST, 240, 0);
        }
        if (isActive("fisher_auction") && stack.isOf(Items.FISHING_ROD)) effect(player, StatusEffects.LUCK, 240, 0);
    }

    private void onUseBlock(ServerPlayerEntity player, Block block) {
        if (isActive("safe_lodging") && block instanceof net.minecraft.block.BedBlock) {
            player.removeStatusEffect(StatusEffects.POISON);
            player.removeStatusEffect(StatusEffects.HUNGER);
        }
        if (isActive("market_day") && block == Blocks.BELL) effect(player, StatusEffects.REGENERATION, 160, 0);
    }

    private void onTick(MinecraftServer server) {
        if (++tickCounter % 100 != 0) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean king = isKing(player);
            if (isActive("velvet_gaol")) effect(player, king ? StatusEffects.RESISTANCE : StatusEffects.MINING_FATIGUE, 140, king ? 1 : 0);
            if (isActive("royal_physician")) {
                if (king) effect(player, StatusEffects.REGENERATION, 140, 1);
                else if (player.isSleeping()) player.wakeUp();
            }
            if (isActive("private_armory") && !king) effect(player, StatusEffects.WEAKNESS, 140, 0);
            if (isActive("silken_roads")) effect(player, king ? StatusEffects.SPEED : StatusEffects.SLOWNESS, 140, king ? 1 : 0);
            if (isActive("dragon_seal") && king) effect(player, StatusEffects.FIRE_RESISTANCE, 140, 0);
            if (isActive("ration_cards")) effect(player, StatusEffects.HUNGER, 140, 0);
            if (isActive("night_school") && !player.getWorld().isDay()) effect(player, StatusEffects.NIGHT_VISION, 220, 0);
            if (isActive("blood_standard") || isActive("bread_and_circuses")) {
                player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(16.0);
            } else {
                player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            }
        }
        if (tickCounter % 1200 == 0) lowHealthCooldown.clear();
    }

    private boolean isKing(ServerPlayerEntity player) {
        return player.getUuidAsString().equals(officeService.getRuler());
    }

    private boolean isOre(Block block) {
        String name = block.getName().getString();
        return name.contains("Ore");
    }

    private boolean isLog(Block block) {
        String name = block.getName().getString();
        return name.contains("Log") || name.contains("Stem");
    }

    private boolean isCrop(Block block) {
        return block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.BEETROOTS;
    }

    private void effect(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect effect, int ticks, int amplifier) {
        player.addStatusEffect(new StatusEffectInstance(effect, ticks, amplifier, true, false, true));
    }

    private void xp(ServerPlayerEntity player, int amount) {
        int adjusted = amount;
        if (isActive("crown_tax")) adjusted = isKing(player) ? Math.max(1, Math.round(amount * 1.4f)) : Math.max(0, Math.round(amount * 0.85f));
        if (isActive("austerity_act") && !isKing(player)) adjusted = Math.max(0, Math.round(adjusted * 0.9f));
        plugin.getTreasuryService().collectTax(player, "xp", adjusted);
        player.addExperience(adjusted);
    }

    private void drop(ServerPlayerEntity player, String itemId, int count) {
        var item = Registries.ITEM.get(new Identifier(itemId));
        if (item == Items.AIR) return;
        ItemEntity entity = new ItemEntity(player.getWorld(), player.getX(), player.getY(), player.getZ(), new ItemStack(item, count));
        player.getWorld().spawnEntity(entity);
    }

    public record ApplyResult(boolean success, String message) {}
}
