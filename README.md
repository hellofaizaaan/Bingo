# Bingo

A Paper plugin: teams race to **collect** the items on a shared, seeded bingo card.
First to a line wins.

- **Platform:** Paper `26.2` (Bukkit API), Kotlin, JDK 25
- **Package:** `com.mohammadfaizan.bingo`

## How it plays

1. Players `/bingo join <team>` (or let `auto-assign` split everyone up).
2. An admin runs `/bingo start` — a card is rolled from a seed, players are healed,
   cleared, spread out, and a countdown runs.
3. **GO.** Picking up, crafting, or moving a card item claims that cell instantly
   for your team (a 0.5 s inventory sweep is the backstop). Cells stay claimed.
   A live action bar shows your count and the current leader.
4. First team to the win condition takes it — titles, fireworks, done.

## Commands

`/bingo <sub>` &nbsp;(alias `/bg`)

| Sub | Does |
|---|---|
| `start [mode] [size] [seed]` | begin a game (defaults from config) |
| `solo [mode] [size]` | begin a game where every online player is their own team |
| `stop` | end the game |
| `join <team>` / `leave` | pick / drop a team |
| `spectate` | go spectator; `/bingo card` then shows which team holds each cell |
| `card` | open the card as a chest GUI (glowing = your team has it, barrier = locked by another team) |
| `teams` | list teams and their members |
| `status` | mode, size, seed, per-team cell counts |
| `reveal` | print the current card to yourself |
| `top` | win/games leaderboard (persisted in `stats.yml`) |
| `stats [player]` | wins / games for you or another player |
| `reroll` | roll a new card (before the game starts) |
| `reload` | re-read `config.yml` + item pool |

On start each player is handed a **Bingo Card** item (right-click to open the GUI).

`start` / `stop` / `reroll` / `reload` need `bingo.admin` (op). `bingo.exempt` players aren't auto-assigned.

## Modes

| Mode | Win |
|---|---|
| `standard` | first full row, column, or diagonal |
| `blackout` | first to every cell |
| `lockout` | cells lock **globally** when claimed — competitive; first to a line of your own cells |
| `count` | first to `count-target` cells (lines ignored) |

## Config (`config.yml`)

| Key | Default | Meaning |
|---|---|---|
| `default-mode` / `default-size` | `standard` / `5` | used by bare `/bingo start` |
| `count-target` | `13` | cells needed in `count` mode |
| `start-countdown` | `10` | seconds before GO |
| `time-limit` | `0` | seconds; `0` = none. On expiry, most cells wins (tie = draw) |
| `auto-assign` | `true` | round-robin every online player onto teams at start |
| `on-start.*` | all `true` | clear inventory, heal, spread, set time to day |
| `spread-radius` | `500` | blocks from world spawn |
| `teams` | red/blue/green/yellow | `id: { display, color }` — colour is a MiniMessage colour name |
| `item-pool` | ~130 items + 9 groups | `distribution` weights, `groups` (named "any of these" sets), and `easy` / `medium` / `hard` lists of materials and `"#group"` refs |

Card generation: fill `size²` distinct **cells** by rolling a tier (weighted by
`distribution`) then a random entry from it, seeded by the game seed — so the same
seed always produces the same card. A cell is either an exact item or a group
(`#planks` → "any planks"); holding **any** member of a group claims it.

## Build

```bash
./gradlew build      # -> build/libs/Bingo-<version>.jar
./gradlew runServer  # throwaway Paper 26.2 test server
```

Requires **JDK 25**. No wrapper jar committed yet — open in IntelliJ (downloads
Gradle 9.7.1 from `gradle-wrapper.properties`) or run `gradle wrapper --gradle-version 9.7.1` once.

## Releases

`.github/workflows/release.yml` builds on every push and publishes a GitHub
Release with the jar whenever `version` in `build.gradle.kts` changes.

## Roadmap

- [x] Instant claim on pickup / craft / inventory move (+ 0.5 s backstop)
- [x] `/bingo solo` — everyone is their own team
- [x] Live action-bar standings
- [x] Material groups (`#planks` → "any planks")
- [x] Spectator mode (`/bingo spectate` + holder-aware card view)
- [x] Item-pool size guard on start
- [x] Physical Bingo Card item (right-click to open)
- [x] Claim particles + persisted win/games stats (`/bingo top`, `/bingo stats`)
- [x] `/bingo reveal`
- [ ] 7×7 cards (needs a double-chest / bigger GUI)
- [ ] Per-game team creation (`/bingo team create`)
- [ ] Scoreboard sidebar
- [ ] Persist an in-progress game across restart
