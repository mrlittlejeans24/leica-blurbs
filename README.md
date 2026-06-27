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

There are two backends, selected automatically at check time by
[`RoutingPriceFinder`](app/src/main/java/com/dealwatch/data/remote/RoutingPriceFinder.kt):

1. **Keyless scraping (default, zero setup)** —
   [`WebScrapePriceFinder`](app/src/main/java/com/dealwatch/data/remote/WebScrapePriceFinder.kt)
   searches DuckDuckGo's HTML endpoint, follows the top retailer links, and reads
   each page's price from its structured metadata (schema.org JSON-LD
   `offers.price`, or Open Graph / `itemprop` price tags). Reading a page's own
   structured data is far more accurate than parsing search snippets.

   **Caveat:** this is genuinely best-effort. The sources that expose clean
   structured prices (Google Shopping, eBay) actively block bots, and the ones
   that don't block (DuckDuckGo) can still rate-limit or omit prices. So keyless
   checks can come back incomplete or empty. When that happens the app shows the
   reason on the product card (e.g. "Search blocked — add a free API key").

2. **SerpApi (reliable, recommended)** —
   [`SerpApiPriceFinder`](app/src/main/java/com/dealwatch/data/remote/SerpApiPriceFinder.kt)
   calls SerpApi's Google Shopping engine and gets clean structured JSON (price,
   store, link). Add a free key in **Settings** (the free tier covers ~100
   checks/month — plenty for a few products a day). When a key is set, the app
   uses it; otherwise it falls back to keyless scraping. The key is stored only
   on the device.

Both backends implement one interface,
[`PriceFinder`](app/src/main/java/com/dealwatch/data/remote/PriceFinder.kt), so a
third source (a retailer feed, a different API) is just another implementation
wired into `RoutingPriceFinder`.

## Architecture

Single-module Android app, Kotlin + Jetpack Compose, MVVM.

| Layer | Pieces |
|-------|--------|
| UI | Jetpack Compose + Material 3, Navigation Compose (`ui/`, `MainActivity.kt`) |
| State | `ProductViewModel` exposing Room `Flow`s as Compose state |
| Data | Room (`TrackedProduct`, `PricePoint`, `ProductDao`, `DealDatabase`), `ProductRepository` |
| Price source | `PriceFinder` interface, `RoutingPriceFinder` → `SerpApiPriceFinder` (key) or `WebScrapePriceFinder` (keyless, OkHttp + Jsoup) |
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
