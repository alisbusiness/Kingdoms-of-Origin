# Kingdoms of Origin

Welcome to **Kingdoms of Origin**! This is a Fabric 1.20.x server-side mod that transforms your multiplayer experience by introducing a player-elected **King** office integrated directly with the **Origins** mod.

Have you ever wanted to rule your server with divine powers? Now you can! Players can nominate themselves, run campaigns, and vote in server-wide elections. The victor is crowned King and granted the **King Zeus** origin (or any custom origin), unlocking unique, god-like powers for the duration of their reign.

---

## 👑 What This Mod Adds

### 1. The Election Cycle
The core of the mod is the democratic process. Elections happen automatically based on configurable term limits (default: 7 real-world days).
- **Interim King:** When a server first starts, the first player to join is granted the King origin as an interim ruler. They hold this power until they initiate the first election.
- **Nomination Phase:** Players use `/kingdom run [slogan]` to declare their candidacy.
- **Campaign Phase:** A period for candidates to rally support and make promises to the server.
- **Voting Phase:** Players use `/kingdom vote` to securely and anonymously cast their ballot via an interactive GUI.
- **Coronation:** Once voting concludes, the winner is automatically crowned!

### 2. The Power of The King
When a player is crowned King, they are bestowed with **The King** origin. This grants them incredible abilities to enforce their rule, but balances it with severe weaknesses:
- 👑 **Royal Decree (Primary Active):** Rally your subjects! Issue a command that grants Strength II and Resistance I to all players within 15 blocks for 15 seconds (60s Cooldown).
- ⚡ **Wrath of the King (Secondary Active):** Call down a devastating bolt of lightning on whatever block or entity you are looking at up to 30 blocks away (30s Cooldown).
- 💥 **Iron Fist (Passive):** Strike with the weight of your realm (+3 Attack Damage).
- 🛡️ **Crown's Resilience (Passive):** Fortified by the burden of the crown (+10 Max Health, +4 Armor, +2 Armor Toughness).
- 🏃 **Sovereign's March (Passive):** Never slow to act (+15% Movement Speed).
- 🪽 **Divine Flight (Passive):** Take to the skies to survey your domain using creative flight.
- ✨ **Royal Radiance (Drawback):** The crown marks you. All can see where you stand (Permanent Glowing effect).
- 👁️ **Undeniable Presence (Drawback):** A king cannot hide from his people or enemies (Immunity to Invisibility potions).
- 🎯 **Mark of the Crown (Drawback):** Enemies are drawn to power (Reduced Knockback Resistance).
- 🤲 **Soft Hands (Drawback):** Accustomed to ruling, not manual labor (-30% Mining Speed).
- 🏹 **Prime Target (Drawback):** An undeniable presence makes you an easy mark (+50% Projectile Damage Taken).
- 🌑 **Creature of Light (Severe Drawback):** Your sovereign power is tied to the light. In darkness, your max health is halved, you take +50% more damage, and suffer from crippling Weakness, Slowness, and Mining Fatigue.

### 3. Seamless Origin Transitions
The mod integrates perfectly with your existing Origins.
- **Layer Mode (Recommended):** The King origin is added as a secondary "layer" on top of your primary origin. You keep your base powers, and gain the King's powers. When your term ends, the King layer is simply removed.
- **Replace Mode:** The King origin completely replaces your current origin. When your term is up, your original origin is safely restored!
- **Orbs of Origin:** Configurable options allow the server to grant an Orb of Origin upon peaceful transfer of power or abdication.

### 4. Rich Server Integration
- **GUI Menus:** Beautiful, easy-to-use in-game menus (`/kingdom menu`) to view candidates, vote, and see election status.
- **Live Announcements:** Boss bars and scoreboard updates keep everyone informed about ongoing elections.
- **Map Support:** Integrates with BlueMap and Dynmap to highlight the Capital and the current ruler's domain.

---

## 🛠️ Server Administrator Guide

### Requirements
| Requirement | Version |
|---|---|
| Java | 21+ |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.15.0+ |
| Fabric API | 0.92.2+1.20.1 (or compatible) |
| Origins mod | 1.10.x for 1.20.x (**optional** — origin features disabled without it) |
| BlueMap | 3.x+ (**optional** — map marker integration) |
| Dynmap | 3.4+ (**optional** — map marker integration) |

### Installation

1. Drop `kingdoms-of-origin-<version>.jar` into your server's `mods/` folder.
2. Drop the `origins-fabric-*.jar` into `mods/` for origin assignment features.
3. (Optional) Install BlueMap or Dynmap for map markers. Set `map.provider` in config.
4. (Optional) Customize the origins by editing the `data/kingdoms_of_origin/` files in the mod source and rebuilding.
5. Start the server — `config/kingdoms_of_origin/config.yml` is generated automatically.
6. The Origins are automatically loaded from within the mod itself!

### Built-in Datapack
The origin and power definitions are now bundled directly inside the mod jar!
You do not need to install an external datapack.

To change the King origin without touching Java code:
- Edit `src/main/resources/data/kingdoms_of_origin/origins/king.json` to change powers or appearance and rebuild the mod.
- Or create a new origin file and update `origin_mode.king_origin_id` in `config.yml` to point to it.

---

## ⚙️ Configuration (`config.yml` Reference)

| Key | Type | Default | Description |
|---|---|---|---|
| `office.id` | string | `king` | Internal DB key for the office. Changing loses existing state. |
| `office.display_name` | string | `King` | Name shown in chat and the GUI. |
| `office.term_days` | int | `7` | Real-world days per term before a new election triggers. |
| `office.election_enabled` | bool | `true` | Disable to manage office entirely via admin commands. |
| `office.nomination_days` | int | `2` | Days the nomination window stays open. |
| `office.campaign_days` | int | `2` | Days between nominations closing and voting opening. |
| `office.voting_days` | int | `1` | Days the voting window stays open. |
| `origin_mode.type` | string | `layer` | `layer` = separate Origins layer; `replace` = replaces primary origin. |
| `origin_mode.king_origin_id` | string | `kingdoms_of_origin:king` | Origins ID of the king origin. Change freely — no Java edit needed. |
| `origin_mode.king_layer_id` | string | `kingdoms_of_origin:office` | Layer ID used in layer mode. Must match the datapack layer file. |
| `origin_restore.restore_previous_origin_on_term_end` | bool | `true` | Restore the player's pre-office origin when their term ends. |
| `origin_restore.clear_king_origin_on_term_end` | bool | `true` | Remove the king origin when the term ends. |
| `transition.give_orb_on_transfer` | bool | `true` | Give an Orb of Origin to the incoming ruler on election transfer. |
| `transition.give_orb_on_abdication` | bool | `true` | Give an Orb of Origin when the ruler abdicates voluntarily. |
| `transition.give_orb_on_forced_removal` | bool | `false` | Give an Orb of Origin on admin forced removal. |
| `voting.system` | string | `plurality` | Vote tallying system (only `plurality` implemented). |
| `voting.minimum_playtime_minutes` | int | `60` | Minutes of playtime required to cast a vote. |
| `voting.anonymous_votes` | bool | `true` | Hide individual votes; only reveal the final tally. |
| `ui.use_scoreboard_announcements` | bool | `true` | Show announcements on the scoreboard sidebar. |
| `ui.use_bossbar_during_election` | bool | `true` | Display a boss bar during the voting window. |
| `ui.send_chat_broadcasts` | bool | `true` | Broadcast major events to all players in chat. |
| `map.provider` | string | `bluemap` | Map plugin: `bluemap`, `dynmap`, or `none`. |
| `map.show_capital_marker` | bool | `true` | Place a Capital POI marker on the map. |
| `debug.log_origin_transfers` | bool | `true` | Log every origin assignment to the console. |
| `debug.log_gui_actions` | bool | `false` | Log GUI open/close events (verbose). |

---

## 📜 Command Reference

### Player Commands
These commands are available to all players:

| Command | Description |
|---|---|
| `/kingdom status` | Show current ruler, term end, and election phase. |
| `/kingdom start-election` | Start a new election cycle (King only). |
| `/kingdom ruler` | Show the current ruler's name, term details, and active perks. |
| `/kingdom candidates` | List candidates in the current election. |
| `/kingdom run [slogan]` | Register yourself as a candidate (nomination phase only). |
| `/kingdom promise <perks...>` | Declare your campaign promises as a candidate (comma-separated IDs). |
| `/kingdom setperks <perks...>` | Set the active policies for the server (King only, comma-separated IDs). |
| `/kingdom perks` | Open a read-only GUI showing the currently active kingdom policies. |
| `/kingdom perk <id>` | Inspect a specific policy by ID. |
| `/kingdom trust` | View the current ruler's trust score and recent promise history. |
| `/kingdom vote` | Open the voting GUI (voting phase only). |
| `/kingdom menu` | Open the main kingdom GUI to navigate all features. |
| `/kingdom help` | Show help text and available commands. |

### Admin Commands
All admin commands require operator level 2 (`isOp`).

| Command | Description |
|---|---|
| `/kingdom admin start-election` | Force-start an election for the current office. |
| `/kingdom admin end-election` | Force-advance the current election to the next phase. |
| `/kingdom admin set-ruler <player>` | Directly appoint a player as ruler. |
| `/kingdom admin remove-ruler` | Remove the current ruler without transferring office. |
| `/kingdom admin force-transfer <player>` | Transfer office from current ruler to the target player. |
| `/kingdom admin give-orb <player>` | Give an Orb of Origin to a player. |
| `/kingdom admin set-phase <phase>` | Manually set the election phase (`NOMINATION`, `VOTING`, etc.). |
| `/kingdom admin debug-sync` | Validate and re-sync the current ruler's king origin. |
| `/kingdom admin reload` | Placeholder — config is currently read at startup only; restart to apply changes. |

---


### 3. Royal Perks & Policies
The King can enact server-side policies that feel like government decrees instead of flat stat modifiers. Policies trigger from gameplay events such as mining ore, harvesting crops, fighting mobs, sleeping, eating, taking damage, or moving through the world.

Policies are selected through a chest GUI from the ruler panel or with `/kingdom setperks <ids...>`. Any player can view active policies with `/kingdom perks`, and candidates can promise policies before an election with `/kingdom promise <ids...>`.

#### Policy Budget
- Each term starts with **20 Policy Points**.
- Minor policies cost **2** points.
- Moderate policies cost **4** points.
- Strong policies cost **6** points.
- Debuff policies refund **3** points.
- Corruption policies cost **0** points.
- Only **one policy per category** may be active at a time.
- The five categories are **Labor**, **Military**, **Economic**, **Social**, and **Corruption**.

#### Promise vs. Reality
Candidates can declare policy promises before an election. When the winning ruler enacts policies, the server compares the active policies against their promises:

- Honored promises increase the ruler's trust score.
- Broken promises reduce the ruler's trust score.
- Each broken promise is broadcast publicly.
- `/kingdom trust` shows the current ruler's score and recent promise history.
- `/kingdom ruler` also displays the current ruler's trust score.

#### Policy Categories

##### Labor
- **Iron Mandate** (`iron_mandate`): By royal order, every mined iron or copper ore has a one-in-three chance to drop an extra raw ore.
- **Stone Covenant** (`stone_covenant`): The crown blesses public works: breaking stone below Y=32 grants Haste I for 12 seconds.
- **Breadline** (`breadline`): The granaries open: harvesting fully grown wheat, carrots, potatoes, or beetroot restores 1 hunger once every 20 seconds.
- **Charcoal Charter** (`charcoal_charter`): The forests serve the realm: chopping logs has a one-in-four chance to drop charcoal.
- **Deep Levy** (`deep_levy`): The mines are mobilized: breaking deepslate ores grants Haste II for 10 seconds and 1 experience point.
- **Green Commons** (`green_commons`): The commons are protected: breaking leaves has a one-in-five chance to return a matching sapling or apple.
- **Canal Act** (`canal_act`): State canals speed labor: mining or harvesting while wet grants Dolphin's Grace for 8 seconds.
- **Granary Audit** (`granary_audit`): The crown audits every harvest: crop harvesting sometimes withholds the bonus yield and refunds 3 Policy Points.
- **Timber Quota** (`timber_quota`): Royal quotas bite: every tenth log chopped drops two extra sticks and grants Haste II for 15 seconds.
- **Quarry Whistle** (`quarry_whistle`): When a miner breaks coal, redstone, or lapis ore, nearby subjects gain Haste I for 8 seconds.

##### Military
- **The People's Blade** (`peoples_blade`): Militia law is declared: after killing a hostile mob, gain Strength I for 10 seconds.
- **Shield Wall Decree** (`shield_wall_decree`): The guard holds formation: blocking damage grants Resistance I for 6 seconds.
- **Wolf Tax** (`wolf_tax`): Kennels are funded: killing a skeleton has a one-in-four chance to grant Speed I for 12 seconds.
- **Last Stand Clause** (`last_stand_clause`): No subject falls quietly: dropping below 5 hearts grants Resistance II for 8 seconds once per minute.
- **Monster Bounty** (`monster_bounty`): The treasury pays for safety: killing hostile mobs grants 1 extra experience.
- **Siege Rations** (`siege_rations`): Wartime rations begin: everyone gains Strength I after eating but loses 1 hunger immediately.
- **Blood Standard** (`blood_standard`): Wartime banners rise: everyone gains Strength I while below half health but has 2 fewer max hearts.
- **Powder Inspection** (`powder_inspection`): Explosives are regulated: creeper and TNT damage grants Fire Resistance and Resistance for 8 seconds.
- **Draft Notice** (`draft_notice`): The draft burdens all subjects: combat kills no longer trigger bounty XP and refund 3 Policy Points.
- **Border Watch** (`border_watch`): Watch posts report danger: being hit by a projectile grants Speed I for 8 seconds.

##### Economic
- **Guild Tithe** (`guild_tithe`): Guilds pay in kind: earning experience has a one-in-four chance to grant 1 extra experience.
- **Minted Overtime** (`minted_overtime`): The mint rewards long labor: every fifth ore broken grants 3 experience.
- **Market Day** (`market_day`): Market stalls open: trading with villagers grants Regeneration I for 8 seconds.
- **Salvage Rights** (`salvage_rights`): Nothing is wasted: killing armored mobs has a chance to drop an iron nugget.
- **Enchanter's License** (`enchanters_license`): Licensed scholars prosper: collecting an experience orb while near an enchanting table grants 2 bonus XP.
- **Public Ledger** (`public_ledger`): The ledgers are open: every new active policy announces its cost and remaining Policy Points.
- **Austerity Act** (`austerity_act`): Austerity is imposed: subjects lose 10 percent of earned experience and refund 3 Policy Points.
- **Blacksmith Contract** (`blacksmith_contract`): The forges work for the realm: mining iron while holding a damaged tool repairs it by 1 durability.
- **Fisher Auction** (`fisher_auction`): Dock auctions are sanctioned: catching fish grants Luck I for 12 seconds.
- **War Bonds** (`war_bonds`): Wartime bonds sell fast: everyone gains 25 percent bonus XP from combat but takes 10 percent more damage.

##### Social
- **Open Roads Act** (`open_roads`): The highways are cleared: sprinting on roads, stone, or planks grants Speed I.
- **Public Clinic** (`public_clinic`): The clinics open: sleeping or respawning grants Regeneration II for 15 seconds.
- **Festival Law** (`festival_law`): The realm celebrates: eating sweet food grants Jump Boost I for 12 seconds.
- **Safe Lodging** (`safe_lodging`): Inns receive funding: entering a bed clears Poison and Hunger.
- **Courier Network** (`courier_network`): Royal couriers ride: after traveling 300 blocks, gain Speed II for 20 seconds.
- **Night School** (`night_school`): Night schools convene: after sunset, subjects gain Night Vision while outdoors.
- **Bread and Circuses** (`bread_and_circuses`): Wartime pageantry begins: everyone gains Speed II, but max health is reduced by 2 hearts.
- **Ration Cards** (`ration_cards`): Rations are tightened: natural regeneration is slowed by Hunger I and refund 3 Policy Points.
- **Civil Service** (`civil_service`): Helpful clerks reduce friction: opening the policy viewer shows the king's trust and current promises.
- **Stone Shelters** (`stone_shelters`): Public shelters stand ready: taking fall damage grants Resistance I for 8 seconds.

##### Corruption
Corruption policies are asymmetric: the king gains personal power while subjects pay the cost. They cost 0 Policy Points, but promise breaks and public reaction can damage trust.

- **Crown Tax** (`crown_tax`): The king claims first profit: the king gains 40 percent bonus XP while subjects lose 15 percent XP.
- **Velvet Gaol** (`velvet_gaol`): The palace is secure: the king receives Resistance II while subjects suffer Mining Fatigue I.
- **Royal Physician** (`royal_physician`): Court physicians serve one patient: the king receives Regeneration II while subjects cannot skip night.
- **Private Armory** (`private_armory`): The royal armory closes to the public: the king gains Strength II while subjects suffer Weakness I.
- **Silken Roads** (`silken_roads`): The roads bend toward the palace: the king gains Speed II while subjects suffer Slowness I.
- **Dragon Seal** (`dragon_seal`): Forbidden seals protect the throne: the king gains Fire Resistance and subjects take 10 percent more damage.


---

## 🚧 Known Limitations / Roadmap

- Config is loaded **once at startup**; `/kingdom admin reload` does not yet re-read `config.yml`. Restart the server to apply config changes.
- Only the **plurality** voting system is implemented. Ranked-choice and runoff are planned.
- Map marker coordinates (Capital, Election Hall) are currently placeholders. Custom location configuration is planned.
- The Orb of Origin crafting recipe works automatically from the built-in data. Verify your Origins version registers `origins:orb_of_origin`.
