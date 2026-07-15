# CustomBlocks Testing Glossary

This document strictly defines the terminology used across all testing guides. If a word isn't here, it shouldn't be used in the guides. Our goal is 100% plain English, with zero developer jargon.

## Core Status Definitions

| Term                 | Symbol | Exact Meaning                                                                                                            |
| -------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------ |
| **Passed**           | ✅      | The test works perfectly in the actual game exactly as designed.                                                         |
| **Partial**          | 🟡      | Some parts work, but others are broken, unfinished, or need polish.                                                      |
| **Not Tested**       | 🟥      | The code is built, but nobody has gone into Minecraft to actually test it yet.                                           |
| **Finding**          | ⚠️     | The test passed the strict requirement, but you found a weird edge case or visual glitch that should be looked at later. |
| **Regression**       | 💔      | It used to pass, but a recent update broke it.                                                                           |
| **Blocked**          | ❗      | You literally cannot test this right now because a dependency is broken or a server is offline.                          |
| **Parked**           | 🧊      | The code is built, but the owner has explicitly decided not to test or use it right now.                                 |
| **Polish**           | 🛠️     | It technically works, but it feels clunky, sounds bad, or looks ugly. Needs a polish pass.                               |
| **Planned**          | ⏳      | The design is written, but no code exists yet. There is nothing to test.                                                 |
| **Needs Discussion** | ❔      | The feature is confusing, poorly designed, or contradictory. Stop building and talk to the owner.                        |
| **Test Now**         | 🎯      | This is your active mission. Go into the game and run this test immediately.                                             |

## Common Terminology

- **"Build Green":** *Avoid using this.* If code compiles but hasn't been tested in-game, it is strictly **🟥 Not Tested**.
- **"OP-gated":** *Use "Requires Admin/OP".* Means only a server operator can run the command or use the feature.
- **"In-game":** Inside the running Minecraft client connected to the server.
- **"Clean / Fresh Install":** Starting the server with completely empty `config/customblocks` folders, as if it's a brand new download.
- **"Dashboard":** The main `/cb` menu GUI in the game.
- **"Studio":** The `/cb create` and `/cb editor` block creation screens.
