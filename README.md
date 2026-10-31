# NoteLoader

NoteLoader is a Minecraft server mod for version 1.21+ that allows chunks around NoteBlocks to stay loaded temporarily when triggered.

## Features

- Forces a configurable area of chunks around a NoteBlock to remain loaded when a player clicks it or when it receives a redstone signal.
- Configurable radius, duration, cooldown, loading mode, and entity ticking via JSON or commands.
- Two loading modes:
  - `forced` – uses the vanilla forced-chunk system (`/forceload` equivalent). Forced chunks are persisted and always tick entities.
  - `ticket` – adds chunk tickets directly through the server chunk manager. Nothing is persisted to disk, and entity ticking can be disabled (chunks then stay block-ticking, so redstone and block entities keep running).

## Commands

- `/noteloader info` – Display all currently forced chunks, remaining ticks, and the active loading mode.
- `/noteloader reload` – Reload the configuration file.
- `/noteloader set radius <value>` – Set the loading radius.
- `/noteloader set duration <ticks>` – Set how long chunks remain loaded.
- `/noteloader set mode <forced|ticket>` – Choose the chunk loading implementation. Applies to active chunks immediately.
- `/noteloader set entityticking <true|false>` – Whether entities keep ticking in loaded chunks (only effective in `ticket` mode; `forced` mode always ticks entities).
- `/noteloader cleanall` – Instantly clears all currently forced chunks across all dimensions.

## Installation

1. Place the mod `.jar` file into your server's `mods` folder.
2. Start the server with Fabric loader 1.21+ and Fabric API installed.
3. Configure options in `config/noteloader.json` as needed.

## License

MIT