package com.security.careactivator.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.security.careactivator.prefs.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Product IDs — create these in Play Console > Monetize > Products. */
object BillingProducts {
    /** Subscription base plan WITH a 7-day free trial (configure the trial in Console). */
    const val SUBSCRIPTION_ALWAYS_ON = "always_on_monthly"

    /** One-time "lifetime" unlock. */
    const val ONE_TIME_LIFETIME = "lifetime_unlock"
}

/**
 * Thin wrapper over Google Play Billing (billing-ktx). Single source of truth for
 * whether the Pro (Always-on) feature is unlocked. Talks to the Play Store app via
 * IPC — no INTERNET permission required, so it works with this app's offline design.
 *
 * Entitlement is always re-derived from Play (queryPurchasesAsync); the local flag in
 * AppPrefs is only a cache so the UI can render before the first query returns.
 */
class BillingManager(private val context: Context) {

    private val prefs = AppPrefs.get(context)

    private val _isUnlocked = MutableStateFlow(prefs.isProUnlocked)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
            updateUnlocked(purchases)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .build()

    /** Connect to Play and restore any existing entitlement. Idempotent. */
    fun start() {
        if (billingClient.isReady) {
            restorePurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Play Store unavailable; will retry on next start()/foreground.
            }
        })
    }

    private fun restorePurchases() {
        val subs = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(subs) { _, subPurchases ->
            val inapp = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(inapp) { _, oneTimePurchases ->
                updateUnlocked(subPurchases + oneTimePurchases)
            }
        }
    }

    private fun updateUnlocked(purchases: List<Purchase>) {
        val unlocked = purchases.any { it.isEntitlementActive() }
        _isUnlocked.value = unlocked
        prefs.isProUnlocked = unlocked
    }

    private fun Purchase.isEntitlementActive(): Boolean {
        if (purchaseState != Purchase.PurchaseState.PURCHASED) return false
        // During a free trial Play keeps the purchase PURCHASED + autoRenewing, so
        // simply checking membership is enough. Lapsed/cancelled subs drop out here.
        return products.contains(BillingProducts.SUBSCRIPTION_ALWAYS_ON) ||
            products.contains(BillingProducts.ONE_TIME_LIFETIME)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* acknowledged */ }
        }
    }

    fun launchSubscription(activity: Activity) {
        launchProduct(activity, BillingProducts.SUBSCRIPTION_ALWAYS_ON, BillingClient.ProductType.SUBS)
    }

    fun launchLifetime(activity: Activity) {
        launchProduct(activity, BillingProducts.ONE_TIME_LIFETIME, BillingClient.ProductType.INAPP)
    }

    private fun launchProduct(activity: Activity, productId: String, type: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(type)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val product = details.firstOrNull() ?: return@queryProductDetailsAsync

            val detailsParams = if (type == BillingClient.ProductType.SUBS) {
                val offerToken = product.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.offerToken
                    ?: return@queryProductDetailsAsync
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product)
                    .setOfferToken(offerToken)
                    .build()
            } else {
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product)
                    .build()
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(detailsParams))
                .build()
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BillingManager? = null

        fun create(context: Context): BillingManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }

        fun get(): BillingManager =
            INSTANCE ?: error("BillingManager.create() must be called in Application.onCreate()")
    }
}
