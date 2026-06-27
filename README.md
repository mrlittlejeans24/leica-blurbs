# DealWatch

An Android app to track products you want and get a notification when the price
hits a **new all-time low**. You type in a product, and the app checks the web
for the best price once a day in the background — no account, no server, no API
keys required.

## What it does

- **Add products to track** — type what you want (e.g. "Sony WH-1000XM5 headphones")
  and optionally a target price you're hoping for.
- **Daily price checks** — a background job (Android `WorkManager`) searches the
  web roughly once a day, whenever the device has a network connection, and
  records the best price it finds.
- **Deal alerts** — when the best price found drops below the lowest price ever
  recorded for that product, you get a high-priority notification. Tapping it
  opens the offer in your browser. The first check just establishes a baseline,
  so you won't get a noisy alert the moment you add something.
- **Price history** — each product has a detail screen with a sparkline of its
  price over time, the current best offer, the lowest ever seen, and a toggle to
  mute alerts per product.

## How price-finding works (and its limits)

By design there is **no backend and no API key**. The app fetches public web
search (shopping) results directly on the device and parses prices out of the
HTML.

This keeps the app free and self-contained, but **HTML scraping is inherently
fragile**: search providers change their markup and may rate-limit or block
automated requests, so results can be incomplete or temporarily dry up. The
parser is written defensively (it scans for price-shaped tokens rather than
relying on specific CSS classes) so it degrades to "fewer/no results" instead of
crashing.

If you want more reliable results later, everything goes through one interface,
[`PriceFinder`](app/src/main/java/com/dealwatch/data/remote/PriceFinder.kt). Drop
in an implementation backed by a paid price API (e.g. SerpApi or a retailer
feed), wire it into
[`ProductRepository`](app/src/main/java/com/dealwatch/data/ProductRepository.kt),
and nothing else has to change.

## Architecture

Single-module Android app, Kotlin + Jetpack Compose, MVVM.

| Layer | Pieces |
|-------|--------|
| UI | Jetpack Compose + Material 3, Navigation Compose (`ui/`, `MainActivity.kt`) |
| State | `ProductViewModel` exposing Room `Flow`s as Compose state |
| Data | Room (`TrackedProduct`, `PricePoint`, `ProductDao`, `DealDatabase`), `ProductRepository` |
| Price source | `PriceFinder` interface + `WebScrapePriceFinder` (OkHttp + Jsoup) |
| Background | `PriceCheckWorker` + `PriceCheckScheduler` (WorkManager, daily, network-constrained) |
| Notifications | `DealNotifier` (notification channel + deal alerts) |

The **"good deal" rule** lives in `ProductRepository.checkProduct()`: an alert
fires only on a strict new all-time low.

## Building

Requires the Android SDK (API 35 / build-tools 35) and JDK 17. Easiest path is
to open the project in **Android Studio** and let it sync, then Run.

From the command line:

```bash
# point Gradle at your SDK (Android Studio writes this for you)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

./gradlew assembleDebug        # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # build + install on a connected device/emulator
```

Both `assembleDebug` and `assembleRelease` (with R8 minification) are verified to
build cleanly.

### Permissions

- `INTERNET` / `ACCESS_NETWORK_STATE` — to fetch prices.
- `POST_NOTIFICATIONS` — requested at runtime on first launch (Android 13+) so
  deal alerts can be shown.

## Notes & possible next steps

- WorkManager schedules the check ~daily and batches it for battery efficiency,
  so the exact run time isn't guaranteed to the minute — that's expected Android
  background behavior. Use the refresh button in the top bar to check on demand.
- Product images/thumbnails, multi-store comparison in the UI, and a configurable
  check frequency are natural follow-ons.
- Swapping `WebScrapePriceFinder` for an API-backed finder is the single biggest
  reliability upgrade.
