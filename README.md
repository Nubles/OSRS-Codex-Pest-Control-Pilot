# Codex Pest Control Pilot

**Which portal to work next, and how many games until your void.** In every Pest Control game the Pilot reads the shield order as it unfolds, tells you which portal to work, marks it in the world and on the minimap, and keeps a running count of your damage so you never miss out on points. Outside the game it counts your commendation points toward the void pieces you choose and how many games that takes.

It is part of the **Codex** suite, alongside [Codex Navigator](https://github.com/Nubles/OSRS-Codex-Navigator), [Codex Guardian](https://github.com/Nubles/OSRS-Codex-Guardian), [Codex Gear Roadmap](https://github.com/Nubles/OSRS-Codex-Gear-Roadmap), [Codex Session Chronicle](https://github.com/Nubles/OSRS-Codex-Session-Chronicle), [Codex Ledger](https://github.com/Nubles/OSRS-Codex-Ledger) and [Codex Quartermaster](https://github.com/Nubles/OSRS-Codex-Quartermaster), and shares their look: warm ink, aged gold and the game's own fonts.

Guidance only: the Pilot never clicks, moves or attacks for you.

<p align="center">
  <img src="docs/screenshots/battle.png" width="225" alt="Battle">
  <img src="docs/screenshots/goal.png" width="225" alt="Goal">
  <img src="docs/screenshots/record.png" width="225" alt="Record">
</p>

## What it does

- **Work next.** One line that says what to do now. When a shield is down: attack that portal, the weakest one if several are open unless another is a good deal nearer, and kill the spinners first when they are healing it. When no shield is down but the order is known: head for the portal whose shield drops next, and which gate to leave by. Before that: hold by the knight, with the portals that could drop next. When the Void Knight is low and nothing is open: defend him.
- **The island, charted.** The Battle tab draws the outpost with its west, east and south gates and the four portals where they stand. Open portals glow, shielded ones sit behind a pale ring, fallen ones go grey, each shows its health, and they are numbered in the order their shields drop. The one to work wears a gold halo, with the way there traced from the knight through the right gate.
- **Reading the rotation.** Shields always drop in one of six orders. After the first one or two drops the Pilot knows the rest of the game and sends you ahead of the crowd.
- **Marked in the world.** The portal to work gets an outline in its colour, a gilt ring on its tile, a word over it and a ring on the minimap. While the next drop is still open, every candidate gets a faint outline.
- **Damage for points.** A game only gives points once you have dealt 50 damage. The plaque and the panel count yours, so you know when you have done enough.
- **Goal.** Pick the void melee, ranger or mage set, all three helms, elite void, or any pieces you like (the mace and seal too). The Pilot counts your points against what is left, skipping pieces already in your bank or inventory, and says how many games and how long that is. Points per game and minutes per game are measured from your own games, losses included, once three are in. It flags the combat levels void needs and the hard Western Provinces diary elite needs, and says in the chat box when your points first cover a piece.
- **Record.** Games played, how many were won, points earned, points per hour in games and your recent games with their damage, length and points.
- **Plaque.** A gilt plaque in the game view with what to work next and your damage. At the outpost and in the lander it shows your points toward the goal instead.

<p align="center">
  <img src="docs/screenshots/battle-wait.png" width="225" alt="Waiting for the first shield">
  <img src="docs/screenshots/goal-elite.png" width="225" alt="Elite void goal">
  <img src="docs/screenshots/lander.png" width="225" alt="In the lander">
</p>

<p align="center">
  <img src="docs/screenshots/plaque.png" alt="Plaque in a game">
  <img src="docs/screenshots/plaque-next.png" alt="Getting ahead of the next shield">
  <img src="docs/screenshots/plaque-outpost.png" alt="Plaque at the outpost">
</p>

## How it reads a game

The game's own status bar gives each portal's health and the knight's. Shields are read from the portals themselves and from the "portal shield has dropped" messages, and each drop rules out the rotations that do not start that way. Your damage is the sum of your own hitsplats on anything in the game. Your points are read from the lander when you board one and from the message at the end of each game.

Everything is kept per account in RuneLite's profile settings, nothing leaves your computer.

## Settings

| Setting | What it does |
| --- | --- |
| Pilot plaque | Show the plaque in games only, at the outpost too (the default), or never |
| Mark the portal | Outline the portal to work in the world and on the minimap |
| Mark possible next portals | Faintly outline every portal that could drop next while none is down |
| Defend knight below | The knight's health at which defending him comes first (60, 0 turns it off) |
| Lander for estimates | Which lander the games-left estimate assumes until your games are measured |
| Announce affordable rewards | Say in the chat box when your points first cover a piece of your goal |

## Building

```
./gradlew build
```

Run it in a development client with `./gradlew run`.

## Licence

BSD 2-Clause, see [LICENSE](LICENSE).
