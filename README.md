# Pyrrlanta

A NeoForge mod for Minecraft 1.21.1. This is currently an empty skeleton — no
blocks, items, or features yet — set up so multiple people can build on it
together.

## Requirements

- JDK 21 (Gradle will auto-download one via its toolchain support if you
  don't have it — you don't need to install it manually).
- Any Java IDE with Gradle support. IntelliJ IDEA is the most common choice
  for NeoForge development.

## Getting started

Clone the repo, then open it in your IDE as a Gradle project (or run the
commands below from the repo root).

```bash
# Run the game as a client, with the mod loaded
./gradlew runClient

# Run a dedicated server, with the mod loaded
./gradlew runServer

# Just build the mod jar (output in build/libs/)
./gradlew build
```

On Windows use `gradlew.bat` instead of `./gradlew`.

If your IDE is missing dependencies or something looks stale, try:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

## Project layout

- `src/main/java/com/pyrrlanta/pyrrlanta/` — mod source code.
  - `Pyrrlanta.java` — main mod entry point (common side).
  - `PyrrlantaClient.java` — client-only setup.
- `src/main/resources/assets/pyrrlanta/` — lang files, textures, models, etc.
- `src/main/templates/META-INF/neoforge.mods.toml` — mod metadata, templated
  from the properties in `gradle.properties`.
- `gradle.properties` — mod id, name, version, and Minecraft/NeoForge
  versions live here.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch/PR workflow.

## License

MIT — see [LICENSE](LICENSE).
