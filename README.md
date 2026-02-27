# NoteLoader

NoteLoader is a Minecraft server mod for version 1.21+ that allows chunks around NoteBlocks to stay loaded temporarily when triggered.

## Features

- Forces a configurable area of chunks around a NoteBlock to remain loaded when a player clicks it or when it receives a redstone signal.
- Configurable radius, duration, and cooldown via JSON.

## Commands

- `/noteloader info` – Display all currently forced chunks and remaining ticks.
- `/noteloader reload` – Reload the configuration file.
- `/noteloader setradius <value>` – Set the loading radius.
- `/noteloader setduration <ticks>` – Set how long chunks remain loaded.

## Installation

1. Place the mod `.jar` file into your server's `mods` folder.
2. Start the server with Fabric loader 1.21+ and Fabric API installed.
3. Configure options in `config/noteloader.json` as needed.

## License

MIT