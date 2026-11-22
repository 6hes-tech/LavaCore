# LavaCore

Core plugin for lavacast.cc. Fork of [AnarchyCore](https://github.com/banimania/AnarchyCore) by danimania,
rewritten for Paper/Pufferfish/Purpur 1.21.10 (Java 21).

## Build

```sh
doas apt install openjdk-21-jdk maven
cd LavaCore
mvn package
```

The jar ends up at `target/LavaCore-1.0.3.jar`. Drop it in `plugins/`. Remove the old AnarchyCore jar first,
they register the same commands. LavaCore uses its own folder (`plugins/LavaCore/`), so the old config isn't read.

## 1.0.3
* /stats shows the server name, creation date, days since creation, total world size and how many players
  have ever joined (2b2t style). All text and colors are in config, world size refreshes in the background.

## 1.0.2
* Greentext (`>` turns green) and orangetext (`<` turns orange), prefixes and colors in config.
* Chat antispam: cooldown and burst limit, warns on the first block, kicks if they keep going right after.
  Bypass with `lavacore.bypassspam`.

## 1.0.1
* Added /msg (tell, w, whisper, m, pm) and /r (reply). Optional: muted players can't whisper you (`whispers.blocked-by-mute`).
* Eyes of ender can go into end portal frames again (the block ban checked the frame block instead of the item, and deleted the eyes).
* Nether roof and bottom each have a `mode`: teleport, kill or free.
* New config options get added to an existing config.yml automatically.

## What changed from AnarchyCore

### Requested
* **Random spawn fixed.** Never spawns inside blocks, in water/lava, on leaves, cactus, magma, campfires,
  powder snow, fences, etc. Needs solid ground plus 2 free blocks. Falls back to a safe spot at world spawn
  if it can't find one. Leaving the End through the exit portal no longer random spawns you.
* **/ignore is now /mute, /unmute, /mutelist.** Personal mutes (only hides chat from you), saved to
  `data.yml` so they survive restarts. Works for offline players who've joined before.
* **Piston dupe removed.**
* **2b2t style death messages.** Recolors the vanilla death message (victim, killer, weapon, text each
  get their own color in config), so every death type works and item hover still works.
* **Auto restart** every 24h after startup (or daily at a fixed time) with chat warnings.
* **Built against the Paper 1.21.10 API**, Java 21, `api-version: '1.21'` (no more legacy material mode).

### Bugs fixed
* /ignore never worked: its chat listener was commented out and the map was read with the wrong key types.
* Opening a hopper with any illegal item **duped your whole inventory** (dropped a copy of every stack,
  then set each to 1). Now it just deletes the illegal items.
* Anyone could use `&` color codes and PlaceholderAPI `%placeholders%` in chat (every message went through
  the config formatter). Chat now goes through Paper's normal pipeline.
* 32k patch cancelled any hit over 40 damage, which kills mace smashes. Now it only cancels hits with an
  overenchanted weapon.
* Anti dispenser crash used 1.12 heights (y < 5), breaking every dispenser in deepslate. Also blocked all
  flint and steel dispensers. Now uses the world's real build limits; flint and steel blocking is optional.
* Nether roof teleported you 5 blocks down into the bedrock ceiling (suffocation), reset your camera,
  only worked on a world named `world_nether`, and ignored boats/horses. Now finds the nearest safe spot.
* /toggledeathmsgs and /togglejoinmsgs threw ConcurrentModificationException when disabled and were lost
  on relog. Now saved per UUID.
* /nc was lost on relog; `strike` and `italic` actually applied magic text. Now saved, and supports
  a color plus formats (`/nc red bold`).
* /ping used NMS reflection that's broken on modern versions.
* Every command crashed with ClassCastException when run from console.
* Empty MOTD list crashed the ping handler.
* End portal frame / spawner block options were never checked; blocked items were deleted from the main hand
  even when placed from the off hand.
* Welcome message only showed if custom join messages were enabled.
* Salc1 "patch" wiped legit chest donkeys and the dupe it targeted doesn't exist on 1.21; now off by default.
* Removed bStats (it reported under the original author's plugin id) and the javafx import that broke builds.
* Command whitelist can't be bypassed with namespaced commands, and non-whitelisted commands are hidden
  from tab completion.

## Permissions

| Permission | Default | What |
|---|---|---|
| lavacore.nc | everyone | /nc colors |
| lavacore.nc.format | op | /nc formats |
| lavacore.reload | op | /corereload |
| lavacore.bypasswhitelist | op | skip command whitelist |
| lavacore.bypassillegals | op | skip illegal check and 32k patch |
| lavacore.bypassblocks | op | place bedrock etc |
| lavacore.bypassspam | op | skip chat antispam |
| lavacore.bypassnether | op | nether roof/bottom |

## Auto restart note

`method: restart` uses `restart-script` in spigot.yml, and just stops the server if that isn't set up.
On a hosting panel, whoever controls the panel needs auto restart on stop enabled, or the server stays down.
