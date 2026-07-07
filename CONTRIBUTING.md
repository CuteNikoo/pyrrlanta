# Contributing to Pyrrlanta

## Workflow

1. Clone the repo (don't work off a fork unless you're an outside
   contributor without write access).
2. Create a branch off `main` for your change:
   ```bash
   git checkout -b your-name/short-description
   ```
3. Commit your changes with clear messages.
4. Push your branch and open a pull request against `main`.
5. At least one other person should review before merging, when possible.
6. The `Build` GitHub Actions workflow runs `./gradlew build` on every push
   and PR — make sure it's green before merging.

`main` is the shared branch. Avoid pushing directly to it for anything
non-trivial — use a branch + PR so people don't step on each other's changes.

## Avoiding merge conflicts

- Keep `gradle.properties` (mod id/version) changes to their own small PRs —
  everyone touches this file rarely, but conflicts here are annoying.
- If you're adding a new block/item/feature, put it in its own Java file
  rather than piling onto `Pyrrlanta.java`, and register it from there. This
  keeps the main class as a small, stable set of registration calls instead
  of a merge-conflict magnet.
- Two people editing the same lang file (`en_us.json`) at once is a common
  source of conflicts — pull before you start, and coordinate in chat if
  you're both adding translations at the same time.

## Code style

- Follow the existing formatting in the template (NeoForge's standard MDK
  conventions — 4-space indents, standard Java package structure).
- Mod id is `pyrrlanta`; keep resource/registry names in that namespace.

## Local testing

Run `./gradlew runClient` to launch the game with your changes and check
them in-game before opening a PR.
