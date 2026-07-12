# PLAN: THEBEST → der beste Launcher, den AI je gemacht hat

Dieser Plan baut auf dem vorhandenen Prototyp auf (suchzentrierter „Intention Launcher",
eine Activity, ~800 Zeilen Java, kein Internet, keine Analytics) und beschreibt den Weg
zu einem Launcher, der in Geschwindigkeit, lokaler Intelligenz und Ruhe alles schlägt,
was es gibt — ohne die Kernidentität zu verraten: **kein Konto, kein Feed, keine Werbung,
keine Internet-Permission.**

---

## 0. Nordstern & Messlatte

Die Vision in einem Satz: *Der Launcher errät, was du willst, bevor du tippst — und wenn
er rät, rät er lokal.*

Messbare Ziele (jede Phase wird daran gemessen):

| Metrik | Ziel |
|---|---|
| Kaltstart bis interaktiv | < 400 ms auf Mittelklasse-Gerät |
| Suche → Launch | ≤ 2 Interaktionen (tippen + GO) für 95 % der Launches |
| Trefferquote der Top-Vorhersage (leere Suche) | > 60 % nach 2 Wochen Nutzung |
| APK-Größe | < 3 MB |
| Netzwerkzugriffe | exakt 0 (per Manifest garantiert, per Test verifiziert) |
| Jank auf der Home-Oberfläche | 0 übersprungene Frames beim Öffnen |

---

## 1. Ist-Zustand: ehrliche Bestandsaufnahme

**Stärken (behalten):**
- Suchzentrierte Philosophie, Command-Palette (`:theme`, `:all`, …), Such-Linsen (`#fav`, `#recent`)
- Long-Press-Menü (Pin/Hide/Info/Uninstall), lokale Persistenz, Accent-Themes
- Keine Internet-Permission, `allowBackup=false`, komplett offline

**Schwächen (die Roadmap adressiert jede davon):**
1. **Kein Gradle-Wrapper, kein CI** — Builds sind nicht reproduzierbar, kein PR wird verifiziert.
2. **`reloadApps()` synchron in `onResume()`** — bei 300+ Apps blockiert jeder Home-Druck den Main-Thread. Es gibt keinen `LauncherApps.Callback` für Paket-Änderungen.
3. **Monolithische `MainActivity` (612 Zeilen)** — UI, Datenzugriff, Gesten, Commands und Dialoge in einer Klasse; programmatisches UI ohne Layout-Ressourcen.
4. **Listen ohne Recycling** — `LinearLayout` + `ScrollView` statt `RecyclerView`; `:all` mit vielen Apps wird träge und speicherhungrig.
5. **Ranking ist statisch** — feste Gewichte, Recents nur als 8er-Set ohne Häufigkeit, Zeit oder Kontext. „Local brain" ist bisher ein Slogan, kein Modell.
6. **Persistenz fragil** — `SharedPreferences` mit `\n`-Joins; kein Schema, keine Migration, Backup nur als Klartext in die Zwischenablage.
7. **Null Tests** — auch `SearchRanker` und `LauncherStore`, die perfekt testbar wären.
8. **Kein Work-Profile-, Multi-User- oder Shortcut-Support**, keine dynamische Farbgebung (Monet), kein helles Theme, keine Lokalisierung (UI ist englisch, Nutzer deutsch).

---

## 2. Roadmap in sechs Phasen

### Phase 1 — Fundament (das Unsichtbare zuerst)

*Ziel: reproduzierbare Builds, Sicherheitsnetz, saubere Architektur — Voraussetzung für alles Weitere.*

- Gradle-Wrapper einchecken (`gradle wrapper`), Version-Catalog (`libs.versions.toml`).
- GitHub Actions: `assembleDebug` + Unit-Tests + Lint auf jedem Push/PR.
- **Kotlin-Migration** der vier Klassen (Java → Kotlin ist hier ein Nachmittag, zahlt sich in
  jeder weiteren Zeile aus). UI bleibt zunächst View-basiert und schlank — Compose ist für
  einen Launcher mit <3 MB-Ziel und Kaltstart-Budget bewusst *nicht* Phase 1 (Neubewertung in Phase 4).
- Architektur-Schnitt in Module/Pakete:
  - `core/apps` — App-Repository mit `LauncherApps.Callback` (Paket installiert/entfernt/geändert → gezieltes Update statt Voll-Reload), Laden auf Hintergrund-Thread, In-Memory-Cache.
  - `core/store` — Persistenz hinter Interface (Start: SharedPreferences; Phase 3: Room/SQLite für Launch-Events).
  - `core/rank` — Ranker als reine, testbare Funktion (ist er fast schon).
  - `ui/` — Home, Suche, Long-Press, Commands als getrennte Controller.
- Unit-Tests für `SearchRanker` (Prefix/Contains/Subsequenz/Umlaute/Leere Query) und `LauncherStore` (Ordnung, Toggle, Reset). Robolectric für das App-Repository.
- `RecyclerView` mit `ListAdapter`/`DiffUtil` für Ergebnisliste und `:all`.
- Icon-Pipeline: Icons asynchron laden, Memory-LRU-Cache, korrekte Dichte.

**Definition of Done:** CI grün, Kaltstart gemessen (Macrobenchmark), kein Frame-Drop beim Resume mit 400 installierten Apps (Emulator-Test).

### Phase 2 — Suche, die sich telepathisch anfühlt

*Ziel: Die Suche ist der Kern des Produkts — sie muss jede Konkurrenz schlagen.*

- **Matching v2:** Initialen-Matching („gm" → *G*oogle *M*aps), gebundene Levenshtein-Distanz ≤ 1 für Tippfehler, Kamel-/Wortgrenzen-Bonus, Normalisierung inkl. Transliteration (ß→ss, é→e — Basis existiert).
- **Mehr Quellen, weiterhin 100 % offline:**
  - App-Shortcuts (`LauncherApps.getShortcuts`) — „whatsapp anna" springt direkt in den Chat.
  - Settings-Deep-Links (WLAN, Bluetooth, Hotspot … als durchsuchbare Aktionen).
  - Inline-Rechner & Einheiten-Umrechnung direkt im Suchfeld (`= 19*4`, `3 mi in km`).
  - Optional & permission-gated: Kontakte-Linse `#kontakt` (nur wenn der Nutzer die Permission explizit erteilt; Standard bleibt permission-frei).
- **Such-Linsen ausbauen:** `#hidden`, `#work` (Work-Profile), `#neu` (frisch installiert).
- Keyboard-first perfektionieren: GO startet Top-Treffer (existiert), lange GO-Taste startet Treffer 2, Ziffern 1–9 als optionale Schnellwahl in den Ergebnissen.

### Phase 3 — Das lokale Gehirn (der „AI"-Teil, ohne Cloud)

*Ziel: „local brain" vom Slogan zum Feature machen. Alles on-device, erklärbar, abschaltbar.*

- **Launch-Event-Log in Room:** `(app, timestamp, wochentag, stundenslot, vorher_gestartete_app)` — ersetzt das 8er-Recents-Set. Aufbewahrung begrenzt (z. B. 90 Tage), `:reset` löscht alles.
- **Ranking-Modell v1 (regelbasiert, erklärbar):**
  - Frecency: Häufigkeit × exponentieller Zeit-Decay (bewährt: Firefox-Frecency).
  - Zeit-Kontext: getrennte Zähler pro Wochentag-Typ × Tageszeit-Bucket („morgens 7–9 Uhr werktags → Bahn-App oben").
  - Sequenz-Kontext: Markov-Übergang erster Ordnung („nach Kamera kommt meist Galerie").
- **Ranking-Modell v2 (lernend, weiterhin on-device):** kleine logistische Regression über dieselben Features, online trainiert bei jedem Launch. Kein TensorFlow-Ballast — das sind <100 Zeilen Kotlin und bleibt unter dem APK-Budget.
- **Transparenz als Feature:** Command `:warum` zeigt pro Vorschlag die Score-Zusammensetzung. Kein anderer Launcher traut sich das — es passt exakt zur Philosophie „owned by the user".
- Editierbare Gewichte via `:tune` (im DESIGN.md bereits als Experiment notiert).
- **Predicted Essentials** auf der Home-Fläche speisen sich aus diesem Modell statt aus dem statischen Score.

### Phase 4 — Oberfläche: ruhig, schön, zugänglich

*Ziel: Das ruhigste UI im Launcher-Markt — aber poliert statt spartanisch.*

- **Material You / Monet:** Accent-Farbe optional aus dem Wallpaper (`android:theme` dynamisch), das bestehende manuelle Accent-Cycling bleibt als Override. Helles + dunkles Theme, folgt dem System.
- Themed/Monochrome Icons (Android 13+) mit Fallback.
- Layout-Ressourcen statt reinem Code-UI dort, wo es Wartbarkeit bringt; Neubewertung Compose: nur wenn Kaltstart- und Größen-Budget nachweislich halten (Baseline Profiles einplanen).
- Bewegung: dezente, unterbrechbare Animationen (Springs), Haptik konsistent (existiert punktuell).
- **Accessibility als Release-Kriterium:** TalkBack-Durchlauf, 48-dp-Ziele (Menü-Pill ist aktuell 56×48 — prüfen), Font-Scaling bis 200 %, Kontrast-Check der Accent-Paare, RTL (deklariert, nie getestet).
- **Lokalisierung:** Deutsch + Englisch vollständig; Strings raus aus dem Code in `strings.xml`.
- Edge-Alphabet-Scrubbing nur in `:all` (wie in DESIGN.md geplant), nie auf der Home-Fläche.
- Optionale Widgets als Karten unter den Predicted Essentials (`AppWidgetHost`) — opt-in, maximal 2, damit die Ruhe bleibt.

### Phase 5 — Power-Features & Robustheit

- **Work-Profile:** `UserHandle`-korrektes Launchen (aktuell `Process.myUserHandle()`-only), Badge, `#work`-Linse.
- **Icon-Packs** ohne Launcher3-Fork: Standard-`appfilter.xml`-Parser (das Format ist de facto standardisiert und klein).
- **Backup/Restore als Datei** via Storage Access Framework (JSON mit Schema-Version) — ersetzt den Clipboard-Export; Import inklusive.
- App-Umbenennen & freie Tags (Tags werden Such-Linsen: `#reisen`).
- Geschützter Hidden-Space: `:hidden` optional hinter `BiometricPrompt`.
- Gesten konfigurierbar (Swipe-Ziele frei belegbar), Double-Tap-to-Lock via Accessibility-Service (opt-in, sauber erklärt).
- Fehlertoleranz: App deinstalliert während Suche offen, Profil gesperrt, leerer App-Katalog beim Erststart — alles definierte Zustände.

### Phase 6 — Qualität & Release

- Macrobenchmark (Kaltstart), Baseline Profiles, LeakCanary im Debug-Build.
- Screenshot-Tests für Home/Suche/`:all` in hell/dunkel/DE/EN.
- **Privacy-Beweis statt Privacy-Versprechen:** CI-Check, der das Merged Manifest auf `INTERNET`-Permission-Freiheit prüft und fehlschlägt, wenn je eine Dependency sie einschleppt.
- Release-Pipeline: signierte Builds, `versionCode`-Automatik, Changelogs, **F-Droid** als primärer Kanal (passt zur Philosophie; reproducible build anstreben), GitHub Releases als zweiter.
- README/Store-Texte + Screenshots (fastlane-Struktur), Onboarding-Screen der die Philosophie in 3 Karten erklärt.

---

## 3. Reihenfolge & Abhängigkeiten

```
Phase 1 (Fundament) ──► Phase 2 (Suche) ──► Phase 3 (Gehirn) ──► Phase 6 (Release)
        │                                        ▲
        └──► Phase 4 (UI) ──► Phase 5 (Power) ───┘
```

- Phase 1 ist nicht verhandelbar und blockiert alles.
- Phase 2 und 4 können danach parallel laufen; Phase 3 braucht das Event-Log aus Phase 1/2.
- Nach jeder Phase ein installierbarer, im Alltag benutzbarer Stand (der Launcher wird ab Phase 1 täglich selbst genutzt — Dogfooding ist der wichtigste Test).

## 4. Was wir bewusst NICHT bauen

Genauso wichtig wie die Roadmap:

- Kein App-Drawer mit Seiten und Ordnern — `:all` + Suche ersetzt das.
- Keine Cloud-Sync, kein „AI-Assistent" mit Netzanbindung, keine Feeds/News-Panels.
- Keine Notification-Badges über `NotificationListenerService` — der Preis (Zugriff auf alle Benachrichtigungsinhalte) widerspricht der Philosophie.
- Kein Launcher3-Fork — die Codebasis bleibt klein genug, dass eine Person sie vollständig versteht.

## 5. Nächster konkreter Schritt

Phase 1, Ticket 1: Gradle-Wrapper + GitHub-Actions-CI + erste `SearchRanker`-Unit-Tests.
Das ist ein einzelner PR, macht jeden weiteren PR verifizierbar und verändert null Produktverhalten.
