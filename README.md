[ English | [Русский](README_RU.md) ]

# 💡 Envoys

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=for-the-badge&logo=minecraft)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)

**Envoys** is a Minecraft modification designed for creating custom Non-Player Characters (NPCs) with branching dialogues, an advanced economy, quests, and a built-in visual logic editor paired with a lightweight Lua engine.

The mod offers two ways to build scenarios: an **intuitive in-game visual builder** for quickly assembling action sequences, and **full-fledged Lua scripting** for complex, programmable logic.

---

## 📚 Documentation & Guides

Comprehensive guides covering mod configuration, the visual editor, commands, and the complete Lua API:

* 📖 **[English Documentation](docs/en/INDEX.md)** — complete documentation and API reference.
* 📖 **[Русская документация](docs/ru/INDEX.md)** — full step-by-step guide in Russian.

---

## ⚡ Features

### 🎨 Visual Scenario Editor (GUI)
* **Code-free logic creation:** configure NPC behavior via a clean in-game interface — action chains, dialogue trees, and reactions to clicks, attacks, or player proximity.
* **Synchronization & status indicators:** clear color-coded event states (🟢 up-to-date, 🟡 modified and awaiting compilation, ⚪ protected custom code).
* **Crash protection:** automatic draft backups saved to `.temp` upon exiting the game, with instant session restore prompts on re-entry.

### 🔄 Bidirectional Translation (GUI ↔ Lua)
* **Custom code protection:** if you write advanced logic directly in a `.lua` file, the editor detects custom code and locks visual overwrites from the GUI to prevent data loss.
* **Client-side deployment:** upload scripts directly to the server and pull current scenarios back to your client (`pull` / `send`).

### 🧠 Embedded Lua Engine (Cobalt)
* **Full Lua 5.1 support:** coroutines (`wait`, `move`, asynchronous dialogues) running directly on the main server thread without dropping TPS.
* **Safe sandbox:** built-in watchdog timer to terminate infinite loops, accompanied by strict filesystem isolation.
* **Hot reloading:** update NPC behavior on the fly without restarting the world or server.

### 💬 Dialogues & Branching
* Interactive dialogue windows with clickable reply choices.
* Full support for UTF-8 (Cyrillic, special characters) and Minecraft color code formatting.

### 💰 Advanced Economy & Trading
* **Dynamic offers:** generate trade lists, prices, and stock on the fly via code or visual menus.
* **Auto-inflation & demand:** configure price scaling factors (`priceMultiplier`, `demand`).
* **Limits & timers:** restrict maximum purchases (`maxTrades`) and set restock cooldowns (`resetTime`).
* **Reputation levels:** gate high-tier items and unlock new trade slots as player reputation progresses (`envoys.merchant`).

### 📜 Quest System
* Supports three core objective types:
  * **Mob hunting** (kill tracking with progress counters);
  * **Item gathering & delivery**;
  * **Multi-stage story quests** (step-by-step progression).

---

## 🛠️ Requirements

* **Minecraft:** `1.21.1`
* **Mod Loader:** `NeoForge` (`21.1.233` or newer)
* **Java:** `21`

---

## 📥 Installation

1. Install the matching version of **NeoForge** for both client and server.
2. Place the downloaded `.jar` file into your `.minecraft/mods` directory.
3. Launch the game.

---

## 🚀 Quick Start

1. Grab the configuration tool (**Tuner**) or use the `/envoys` commands.
2. **Right-click** on a block or entity to configure the NPC's name, skin, and basic properties.
3. Navigate to the **Events** tab:
   * **Via GUI:** add actions, configure dialogues, and click *“Process Scenario”* to compile the logic onto the NPC.
   * **Via Code:** create a `.lua` file in `.minecraft/envoys/local/` and deploy it to the NPC:
     ```text
     /envoys lua send aim my_script.lua
     ```

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.  
The source code is open for study and forks, provided that original authorship is preserved and derivative works are published under an identical license.

Author: **FrostMatrix**  
Repository: [github.com/FrostMatr1x/Envoys](https://github.com/FrostMatr1x/Envoys)