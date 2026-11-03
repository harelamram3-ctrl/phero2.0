# SuperHeroes 3.1.1 — Source Repository

A Paper plugin source project that lets players choose from 14 superhero characters, use abilities and ultimates, and progress through XP and levels.

> **Build status:** This ZIP contains source code, not a prebuilt JAR. A JAR must be compiled with Maven and Java 21. The current project does **not** yet implement the planned Free/Infinity Edition licensing split, automatic Infinity Gauntlet world event, or bundled Resource Pack delivery. Those are future development items and are not advertised as implemented.

## Requirements
- Java 21
- Paper 1.21.11
- Maven 3.9+

## Build
Run from the project root:

```bash
mvn clean package
```

The plugin JAR will be generated at:

```text
target/SuperHeroes-3.1.1.jar
```

## Install
1. Build the project, or download the JAR from a successful GitHub Actions build.
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
XP is awarded for damage dealt and kills. Levels and XP are stored in `plugins/SuperHeroes/players.yml`. Level bonuses are configurable in `config.yml`.

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

## Current implemented highlights
- 14 superhero choices, abilities, ultimates and progression.
- Spider-Man web shot and web swing mechanics.
- Hero-specific movement and passive stats.
- Optional player skin integration through SkinsRestorer.
- Player-head equipment for hero identity.

## GitHub Actions
The included workflow builds the project on pushes and pull requests to `main`, then uploads the JAR as an Actions artifact. Open the workflow run in GitHub and download the `plugin-jar` artifact after a successful build.

## Notes
Back up your existing plugin folder before replacing an older build. Test on a staging server before using it on a public server.
