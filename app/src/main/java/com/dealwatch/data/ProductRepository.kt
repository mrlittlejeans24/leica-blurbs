package com.dealwatch.data

import android.content.Context
import com.dealwatch.data.remote.PriceFinder
import com.dealwatch.data.remote.RoutingPriceFinder
import kotlinx.coroutines.flow.Flow

/** Result of a check that crossed the alert threshold (a new all-time low). */
data class DealAlert(
    val product: TrackedProduct,
    val newPrice: Double,
    val previousLowest: Double,
    val store: String?,
    val url: String?,
)

class ProductRepository(
    private val dao: ProductDao,
    private val finder: PriceFinder,
) {
    fun observeProducts(): Flow<List<TrackedProduct>> = dao.observeProducts()
    fun observeProduct(id: Long): Flow<TrackedProduct?> = dao.observeProduct(id)
    fun observePriceHistory(id: Long): Flow<List<PricePoint>> = dao.observePriceHistory(id)

    suspend fun addProduct(query: String, targetPrice: Double?): Long =
        dao.insert(TrackedProduct(query = query.trim(), targetPrice = targetPrice))

    suspend fun deleteProduct(id: Long) = dao.delete(id)

    suspend fun setNotificationsEnabled(product: TrackedProduct, enabled: Boolean) =
        dao.update(product.copy(notificationsEnabled = enabled))

    /** Run a price check for every tracked product. Returns the alerts that fired. */
    suspend fun checkAllProducts(): List<DealAlert> =
        dao.getAllProducts().mapNotNull { checkProduct(it) }

    /**
     * Fetches current prices for one product, records the best one in history,
     * updates the product's snapshot, and decides whether this is a deal.
     *
     * Deal rule: a **new all-time low** — the best price found is strictly below
     * the lowest price ever recorded for this product. The very first successful
     * check only establishes the baseline and never alerts.
     */
    suspend fun checkProduct(product: TrackedProduct): DealAlert? {
        val lookup = finder.findPrices(product.query)
        val best = lookup.results.minByOrNull { it.price }
        val now = System.currentTimeMillis()

        if (best == null) {
            dao.update(product.copy(lastCheckedAt = now, lastCheckStatus = lookup.status))
            return null
        }

        dao.insertPricePoint(
            PricePoint(
                productId = product.id,
                price = best.price,
                store = best.store,
                url = best.url,
            ),
        )

        val previousLowest = product.lowestEverPrice
        val isNewLow = previousLowest != null && best.price < previousLowest
        val newLowestEver = minOf(previousLowest ?: best.price, best.price)

        dao.update(
            product.copy(
                lastCheckedAt = now,
                currentPrice = best.price,
                currentStore = best.store,
                currentUrl = best.url,
                currency = best.currency,
                lowestEverPrice = newLowestEver,
                lastCheckStatus = lookup.status,
            ),
        )

        return if (isNewLow && product.notificationsEnabled) {
            DealAlert(
                product = product,
                newPrice = best.price,
                previousLowest = previousLowest!!,
                store = best.store,
                url = best.url,
            )
        } else {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: ProductRepository? = null

        fun get(context: Context): ProductRepository =
            instance ?: synchronized(this) {
                instance ?: ProductRepository(
                    dao = DealDatabase.get(context).productDao(),
                    finder = RoutingPriceFinder(context.applicationContext),
                ).also { instance = it }
            }
    }
}
