# Fauna Compat Manager

A configurable compatibility manager for Minecraft fauna mods on Forge 1.20.1. When you install multiple fauna mods together, you often end up with duplicate animals (two different bears, two different elephants, etc.) spawning side by side. Fauna Compat Manager lets you choose, per species, which mod's version you want — and makes sure you never lose access to items, crafting recipes, or brewing recipes that depended on the version you didn't pick.

Unlike simple "remove the duplicate" compat packs, Fauna Compat Manager is:

- **Configurable** — pick a winner per species via an in-game GUI, or leave everything untouched (default).
- **Non-destructive by default** — with no configuration at all, every mod behaves exactly as if this mod weren't installed. Nothing is suppressed, redirected, or removed until you explicitly choose a mod for a given species.
- **Data-driven** — species pairings, drops, and priorities live in JSON files, so new mod support can be added without touching Java code.
- **Progression-safe** — if the mob you pick doesn't natively drop something the other mod's version did (a crafting ingredient, a taming item, a brewing ingredient), Fauna Compat Manager backfills it so no recipe becomes permanently uncraftable.

## Currently supported mods

- [Alex's Mobs](https://www.curseforge.com/minecraft/mc-mods/alexs-mobs)
- [Naturalist](https://modrinth.com/mod/naturalist)
- [Critters and Companions](https://www.curseforge.com/minecraft/mc-mods/critters-and-companions)

None of these are required dependencies — Fauna Compat Manager detects at runtime which of them are installed and only acts on species where at least two of them overlap.

## Species covered

Every pairing below was verified directly against each mod's loot tables, recipes, and (where relevant) spawn biome tags — not guessed from mob names.

| Species | Mods involved | Notes |
|---|---|---|
| Bear | Alex's Mobs, Naturalist | 4 Alex's Mobs recipes and 1 brewing recipe adapted so either mob's fur works |
| Elephant | Alex's Mobs, Naturalist | Alex's Mobs' elephant has no native drops; Naturalist's leather/bushmeat is backfilled |
| Rhino | Alex's Mobs, Naturalist | Same pattern as Elephant |
| Catfish | Alex's Mobs, Naturalist | Both mods are self-contained (their own raw → cooked chain); no cross-recipe needed |
| Snake | Alex's Mobs, Naturalist | Alex's Mobs' rattlesnake rattle (used to craft a maraca) is backfilled if Naturalist's snake is picked |
| Blue Jay | Alex's Mobs, Naturalist | Spawn-only overlap, no unique drops on either side |
| Tortoise | Naturalist, Alex's Mobs (Terrapin) | Naturalist's scute drop is backfilled if Alex's Mobs' terrapin is picked |
| Alligator | Naturalist, Alex's Mobs (Caiman) | Paired specifically with Caiman, not Crocodile — Alex's Mobs' Crocodile spawns in a distinct desert biome and does not compete with Naturalist's swamp/river Alligator |
| Snail | Naturalist, Critters and Companions | Critters' snail produces slime via a live bottle interaction (not a death drop) — this behavior is ported to Naturalist's snail if it's the one selected |
| Dragonfly | Naturalist, Critters and Companions | Included despite different native biomes (both inhabit the Overworld and represent the same real animal, unlike a Nether-exclusive mob, which would be excluded). Naturalist's frog-hunts-dragonfly behavior (via the vanilla `#minecraft:frog_food` tag) is ported to Critters' dragonfly; Critters' dragonfly wing (used to tame the jumping spider) is backfilled onto Naturalist's dragonfly if picked |

Explicitly evaluated and **excluded** as false positives: Soul Vulture vs. Vulture (Nether-exclusive, no shared drops), Anaconda vs. Snake (different ecological niche, no shared drops/recipes), Crocodile vs. Alligator (distinct desert vs. swamp biome design).

## Configuration

### In-game GUI (recommended)

Requires [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) (client-side). Open it from the Mods menu → Fauna Compat Manager → Config. Each detected species shows a dropdown:

- **All** (default) — both mods' versions coexist exactly as if this mod weren't installed.
- **[Mod name]** — that mod's version becomes the only one that spawns naturally; the other is suppressed and its unique drops/recipes/behaviors are backfilled as described above.

Changes to spawning, drops, and live interactions (brewing, bottle interactions) apply instantly. Changes to crafting-table recipes require a `/reload` or rejoining the world, since Minecraft loads those once at data-pack load time.

### Manual TOML

`config/faunamanager-common.toml`, format `"species=modid"`, e.g. `bear=alexsmobs`. Omit a line (or use `"species=todos"`) to leave that species untouched.

### Cleanup command

Changing a selection does not retroactively remove mobs that already exist in the world. To manually clean up already-spawned, currently-suppressed mobs:

```
/faunamanager cleanup          # preview only, removes nothing
/faunamanager cleanup confirm  # actually removes them
```

Tamed animals and any entity with a custom name are **never** removed, regardless of suppression status. Requires operator permission (level 2). Only affects currently loaded chunks.

## Installation

1. Install Forge 47.3.0+ for Minecraft 1.20.1.
2. Install Fauna Compat Manager.
3. Install any combination of the supported mods above, along with their own dependencies (Alex's Mobs needs Citadel; Naturalist needs GeckoLib).
4. Optionally install Cloth Config API for the in-game configuration screen.

## Building from source

```
git clone <this repo>
cd faunamanager
./gradlew build
```

Requires JDK 17. Output jar is written to `build/libs/`.

## Known limitations

- Crafting-table recipe changes require a world reload (`/reload` or rejoin) to take effect — see Configuration above.
- The cleanup command only affects loaded chunks; run it again after exploring more of the world to catch mobs elsewhere.
- Taming an animal is a one-time interaction that cannot be transferred between mods — if you switch a species selection, previously tamed animals of the now-suppressed mod remain tamed, but you can no longer tame new ones of that variant.
- Detecting "already tamed" for the cleanup command relies on the vanilla `TamableAnimal` interface; a mod using a fully custom taming system outside that interface would not be recognized as tamed by the cleanup check.

## Contributing / adding a new mod

Compatibility groups live in `src/main/resources/data/faunamanager/compat/groups/*.json`, one file per species. See existing files for the format. Cross-mod item substitution (for a mod's crafting recipes that reference a specific item) requires a small `forge:conditional` recipe override alongside a tag — see `data/alexsmobs/recipes/` for examples.

## Credits

Some spawn-cancellation and data-loading patterns were informed by studying the source of [VMinus](https://github.com/lixxir/VMinus) (by lixir) under the terms of its license, which permits studying and adapting individual techniques with attribution. No code from VMinus is copied or redistributed; the architecture and implementation here are original.

This project is not affiliated with the authors of Alex's Mobs, Naturalist, Critters and Companions, or Cloth Config API.

## License

MIT — see `LICENSE`.