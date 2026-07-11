# Launcher experiment notes

THEBEST is intentionally not a Nova clone. The third prototype cuts harder: the home screen is not an app warehouse, it is an intention switchboard.

## Research signals I used

- Search-first launchers such as Kvaesitso and KISS reduce mindless opening because the user must state intent before launching.
- One-handed UX guidance keeps primary actions in the thumb zone and avoids important controls stranded at the top edge.
- Material/accessibility guidance favours clear hierarchy, labelled controls, predictable navigation, and touch targets around 48dp or larger.
- Command palettes work when they merge navigation and quick actions without forcing users through settings screens.

## Product stance

A near-perfect launcher should make the phone feel quieter, faster, and more owned by the user. That means fewer surfaces, not more. THEBEST now centres on four ideas:

1. **Intent before inventory**: search and ranked predictions beat a full-screen icon dump.
2. **Reachability**: search, commands, favourites, and first result are thumb-friendly.
3. **Local intelligence**: ranking combines text match, favourites, and recent launches without accounts, network calls, or analytics.
4. **Reversible control**: every app can be pinned, hidden, inspected, or removed from a long-press menu.

## Implemented prototype

- HOME intent registration so Android can offer it as the default launcher.
- Installed-app discovery through `LauncherApps`.
- Accent-insensitive ranking across labels and package names with prefix, contains, and fuzzy subsequence scoring.
- Quiet home mode that shows only guidance, recents/favourites, and predicted essentials instead of a noisy drawer.
- Focused search mode with large ranked result cards and a primary first-result affordance.
- Smart row with recents, command entry, clear search, settings, and launch-first actions.
- Swipe down to focus search; swipe up to enter commands.
- Command palette for theme, hidden apps, Android settings, app settings, full library, and about.
- Long-press app menu for favourites, hiding, app info, and uninstall.
- Hidden-app review flow.
- Accent theme cycling with a matching gradient backdrop.
- Favourites, hidden apps, recents, and accent colour persisted locally.
- No Internet permission.

## Next experiments

- Replace the simple static ranking weights with an editable local scoring model.
- Add edge alphabet scrubbing only inside `:all`, never on the calm home surface.
- Optional local export/import for launcher state.
- Icon-pack support without adopting a large Launcher3 fork.
- Optional widgets as cards below predicted essentials, not as another noisy page.
