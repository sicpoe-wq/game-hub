# Game Hub

Android game hub (Kotlin + Jetpack Compose).

Playable games:
- **Word Search** — themed letter grids, three difficulties
- **Water Sort** — pour colors between tubes, endless levels
- **Heartbound** — run an adventurer's guild in Lumenreach, and maybe fall for your heroes. v1.8 adds full-screen CG art for confessions, vows, endings, and key plot beats, a gallery that unlocks as those moments are seen, original looping music (hall, evening, battle, romance, ending) and sound effects, and music/SFX volume sliders plus mute in Settings. Audio is original and released under CC0 (see `LICENSE-AUDIO.md`). v1.7 added hall life: decorate the hall and hero rooms, private room visits, forge/library/garden/stables, hireable staff, crafting and enchanting, a traveling merchant with seasonal stock, seasons and weather, hero birthdays, a town map with date locations, a secret late hero (Nim Hale) with her own route, guild endings, and quietly animated portraits. v1.6 added turn-based battles, expeditions, bosses, branching chains, Iron Vesper, bond combos, and reputation. v1.5 added exclusive routes, group events, chapter 1, and fade-to-black cutaways, plus the v1.4 creation and QoL features.

Coming soon: 2048, Snake

Add games in `app/src/main/java/com/tim/gamehub/GameRegistry.kt`.

Build: `./gradlew assembleDebug`
