# SuperHeroes 3.1.1

A Paper plugin that lets players become one of 14 superheroes, use two abilities and an ultimate, and progress through persistent hero XP and levels.

## Requirements
- Java 21
- Paper 1.21.11
- Maven 3.9+

## Build
```bash
mvn clean package
```
The plugin JAR is created at `target/SuperHeroes-3.1.1.jar`.

## Install
1. Build the project or download the JAR from your build pipeline.
2. Put `SuperHeroes-3.1.1.jar` in the server's `plugins` folder.
3. Restart the Paper server.
4. Edit `plugins/SuperHeroes/config.yml` and run `/hero reload` to apply configuration changes.

## Commands
| Command | Description | Permission |
|---|---|---|
| `/hero` or `/hero menu` | Open the hero selection menu | `superheroes.use` |
| `/hero list` | List enabled heroes | `superheroes.use` |
| `/hero info [hero]` | Show hero abilities and cooldowns | `superheroes.use` |
| `/hero stats` | Show kills, ultimate charge, level and XP | `superheroes.use` |
| `/hero ult` | Activate a charged ultimate | `superheroes.use` |
| `/hero none` | Remove your hero | `superheroes.use` |
| `/hero set <player> <hero|none>` | Set another player's hero | `superheroes.admin` |
| `/hero reload` | Reload configuration | `superheroes.admin` |

## Controls
- Right-click with your hero power item: primary ability.
- Sneak + right-click: secondary ability.
- Press `F` (swap hands) while holding the power item: ultimate, when charged.
- Flight-capable heroes can double-jump to fly and sprint while flying to boost.

## Progression
XP is awarded for damage dealt and kills. Levels and XP are stored in `plugins/SuperHeroes/players.yml`. Level bonuses are configurable in `config.yml`; by default each level adds 1% ability damage, capped at 20 bonus levels.

## Configuration
- `settings.disabled-worlds`: worlds where powers are disabled.
- `settings.damage-multiplier`: global ability damage multiplier.
- `settings.cooldown-multiplier`: global cooldown multiplier.
- `settings.pvp-abilities`: whether abilities can damage players.
- `settings.show-bossbar`: show ultimate charge.
- `heroes.<HERO>.enabled`: enable/disable a hero.
- `heroes.<HERO>.damage-multiplier`: per-hero damage multiplier.
- `heroes.<HERO>.cooldown-multiplier`: per-hero cooldown multiplier.
- `progression.*`: XP, level cap, and level damage bonus.

## Notes
This is a source project, not a prebuilt JAR. Build it with Maven on a machine with access to the PaperMC Maven repository. Back up your existing plugin folder before replacing an older build.


## Superhero powers, size and skins (3.1.0)

- Hero-specific movement speed, attack damage, knockback resistance and scale are applied when a hero is selected and restored when the hero is removed. Hulk is larger; Spider-Man and Flash are slightly smaller; Superman and Thor are larger.
- Hulk and Superman receive extra max health. Flying heroes have faster flight and sprint-boost speeds.
- Several beam and Hulk smash attacks have been strengthened.
- **Player skins require SkinsRestorer**. Bukkit/Paper's public API does not provide a supported way for a plugin to replace a player's account skin by itself. Install SkinsRestorer, then configure `skins.apply-command` and `skins.reset-command` in `config.yml`. The defaults use `skin set {skin} {player}` and `skin clear {player}`. Replace the skin names with valid Minecraft Java usernames that have the desired skins (or names of custom skins saved in SkinsRestorer).
- After installing SkinsRestorer, restart the server and run `/hero reload` if you edit the configuration.


## Spider-Man web upgrade (3.1.1)

- Primary: aims at an entity to snare it briefly, deal light damage and reel it toward Spider-Man. If no entity is hit, aim at a solid block to web-zip toward it.
- Secondary (sneak + right-click): attaches a web to a solid block up to 40 blocks away and swings the player with a visible particle rope, forward momentum and fall-distance protection. Sneak again to release.
- The web abilities do not place or modify blocks, so they are safe to use in protected builds.
