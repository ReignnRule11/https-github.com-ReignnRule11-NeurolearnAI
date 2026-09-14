package com.example.production

import com.example.billing.BillingCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionSecretsTest {
    @Test
    fun placeholderGeminiKeysAreRejected() {
        assertFalse(ProductionSecrets.isUsableGeminiKey(null))
        assertFalse(ProductionSecrets.isUsableGeminiKey(""))
        assertFalse(ProductionSecrets.isUsableGeminiKey("MY_GEMINI_API_KEY"))
        assertFalse(ProductionSecrets.isUsableGeminiKey("AIzaSyFakeKeyForTests"))
        assertFalse(ProductionSecrets.isUsableGeminiKey("your-api-key-here"))
    }

    @Test
    fun realLookingGeminiKeysAreAccepted() {
        assertTrue(ProductionSecrets.isUsableGeminiKey("AIzaSyRealConfiguredKeyValue"))
    }

    @Test
    fun coinProductIdsMatchStorePacks() {
        assertEquals(BillingCatalog.COINS_STARTER_100, BillingCatalog.coinProductId(100))
        assertEquals(BillingCatalog.COINS_STANDARD_350, BillingCatalog.coinProductId(350))
        assertEquals(100, BillingCatalog.coinAmountForProduct(BillingCatalog.COINS_STARTER_100))
        assertEquals(350, BillingCatalog.coinAmountForProduct(BillingCatalog.COINS_STANDARD_350))
        assertEquals("premium_max_monthly", BillingCatalog.PREMIUM_MAX_MONTHLY)
    }
}
