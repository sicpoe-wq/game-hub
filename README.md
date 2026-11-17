# Game Hub

Android game hub (Kotlin + Jetpack Compose).

Playable games:
- **Word Search** — themed letter grids, three difficulties
- **Water Sort** — pour colors between tubes, endless levels
- **Chess** — full legal rules (castling, en passant, promotion, checkmate, stalemate, threefold repetition, 50-move rule, insufficient material). Play vs an offline engine (Easy / Medium / Hard) or local pass-and-play. Tap or drag pieces, undo, move list, captured pieces, optional clock, resign/draw, and save/resume. A Themes screen previews independent board themes and piece sets (Classic Wood, Marble, Tournament Green, Neon Cyber, Enchanted, Sakura, Lumenreach boards; Classic Staunton, Minimal Flat, Neon Glow, Fantasy, and Anime Chibi pieces). Subtle board animations (neon pulse, petals, sparkles, lanterns) can be toggled in Settings, along with coordinates and original CC0 sound effects.
- **Heartbound** — run an adventurer's guild in Lumenreach, and maybe fall for your heroes. v1.9 adds a skippable opening animation (night sky, lanterns, parallax, hero silhouettes, and a glowing HEARTBOUND logo) synced to an original title theme, plus an animated title menu with Continue, Gallery, Settings, and a version label. A Play intro setting turns the animation on or off. v1.8 adds full-screen CG art for confessions, vows, endings, and key plot beats, a gallery that unlocks as those moments are seen, original looping music (hall, evening, battle, romance, ending) and sound effects, and music/SFX volume sliders plus mute in Settings. Audio is original and released under CC0 (see `LICENSE-AUDIO.md`). v1.7 added hall life: decorate the hall and hero rooms, private room visits, forge/library/garden/stables, hireable staff, crafting and enchanting, a traveling merchant with seasonal stock, seasons and weather, hero birthdays, a town map with date locations, a secret late hero (Nim Hale) with her own route, guild endings, and quietly animated portraits. v1.6 added turn-based battles, expeditions, bosses, branching chains, Iron Vesper, bond combos, and reputation. v1.5 added exclusive routes, group events, chapter 1, and fade-to-black cutaways, plus the v1.4 creation and QoL features.

Coming soon: 2048, Snake

Add games in `app/src/main/java/com/tim/gamehub/GameRegistry.kt`.

Build: `./gradlew assembleDebug`
