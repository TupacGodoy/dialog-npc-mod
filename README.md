# Dialog NPC Mod — Fabric 1.21.1

Adds fully configurable NPCs with custom dialog boxes (like Cobblemon's Nurse Joy).
Right-click an NPC → dialog screen with clickable option buttons appears.

---

## 📦 Building

Requirements: JDK 17, internet connection (first build downloads dependencies).

```bash
./gradlew build
# Output: build/libs/dialog-npc-1.0.0.jar
```

Copy the jar to your `mods/` folder alongside **Fabric API**.

---

## 🎮 Commands (requires OP level 2)

All commands use standard entity selectors. The fastest way to target the
nearest NPC is: `@e[type=dialognpc:dialog_npc,sort=nearest,limit=1]`

### Create an NPC
```
/npc create "Nurse Joy"
```
Spawns an NPC at your feet. Prints its UUID in chat.

### Set dialog title
```
/npc settitle @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Nurse Joy"
```

### Set dialog text
```
/npc settext @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] Would you like me to heal your party?
```

### Add a clickable option
```
/npc addoption @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Heal Party"       pokeheal @p
/npc addoption @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Random Tip"       say Did you know Pikachu hates being in its Poke Ball?
/npc addoption @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] "Give Potion"      give @p minecraft:potion 1
```
- The **label** (first arg) supports spaces if you quote it: `"Heal Party"`
- The **command** (rest of line) is executed server-side with level-2 permissions
  when the player clicks that button — the client never sees the command text.

### Remove option by index
```
/npc removeoption @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0
```

### Clear all options
```
/npc clearoptions @e[type=dialognpc:dialog_npc,sort=nearest,limit=1]
```

### Set NPC skin texture
```
/npc settexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] minecraft:textures/entity/player/wide/alex.png
```
You can also use textures from your own resource pack:
```
/npc settexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] dialognpc:textures/entity/nurse_joy.png
```
Place `nurse_joy.png` (64×64 player skin format) at:
`assets/dialognpc/textures/entity/nurse_joy.png` inside a resource pack.

### Set custom texture without resource pack
Use textures directly from URLs, player skins, or base64 data — no resource pack needed!

```
/npc settexturetype @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] <vanilla|player|url|base64>
/npc setcustomtexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] <data>
```

**Examples:**

Use a player's skin:
```
/npc settexturetype @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] player
/npc setcustomtexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] Notch
```

Use a texture from URL:
```
/npc settexturetype @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] url
/npc setcustomtexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] https://i.imgur.com/your_texture.png
```

Use base64 encoded image data:
```
/npc settexturetype @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] base64
/npc setcustomtexture @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHe...
```

### Set dialog background color
```
/npc setbgcolor @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0xFF1A1A2E
```
Color format: `0xAARRGGBB` (hex). Example: `0xFF1A1A2E` = dark blue-gray.

### Set title bar color
```
/npc settitlecolor @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0xFF2D2D5A
```
Color format: `0xAARRGGBB` (hex). Example: `0xFF2D2D5A` = purple-blue.

### Set button width
```
/npc setbtnwidth @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 200
```
Width in pixels (50-400). Buttons are centered automatically.

### Set border color
```
/npc setbordercolor @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0xFF404080
```
Color format: `0xAARRGGBB` (hex). Example: `0xFF404080` = purple-blue border.

### Set title text color
```
/npc settitletextcolor @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0xFFFFD966
```
Color format: `0xAARRGGBB` (hex). Example: `0xFFFFD966` = yellow-gold text.

### Set options area height
```
/npc setoptionsheight @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] 0
```
Height in pixels (0 = auto, adjusts to fit buttons). Use custom values to add extra space.

### Configure NPC behavior
Control how the NPC interacts with players:

```
/npc setheadtracking @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] <true|false>
```
Head follows nearby players (default: true).

```
/npc setbodyrotation @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] <true|false>
```
Body rotates to face players (default: false).

```
/npc setcanmove @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] <true|false>
```
NPC can move from spawn position and can be pushed (default: false).

```
/npc setcanrotate @e[type=dialognpc:dialog_npc,sort=nearest,limit=1] <true|false>
```
NPC can rotate (yaw/pitch changes) (default: false).

### Show NPC info
```
/npc info @e[type=dialognpc:dialog_npc,sort=nearest,limit=1]
```

### Remove an NPC
```
/npc remove @e[type=dialognpc:dialog_npc,sort=nearest,limit=1]
```

---

## 🔒 Security model

When the player clicks a dialog option:
- The **client sends only**: `(npcUUID, optionIndex)` — no command text.
- The **server resolves** the command from the NPC's saved NBT data.

This means players cannot inject arbitrary commands by modifying packets.

---

## 🗂 Persistence

NPCs save their data (title, text, options, texture) to the world's NBT storage
automatically. They survive restarts, chunk unloads, and server reboots.

---

## 🎨 Custom textures

### Without resource pack (recommended)
Use the `settexturetype` and `setcustomtexture` commands to load textures from:
- **Player skins**: Use any Minecraft player's skin
- **URLs**: Direct links to PNG images (Imgur, etc.)
- **Base64**: Embedded image data

See the commands above for examples.

### With resource pack
Create a resource pack with this structure:

```
my-npc-pack/
└── assets/
    └── dialognpc/
        └── textures/
            └── entity/
                └── nurse_joy.png   ← 64×64 player skin format
```

Then use:
```
/npc settexture <npc> dialognpc:textures/entity/nurse_joy.png
```

---

## 🔧 Extending

| File | Purpose |
|------|---------|
| `DialogNpcEntity.java` | Entity data, NBT, AI goals |
| `DialogScreen.java` | Client GUI rendering |
| `ModPackets.java` | Network packets |
| `DialogCommand.java` | All `/npc` commands |
| `DialogNpcRenderer.java` | How the NPC looks in the world |
