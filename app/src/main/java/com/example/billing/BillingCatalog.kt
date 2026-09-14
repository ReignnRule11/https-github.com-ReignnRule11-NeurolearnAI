package com.example.billing

object BillingCatalog {
    const val PREMIUM_MAX_MONTHLY = "premium_max_monthly"
    const val COINS_STARTER_100 = "coins_starter_100"
    const val COINS_STANDARD_350 = "coins_standard_350"

    fun coinProductId(amount: Int): String {
        return when (amount) {
            100 -> COINS_STARTER_100
            350 -> COINS_STANDARD_350
            else -> "coins_pack_$amount"
        }
    }

    fun coinAmountForProduct(productId: String): Int? {
        return when (productId) {
            COINS_STARTER_100 -> 100
            COINS_STANDARD_350 -> 350
            else -> productId.removePrefix("coins_pack_").toIntOrNull()
        }
    }
}

sealed class PlayBillingOutcome {
    data object Success : PlayBillingOutcome()
    data object Unavailable : PlayBillingOutcome()
    data object Canceled : PlayBillingOutcome()
    data class Error(val message: String) : PlayBillingOutcome()
}
