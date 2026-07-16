package com.dealplanner.domain

import com.google.common.truth.Truth.assertThat
import com.dealplanner.data.model.DealItem
import com.dealplanner.data.model.PantryItem
import org.junit.Before
import org.junit.Test

class ReceiptReconcilerTest {

    private lateinit var reconciler: ReceiptReconciler

    @Before
    fun setup() {
        reconciler = ReceiptReconciler()
    }

    @Test
    fun `reconcile simple receipt`() {
        val ocrText = """
            CHICKEN BREAST    $8.97
            BROCCOLI CROWNS   $3.98
            TOTAL            $12.95
        """.trimIndent()

        val deals = listOf(
            DealItem(
                name = "Chicken Breast",
                price = 2.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 2.99
            ),
            DealItem(
                name = "Broccoli Crowns",
                price = 1.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 1.99
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.rawLine }).doesNotContain("TOTAL            $12.95")
        assertThat(result.total).isEqualTo(12.95)
    }

    @Test
    fun `calculate VPP for proteins`() {
        val vpp = reconciler.calculateVPP(packagePrice = 10.0, packageWeight = 2.0, servingSize = 0.5)

        assertThat(vpp).isEqualTo(2.5) // $10 / 4 servings = $2.50 per serving
    }

    @Test
    fun `levenshtein distance calculation`() {
        val distance = reconciler.levenshteinDistance("chicken", "chiken")

        assertThat(distance).isEqualTo(1)
    }

    @Test
    fun `fuzzy match receipt line to deal`() {
        val ocrText = "CHIKEN BREST    $8.97"

        val deals = listOf(
            DealItem(
                name = "Chicken Breast",
                price = 2.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 2.99
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].matchedType).isEqualTo("deal")
    }

    @Test
    fun `detect price variance`() {
        val ocrText = """
            CHICKEN BREAST    $12.00
        """.trimIndent()

        val deals = listOf(
            DealItem(
                id = 1,
                name = "Chicken Breast",
                price = 2.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 2.99
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, emptyList(), "Kroger")

        assertThat(result.warnings).isNotEmpty()
    }

    @Test
    fun `mark low confidence items for review`() {
        val ocrText = "XYZABC    $5.99"

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].needsReview).isTrue()
    }

    @Test
    fun `update pantry quantities from receipt`() {
        val ocrText = """
            BLACK BEANS       $1.78
            2 @ $0.89
        """.trimIndent()

        val pantry = listOf(
            PantryItem(
                id = 7,
                item = "Black Beans",
                qty = 2.0,
                unit = "can"
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), pantry, "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].rawLine).isEqualTo("BLACK BEANS       $1.78")
        assertThat(result.receiptItems[0].qty).isEqualTo(2.0)
        assertThat(result.pantryUpdates).isNotEmpty()
        assertThat(result.pantryUpdates[0].qty).isEqualTo(4.0)
        assertThat(result.receiptItems[0].matchedType).isEqualTo("pantry")
        assertThat(result.receiptItems[0].matchedItemId).isEqualTo(7)
    }

    @Test
    fun `parse receipt lines when OCR drops dollar signs`() {
        val ocrText = """
            BLACK BEANS       1.78
            2 @ 0.89
            KROGER PASTA      3.00
            3 @ 1.00
            SUBTOTAL          4.78
            TOTAL             4.78
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "BLACK BEANS       1.78",
            "KROGER PASTA      3.00"
        ).inOrder()
        assertThat(result.receiptItems.map { it.qty }).containsExactly(2.0, 3.0).inOrder()
        assertThat(result.total).isEqualTo(4.78)
    }

    @Test
    fun `parse inline quantity receipt lines without dollar signs`() {
        val ocrText = "2 @ 0.89 BLACK BEANS 1.78"

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].rawLine).isEqualTo("2 @ 0.89 BLACK BEANS 1.78")
        assertThat(result.receiptItems[0].qty).isEqualTo(2.0)
        assertThat(result.receiptItems[0].totalCost).isEqualTo(1.78)
        assertThat(result.total).isEqualTo(1.78)
    }

    @Test
    fun `split weighted quantity line attaches to previous deal and is not imported`() {
        val ocrText = """
            PORK SHOULDER    $12.95
            3.25 lb @ $3.99/lb
        """.trimIndent()
        val deals = listOf(
            DealItem(
                id = 3,
                name = "Pork Shoulder",
                price = 3.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 3.99
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].rawLine).isEqualTo("PORK SHOULDER    $12.95")
        assertThat(result.receiptItems[0].qty).isEqualTo(3.25)
        assertThat(result.total).isEqualTo(12.95)
        assertThat(result.dealMatches).hasSize(1)
        assertThat(result.dealMatches[0].actualPPU).isWithin(0.01).of(3.99)
        assertThat(result.warnings).isEmpty()
    }

    @Test
    fun `receipt pantry match wins over weaker deal match`() {
        val ocrText = "BLACK BEANS       $1.78"
        val deals = listOf(
            DealItem(
                id = 1,
                name = "Chicken Breast",
                price = 2.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 2.99
            )
        )
        val pantry = listOf(
            PantryItem(
                id = 7,
                item = "Black Beans",
                qty = 2.0,
                unit = "can"
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, pantry, "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].matchedType).isEqualTo("pantry")
        assertThat(result.receiptItems[0].matchedItemId).isEqualTo(7)
        assertThat(result.dealMatches).isEmpty()
    }

    @Test
    fun `ignore receipt subtotal tax total and tender lines`() {
        val ocrText = """
            BLACK BEANS      $1.78
            KROGER PASTA     $3.00
            SUBTOTAL         $4.78
            TAX              $0.00
            TOTAL            $4.78
            EBT/CARD         $4.78
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "BLACK BEANS      $1.78",
            "KROGER PASTA     $3.00"
        ).inOrder()
        assertThat(result.total).isEqualTo(4.78)
    }
}
