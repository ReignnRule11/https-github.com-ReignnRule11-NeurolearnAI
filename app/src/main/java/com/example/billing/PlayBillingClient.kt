package com.example.billing

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlayBillingClient(
    private val application: Application,
    private val onPremiumGranted: () -> Unit,
    private val onCoinsGranted: (Int) -> Unit
) : PurchasesUpdatedListener {
    private val TAG = "PlayBillingClient"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectMutex = Mutex()
    private val pendingPurchases = ConcurrentHashMap<String, kotlin.coroutines.Continuation<PlayBillingOutcome>>()

    private val billingClient: BillingClient = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun isPlayBillingAvailable(): Boolean {
        return BuildConfig.PLAY_BILLING_ENABLED && billingClient.isReady
    }

    suspend fun purchasePremium(activity: Activity?): PlayBillingOutcome {
        return launchPurchase(
            activity = activity,
            productId = BillingCatalog.PREMIUM_MAX_MONTHLY,
            productType = BillingClient.ProductType.SUBS
        )
    }

    suspend fun purchaseCoins(activity: Activity?, amount: Int): PlayBillingOutcome {
        return launchPurchase(
            activity = activity,
            productId = BillingCatalog.coinProductId(amount),
            productType = BillingClient.ProductType.INAPP
        )
    }

    suspend fun restorePurchases(): PlayBillingOutcome {
        if (!BuildConfig.PLAY_BILLING_ENABLED) {
            return PlayBillingOutcome.Unavailable
        }
        val connected = ensureConnected()
        if (!connected) {
            return PlayBillingOutcome.Unavailable
        }
        val subs = queryPurchases(BillingClient.ProductType.SUBS)
            ?: return PlayBillingOutcome.Error("Unable to query existing Play subscriptions.")
        val restored = subs.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (restored.isEmpty()) {
            return PlayBillingOutcome.Unavailable
        }
        restored.forEach { grantAndAcknowledge(it) }
        return PlayBillingOutcome.Success
    }

    private suspend fun launchPurchase(
        activity: Activity?,
        productId: String,
        productType: String
    ): PlayBillingOutcome {
        if (!BuildConfig.PLAY_BILLING_ENABLED) {
            return PlayBillingOutcome.Unavailable
        }
        val host = activity ?: return PlayBillingOutcome.Unavailable
        val connected = ensureConnected()
        if (!connected) {
            return PlayBillingOutcome.Unavailable
        }
        val productDetails = queryProductDetails(productId, productType)
            ?: return PlayBillingOutcome.Unavailable
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        if (!offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(offerToken)
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()
        return suspendCancellableCoroutine { continuation ->
            pendingPurchases[productId] = continuation
            continuation.invokeOnCancellation { pendingPurchases.remove(productId) }
            mainHandler.post {
                val result = billingClient.launchBillingFlow(host, flowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    pendingPurchases.remove(productId)
                    if (continuation.isActive) {
                        continuation.resume(mapBillingError(result))
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            completeAll(PlayBillingOutcome.Canceled)
            return
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            completeAll(mapBillingError(result))
            return
        }
        purchases.forEach { purchase ->
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                purchase.products.forEach { productId ->
                    pendingPurchases.remove(productId)?.resume(PlayBillingOutcome.Error("Purchase is pending."))
                }
                return@forEach
            }
            grantAndAcknowledge(purchase)
            purchase.products.forEach { productId ->
                pendingPurchases.remove(productId)?.resume(PlayBillingOutcome.Success)
            }
        }
    }

    private fun grantAndAcknowledge(purchase: Purchase) {
        grantPurchase(purchase)
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { ackResult ->
                if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${ackResult.debugMessage}")
                }
            }
        }
    }

    private fun grantPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        purchase.products.forEach { productId ->
            when (productId) {
                BillingCatalog.PREMIUM_MAX_MONTHLY -> onPremiumGranted()
                else -> BillingCatalog.coinAmountForProduct(productId)?.let(onCoinsGranted)
            }
        }
    }

    private fun completeAll(outcome: PlayBillingOutcome) {
        val waiting = pendingPurchases.values.toList()
        pendingPurchases.clear()
        waiting.forEach { continuation ->
            continuation.resume(outcome)
        }
    }

    private fun mapBillingError(result: BillingResult): PlayBillingOutcome {
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> PlayBillingOutcome.Unavailable
            else -> PlayBillingOutcome.Error(result.debugMessage.ifBlank { "Billing error ${result.responseCode}" })
        }
    }

    private suspend fun ensureConnected(): Boolean = connectMutex.withLock {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Play Billing disconnected")
                }
            })
        }
    }

    private suspend fun queryProductDetails(productId: String, productType: String): ProductDetails? {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(productType)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
                if (!continuation.isActive) return@queryProductDetailsAsync
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resume(null)
                    return@queryProductDetailsAsync
                }
                continuation.resume(productDetailsList.firstOrNull { it.productId == productId })
            }
        }
    }

    private suspend fun queryPurchases(productType: String): List<Purchase>? {
        return suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (!continuation.isActive) return@queryPurchasesAsync
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resume(null)
                    return@queryPurchasesAsync
                }
                continuation.resume(purchases)
            }
        }
    }
}
