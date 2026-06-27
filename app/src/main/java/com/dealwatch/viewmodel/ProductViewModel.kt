package com.dealwatch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dealwatch.data.PricePoint
import com.dealwatch.data.ProductRepository
import com.dealwatch.data.SettingsStore
import com.dealwatch.data.TrackedProduct
import com.dealwatch.work.PriceCheckScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProductRepository.get(app)

    val products: StateFlow<List<TrackedProduct>> =
        repo.observeProducts().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun product(id: Long): Flow<TrackedProduct?> = repo.observeProduct(id)
    fun priceHistory(id: Long): Flow<List<PricePoint>> = repo.observePriceHistory(id)

    fun addProduct(query: String, targetPrice: Double?) {
        if (query.isBlank()) return
        viewModelScope.launch {
            repo.addProduct(query, targetPrice)
            // Kick off an immediate baseline check so the user sees a price fast.
            PriceCheckScheduler.checkNow(getApplication())
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch { repo.deleteProduct(id) }
    }

    fun setNotifications(product: TrackedProduct, enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(product, enabled) }
    }

    fun checkNow() {
        PriceCheckScheduler.checkNow(getApplication())
    }

    fun getApiKey(): String = SettingsStore.getSerpApiKey(getApplication()).orEmpty()

    fun saveApiKey(key: String) {
        SettingsStore.setSerpApiKey(getApplication(), key)
        // Re-check everything immediately so the new key takes effect visibly.
        PriceCheckScheduler.checkNow(getApplication())
    }
}
