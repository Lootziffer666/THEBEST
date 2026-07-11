# THEBEST Launcher

THEBEST is an experimental Android launcher built around one question: what if the home screen stopped being a warehouse and became an intention switchboard?

## What it does now

- Registers as an Android `HOME` launcher.
- Discovers launchable apps through `LauncherApps` without requesting Internet access.
- Searches app names and package names locally with accent-insensitive, ranked matching.
- Shows a quiet home with recents, favourites, predictions, and a thumb-friendly search surface instead of an always-on icon dump.
- Turns search results into large, readable launch cards with the best match promoted as the primary action.
- Supports gestures: swipe down to search, swipe up to commands.
- Long-presses any app for pin/unpin, hide/unhide, app info, or uninstall.
- Provides local commands: `:theme`, `:hidden`, `:settings`, `:apps`, `:all`, `:backup`, `:reset`, and `:about`.
- Supports search lenses: `#fav` for pinned apps and `#recent` for launch history.
- Lets the keyboard GO action launch the best ranked match.
- Persists favourites, hidden apps, recents, and accent colour locally in `SharedPreferences`, with a clipboard backup command.

## Philosophy

No account. No feed. No ads. No analytics. No Internet permission. THEBEST should feel like a fast personal tool, not a content surface that owns the user.

## Build

```bash
gradle :app:assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`, press Home, and pick THEBEST.
