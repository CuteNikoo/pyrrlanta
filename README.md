# Pyrrlanta

NeoForge mods for Minecraft 1.21.1. This repo builds **two independent mods**
from one codebase:

| Subproject | Mod id | Jar | Install on |
|---|---|---|---|
| `items/` | `pyrrlanta` | `pyrrlanta-<version>.jar` | **Client *and* server** |
| `tribes/` | `pyrrlanta_tribes` | `pyrrlanta_tribes-<version>.jar` | **Server only** |

- **`items` (Pyrrlanta)** — the Twilight E.G.O. weapon and armor set. It
  registers real content (items, armor materials) and applies a client-only
  mixin for the helmet's highlight, so every player needs it installed; an
  unmodded client cannot join a server running it.
- **`tribes` (Pyrrlanta Tribes)** — the Towny-style tribe/land-claiming
  system. It registers nothing into any game registry and sends no custom
  network payloads, and its whole UI is built from vanilla screens driven
  server-side. That means **unmodded clients can connect to a server running
  it** — NeoForge decides compatibility by registry/payload negotiation, not
  by matching mod lists, so no special "server only" flag exists or is needed
  in `neoforge.mods.toml`. It also still works in singleplayer/LAN.

The two mods are independent: neither requires the other, and you can run
either one alone.

> **Note on mod ids:** `items` intentionally keeps the original `pyrrlanta`
> id. Its items are registered as `pyrrlanta:twilight_ego_*`, and any already
> in a world or inventory would be destroyed if that namespace changed. The
> tribe system registers nothing, so it was safe to give it a new id — and its
> config file is pinned to the pre-split `pyrrlanta-common.toml` name so
> existing tuned values carry over.

## Requirements

- JDK 21 (Gradle will auto-download one via its toolchain support if you
  don't have it — you don't need to install it manually).
- Any Java IDE with Gradle support. IntelliJ IDEA is the most common choice
  for NeoForge development.

## Getting started

Clone the repo, then open it in your IDE as a Gradle project (or run the
commands below from the repo root).

```bash
# Build both mods (jars land in items/build/libs/ and tribes/build/libs/)
./gradlew build

# Run the game with a specific mod loaded — pick the subproject:
./gradlew :items:runClient
./gradlew :items:runServer
./gradlew :tribes:runClient
./gradlew :tribes:runServer

# Build just one mod
./gradlew :tribes:build
```

On Windows use `gradlew.bat` instead of `./gradlew`.

If your IDE is missing dependencies or something looks stale, try:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

## Project layout

```
items/                                   # mod id "pyrrlanta" (client + server)
  build.gradle                           # mod id, runs, datagen
  src/main/java/com/pyrrlanta/pyrrlanta/
    Pyrrlanta.java                       # @Mod entry point
    PyrrlantaClient.java                 # client-only setup
    item/                                # Twilight E.G.O. items
    mixin/                               # client-only helmet highlight mixin
  src/main/resources/                    # assets, data tags, mixins.json
  src/main/templates/META-INF/neoforge.mods.toml

tribes/                                  # mod id "pyrrlanta_tribes" (server only)
  build.gradle                           # mod id, runs, BlueMap soft dep
  src/main/java/com/pyrrlanta/pyrrlantatribes/
    PyrrlantaTribes.java                 # @Mod entry point
    tribe/                               # claims, commands, protection, GUI, tiers
  src/main/templates/META-INF/neoforge.mods.toml

build.gradle                             # shared config for both subprojects
gradle.properties                        # Minecraft/NeoForge/Parchment versions, shared
settings.gradle                          # declares the two subprojects
```

Each subproject defines its own `mod_id`/`mod_name` in its `build.gradle`;
everything shared (Minecraft/NeoForge/Parchment versions, license, version)
lives in the root `gradle.properties`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch/PR workflow.

## License

MIT — see [LICENSE](LICENSE).
