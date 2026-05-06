package com.example.kingdoms.perk;

import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PerkRegistry {
    public static final int POLICY_POINTS = 20;

    private static final Map<String, PerkDefinition> PERKS = new LinkedHashMap<>();

    static {
        labor();
        military();
        economic();
        social();
        corruption();
    }

    private PerkRegistry() {}

    public static List<PerkDefinition> all() {
        return List.copyOf(PERKS.values());
    }

    public static List<String> legacyOriginPowerIds() {
        return List.of(
            "acrobat", "apple_picker", "aqua_affinity", "arachnid_bane", "blessed_realm",
            "bountiful_harvest_i", "butchers_blade", "censorship", "creeper_resistance", "curfew",
            "current_rider", "deep_dark_resistance", "deep_sea_diver", "diamond_luck", "disarmed_populace",
            "dolphins_grace", "early_bird", "enchanters_wisdom", "enderman_slayer", "fishers_diet",
            "forced_labor_i", "forced_labor_ii", "fragile_armor", "frail_subjects", "green_thumb",
            "guardian_slayer", "heavy_taxes_i", "heavy_taxes_ii", "hero_of_the_realm_i", "hero_of_the_realm_ii",
            "honey_lover", "iron_skin", "lava_immunity", "lumberjack", "master_angler_i",
            "miners_haste_i", "miners_haste_ii", "miners_haste_iii", "mountaineer", "mushroom_forager",
            "night_owl", "obsidian_breaker", "ocean_treasure", "oppression_i", "oppression_ii",
            "ore_doubling", "royal_guard_i", "royal_guard_ii", "royal_vitality_i", "royal_vitality_ii",
            "seas_bounty_i", "shepherd", "slowed_masses", "spelunkers_glow", "squids_ink",
            "starvation_diet_i", "starvation_diet_ii", "stout_heart", "swift_messenger", "swift_striker",
            "tractor", "unbreaking_tools", "undead_slayer", "unlucky_realm", "vegans_grace"
        );
    }

    public static List<PerkDefinition> byCategory(PerkCategory category) {
        return PERKS.values().stream()
            .filter(perk -> perk.category() == category)
            .sorted(Comparator.comparing(PerkDefinition::name))
            .toList();
    }

    public static Optional<PerkDefinition> find(String rawId) {
        String id = normalize(rawId);
        return Optional.ofNullable(PERKS.get(id));
    }

    public static List<String> parseIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> ids = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
            if (!token.isBlank()) ids.add(normalize(token));
        }
        return ids;
    }

    public static String serialize(List<String> ids) {
        return String.join(",", ids.stream().map(PerkRegistry::normalize).distinct().toList());
    }

    public static String normalize(String rawId) {
        String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
        int colon = id.indexOf(':');
        if (colon >= 0) id = id.substring(colon + 1);
        if (id.startsWith("perks/")) id = id.substring("perks/".length());
        return id;
    }

    public static BudgetResult validateBudget(List<String> ids) {
        int spent = 0;
        Map<PerkCategory, String> seen = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (String raw : ids) {
            PerkDefinition perk = PERKS.get(normalize(raw));
            if (perk == null) {
                errors.add("Unknown perk: " + raw);
                continue;
            }
            if (seen.containsKey(perk.category())) {
                errors.add("Only one " + perk.category().displayName() + " perk may be active.");
            } else {
                seen.put(perk.category(), perk.id());
            }
            spent += perk.cost();
        }
        int remaining = POLICY_POINTS - Math.max(0, spent);
        if (spent > POLICY_POINTS) errors.add("Policy budget exceeded by " + (spent - POLICY_POINTS) + " points.");
        return new BudgetResult(errors.isEmpty(), spent, remaining, List.copyOf(errors));
    }

    private static void add(String id, String name, PerkCategory category, PerkKind kind, net.minecraft.item.Item icon, String description) {
        PERKS.put(id, new PerkDefinition(id, name, category, kind, icon, description, description.contains("Wartime")));
    }

    private static void labor() {
        add("iron_mandate", "Iron Mandate", PerkCategory.LABOR, PerkKind.MODERATE, Items.IRON_PICKAXE, "By royal order, every mined iron or copper ore has a one-in-three chance to drop an extra raw ore.");
        add("stone_covenant", "Stone Covenant", PerkCategory.LABOR, PerkKind.MINOR, Items.STONE, "The crown blesses public works: breaking stone below Y=32 grants Haste I for 12 seconds.");
        add("breadline", "Breadline", PerkCategory.LABOR, PerkKind.MINOR, Items.WHEAT, "The granaries open: harvesting fully grown wheat, carrots, potatoes, or beetroot restores 1 hunger once every 20 seconds.");
        add("charcoal_charter", "Charcoal Charter", PerkCategory.LABOR, PerkKind.MINOR, Items.CHARCOAL, "The forests serve the realm: chopping logs has a one-in-four chance to drop charcoal.");
        add("deep_levy", "Deep Levy", PerkCategory.LABOR, PerkKind.STRONG, Items.DIAMOND_PICKAXE, "The mines are mobilized: breaking deepslate ores grants Haste II for 10 seconds and 1 experience point.");
        add("green_commons", "Green Commons", PerkCategory.LABOR, PerkKind.MODERATE, Items.OAK_SAPLING, "The commons are protected: breaking leaves has a one-in-five chance to return a matching sapling or apple.");
        add("canal_act", "Canal Act", PerkCategory.LABOR, PerkKind.MODERATE, Items.WATER_BUCKET, "State canals speed labor: mining or harvesting while wet grants Dolphin's Grace for 8 seconds.");
        add("granary_audit", "Granary Audit", PerkCategory.LABOR, PerkKind.DEBUFF, Items.POISONOUS_POTATO, "The crown audits every harvest: crop harvesting sometimes withholds the bonus yield and refunds 3 Policy Points.");
        add("timber_quota", "Timber Quota", PerkCategory.LABOR, PerkKind.STRONG, Items.DIAMOND_AXE, "Royal quotas bite: every tenth log chopped drops two extra sticks and grants Haste II for 15 seconds.");
        add("quarry_whistle", "Quarry Whistle", PerkCategory.LABOR, PerkKind.MODERATE, Items.BELL, "When a miner breaks coal, redstone, or lapis ore, nearby subjects gain Haste I for 8 seconds.");
    }

    private static void military() {
        add("peoples_blade", "The People's Blade", PerkCategory.MILITARY, PerkKind.MODERATE, Items.IRON_SWORD, "Militia law is declared: after killing a hostile mob, gain Strength I for 10 seconds.");
        add("shield_wall_decree", "Shield Wall Decree", PerkCategory.MILITARY, PerkKind.MINOR, Items.SHIELD, "The guard holds formation: blocking damage grants Resistance I for 6 seconds.");
        add("wolf_tax", "Wolf Tax", PerkCategory.MILITARY, PerkKind.MODERATE, Items.BONE, "Kennels are funded: killing a skeleton has a one-in-four chance to grant Speed I for 12 seconds.");
        add("last_stand_clause", "Last Stand Clause", PerkCategory.MILITARY, PerkKind.STRONG, Items.TOTEM_OF_UNDYING, "No subject falls quietly: dropping below 5 hearts grants Resistance II for 8 seconds once per minute.");
        add("monster_bounty", "Monster Bounty", PerkCategory.MILITARY, PerkKind.MINOR, Items.ROTTEN_FLESH, "The treasury pays for safety: killing hostile mobs grants 1 extra experience.");
        add("siege_rations", "Siege Rations", PerkCategory.MILITARY, PerkKind.MODERATE, Items.COOKED_BEEF, "Wartime rations begin: everyone gains Strength I after eating but loses 1 hunger immediately.");
        add("blood_standard", "Blood Standard", PerkCategory.MILITARY, PerkKind.STRONG, Items.RED_BANNER, "Wartime banners rise: everyone gains Strength I while below half health but has 2 fewer max hearts.");
        add("powder_inspection", "Powder Inspection", PerkCategory.MILITARY, PerkKind.MODERATE, Items.GUNPOWDER, "Explosives are regulated: creeper and TNT damage grants Fire Resistance and Resistance for 8 seconds.");
        add("draft_notice", "Draft Notice", PerkCategory.MILITARY, PerkKind.DEBUFF, Items.IRON_CHESTPLATE, "The draft burdens all subjects: combat kills no longer trigger bounty XP and refund 3 Policy Points.");
        add("border_watch", "Border Watch", PerkCategory.MILITARY, PerkKind.MINOR, Items.SPYGLASS, "Watch posts report danger: being hit by a projectile grants Speed I for 8 seconds.");
    }

    private static void economic() {
        add("guild_tithe", "Guild Tithe", PerkCategory.ECONOMIC, PerkKind.MODERATE, Items.EMERALD, "Guilds pay in kind: earning experience has a one-in-four chance to grant 1 extra experience.");
        add("minted_overtime", "Minted Overtime", PerkCategory.ECONOMIC, PerkKind.STRONG, Items.GOLD_INGOT, "The mint rewards long labor: every fifth ore broken grants 3 experience.");
        add("market_day", "Market Day", PerkCategory.ECONOMIC, PerkKind.MINOR, Items.EMERALD_BLOCK, "Market stalls open: trading with villagers grants Regeneration I for 8 seconds.");
        add("salvage_rights", "Salvage Rights", PerkCategory.ECONOMIC, PerkKind.MODERATE, Items.ANVIL, "Nothing is wasted: killing armored mobs has a chance to drop an iron nugget.");
        add("enchanters_license", "Enchanter's License", PerkCategory.ECONOMIC, PerkKind.STRONG, Items.ENCHANTING_TABLE, "Licensed scholars prosper: collecting an experience orb while near an enchanting table grants 2 bonus XP.");
        add("public_ledger", "Public Ledger", PerkCategory.ECONOMIC, PerkKind.MINOR, Items.BOOK, "The ledgers are open: every new active policy announces its cost and remaining Policy Points.");
        add("austerity_act", "Austerity Act", PerkCategory.ECONOMIC, PerkKind.DEBUFF, Items.CHAIN, "Austerity is imposed: subjects lose 10 percent of earned experience and refund 3 Policy Points.");
        add("blacksmith_contract", "Blacksmith Contract", PerkCategory.ECONOMIC, PerkKind.MODERATE, Items.SMITHING_TABLE, "The forges work for the realm: mining iron while holding a damaged tool repairs it by 1 durability.");
        add("fisher_auction", "Fisher Auction", PerkCategory.ECONOMIC, PerkKind.MINOR, Items.FISHING_ROD, "Dock auctions are sanctioned: catching fish grants Luck I for 12 seconds.");
        add("war_bonds", "War Bonds", PerkCategory.ECONOMIC, PerkKind.STRONG, Items.GOLDEN_SWORD, "Wartime bonds sell fast: everyone gains 25 percent bonus XP from combat but takes 10 percent more damage.");
    }

    private static void social() {
        add("open_roads", "Open Roads Act", PerkCategory.SOCIAL, PerkKind.MODERATE, Items.LEATHER_BOOTS, "The highways are cleared: sprinting on roads, stone, or planks grants Speed I.");
        add("public_clinic", "Public Clinic", PerkCategory.SOCIAL, PerkKind.STRONG, Items.GOLDEN_APPLE, "The clinics open: sleeping or respawning grants Regeneration II for 15 seconds.");
        add("festival_law", "Festival Law", PerkCategory.SOCIAL, PerkKind.MINOR, Items.CAKE, "The realm celebrates: eating sweet food grants Jump Boost I for 12 seconds.");
        add("safe_lodging", "Safe Lodging", PerkCategory.SOCIAL, PerkKind.MINOR, Items.RED_BED, "Inns receive funding: entering a bed clears Poison and Hunger.");
        add("courier_network", "Courier Network", PerkCategory.SOCIAL, PerkKind.MODERATE, Items.MAP, "Royal couriers ride: after traveling 300 blocks, gain Speed II for 20 seconds.");
        add("night_school", "Night School", PerkCategory.SOCIAL, PerkKind.MODERATE, Items.CLOCK, "Night schools convene: after sunset, subjects gain Night Vision while outdoors.");
        add("bread_and_circuses", "Bread and Circuses", PerkCategory.SOCIAL, PerkKind.STRONG, Items.FIREWORK_ROCKET, "Wartime pageantry begins: everyone gains Speed II, but max health is reduced by 2 hearts.");
        add("ration_cards", "Ration Cards", PerkCategory.SOCIAL, PerkKind.DEBUFF, Items.PAPER, "Rations are tightened: natural regeneration is slowed by Hunger I and refund 3 Policy Points.");
        add("civil_service", "Civil Service", PerkCategory.SOCIAL, PerkKind.MINOR, Items.WRITABLE_BOOK, "Helpful clerks reduce friction: opening the policy viewer shows the king's trust and current promises.");
        add("stone_shelters", "Stone Shelters", PerkCategory.SOCIAL, PerkKind.MODERATE, Items.COBBLESTONE, "Public shelters stand ready: taking fall damage grants Resistance I for 8 seconds.");
    }

    private static void corruption() {
        add("crown_tax", "Crown Tax", PerkCategory.CORRUPTION, PerkKind.CORRUPTION, Items.GOLDEN_HELMET, "The king claims first profit: the king gains 40 percent bonus XP while subjects lose 15 percent XP.");
        add("velvet_gaol", "Velvet Gaol", PerkCategory.CORRUPTION, PerkKind.CORRUPTION, Items.IRON_BARS, "The palace is secure: the king receives Resistance II while subjects suffer Mining Fatigue I.");
        add("royal_physician", "Royal Physician", PerkCategory.CORRUPTION, PerkKind.CORRUPTION, Items.GLISTERING_MELON_SLICE, "Court physicians serve one patient: the king receives Regeneration II while subjects cannot skip night.");
        add("private_armory", "Private Armory", PerkCategory.CORRUPTION, PerkKind.CORRUPTION, Items.NETHERITE_SWORD, "The royal armory closes to the public: the king gains Strength II while subjects suffer Weakness I.");
        add("silken_roads", "Silken Roads", PerkCategory.CORRUPTION, PerkKind.CORRUPTION, Items.GOLDEN_BOOTS, "The roads bend toward the palace: the king gains Speed II while subjects suffer Slowness I.");
        add("dragon_seal", "Dragon Seal", PerkCategory.CORRUPTION, PerkKind.CORRUPTION, Items.DRAGON_BREATH, "Forbidden seals protect the throne: the king gains Fire Resistance and subjects take 10 percent more damage.");
    }

    public record BudgetResult(boolean valid, int spent, int remaining, List<String> errors) {}
}
