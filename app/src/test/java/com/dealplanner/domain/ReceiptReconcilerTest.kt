package com.dealplanner.domain

import com.google.common.truth.Truth.assertThat
import com.dealplanner.data.model.DealItem
import com.dealplanner.data.model.PantryItem
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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
    fun `receipt header date applies to imported receipt items`() {
        val ocrText = """
            KROGER
            Date: 10/27/2025
            Time: 14:32

            BLACK BEANS      $1.78
            KROGER PASTA     $3.00
            TOTAL            $4.78
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.date })
            .containsExactly(LocalDate.of(2025, 10, 27), LocalDate.of(2025, 10, 27))
    }

    @Test
    fun `year first receipt header dates apply to imported receipt items`() {
        val slashDateReceipt = """
            KROGER
            Transaction Date: 2025/10/27
            BLACK BEANS      ${'$'}1.78
        """.trimIndent()
        val dashDateReceipt = """
            KROGER
            Purchase Date: 2025-10-28
            KROGER PASTA     ${'$'}3.00
        """.trimIndent()

        val slashResult = reconciler.reconcileReceipt(slashDateReceipt, emptyList(), emptyList(), "Kroger")
        val dashResult = reconciler.reconcileReceipt(dashDateReceipt, emptyList(), emptyList(), "Kroger")

        assertThat(slashResult.receiptItems).hasSize(1)
        assertThat(slashResult.receiptItems.first().date).isEqualTo(LocalDate.of(2025, 10, 27))
        assertThat(dashResult.receiptItems).hasSize(1)
        assertThat(dashResult.receiptItems.first().date).isEqualTo(LocalDate.of(2025, 10, 28))
    }

    @Test
    fun `normalize store names for imported receipt items`() {
        val ocrText = "BLACK BEANS      $1.78"

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), " Kroger ")
        val unknownResult = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "   ")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems.first().store).isEqualTo("Kroger")
        assertThat(unknownResult.receiptItems).hasSize(1)
        assertThat(unknownResult.receiptItems.first().store).isEqualTo("Unknown")
    }

    @Test
    fun `parse bundled demo receipt for phone checklist`() {
        val ocrText = java.io.File("src/main/assets/demo_receipt.txt").readText()
        val deals = listOf(
            DealItem(
                name = "Pork Shoulder",
                price = 3.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 3.99
            ),
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
            ),
            DealItem(
                name = "Mandarin Oranges",
                price = 3.99,
                unit = "bag",
                dealType = "per_unit",
                store = "Kroger",
                pricePerUnit = 3.99
            )
        )
        val pantry = listOf(
            PantryItem(id = 7, item = "Black Beans", qty = 4.0, unit = "can"),
            PantryItem(id = 8, item = "Kroger Pasta", qty = 1.0, unit = "box"),
            PantryItem(id = 9, item = "Rice", qty = 5.0, unit = "lb"),
            PantryItem(id = 10, item = "Carrots", qty = 2.0, unit = "lb")
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, pantry, "Kroger")

        assertThat(result.receiptItems).hasSize(8)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "PORK SHOULDER    $12.95",
            "CHICKEN BREAST   $8.97",
            "BROCCOLI CROWNS  $3.98",
            "MANDARIN ORANGES $3.99",
            "BLACK BEANS      $1.78",
            "KROGER PASTA     $3.00",
            "RICE 5 LB        $3.99",
            "CARROTS 2LB      $1.99"
        ).inOrder()
        assertThat(result.receiptItems.map { it.date }.distinct()).containsExactly(LocalDate.of(2025, 10, 27))
        assertThat(result.receiptItems.map { it.rawLine }).doesNotContain("EBT/CARD        $40.65")
        assertThat(result.total).isEqualTo(40.65)
        assertThat(result.dealMatches).hasSize(4)
        assertThat(result.pantryUpdates.map { it.item }).containsAtLeast("Black Beans", "Kroger Pasta")
    }

    @Test
    fun `bundled demo receipt ignores appended card tender lines`() {
        val ocrText = java.io.File("src/main/assets/demo_receipt.txt").readText() + """

            VISA DEBIT ${'$'}40.65
            CARD TENDER ${'$'}40.65
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(8)
        assertThat(result.receiptItems.map { it.rawLine }).doesNotContain("VISA DEBIT ${'$'}40.65")
        assertThat(result.receiptItems.map { it.rawLine }).doesNotContain("CARD TENDER ${'$'}40.65")
        assertThat(result.total).isEqualTo(40.65)
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
    fun `match receipt line to fuller flyer deal name by shared food tokens`() {
        val ocrText = "CHICKEN BREAST    $8.97"

        val deals = listOf(
            DealItem(
                id = 22,
                name = "Chicken Breast Boneless Skinless",
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
        assertThat(result.receiptItems[0].matchedItemId).isEqualTo(22)
        assertThat(result.receiptItems[0].confidence).isAtLeast(0.7)
    }

    @Test
    fun `unrelated receipt line does not attach to weakest available deal or pantry item`() {
        val ocrText = "TOOTHPASTE    $3.49"
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
        assertThat(result.receiptItems[0].matchedType).isNull()
        assertThat(result.receiptItems[0].matchedItemId).isNull()
        assertThat(result.receiptItems[0].needsReview).isTrue()
        assertThat(result.dealMatches).isEmpty()
        assertThat(result.pantryUpdates).isEmpty()
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
    fun `accumulate pantry quantity updates for repeated matched receipt items`() {
        val ocrText = """
            BLACK BEANS       ${'$'}1.78
            2 @ ${'$'}0.89
            BLACK BEANS       ${'$'}0.89
            1 @ ${'$'}0.89
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

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.qty }).containsExactly(2.0, 1.0).inOrder()
        assertThat(result.pantryUpdates).hasSize(1)
        assertThat(result.pantryUpdates.first().id).isEqualTo(7)
        assertThat(result.pantryUpdates.first().qty).isEqualTo(5.0)
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
    fun `parse receipt lines when OCR uses comma decimals`() {
        val ocrText = """
            BLACK BEANS       1,78
            2 @ 0,89
            KROGER PASTA      3,00
            3 @ 1,00
            TOTAL             4,78
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "BLACK BEANS       1,78",
            "KROGER PASTA      3,00"
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
    fun `parse item first inline quantity receipt lines`() {
        val ocrText = """
            BLACK BEANS 2 @ 0.89 1.78
            KROGER PASTA 3 @ 1,00 3,00
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "BLACK BEANS 2 @ 0.89 1.78",
            "KROGER PASTA 3 @ 1,00 3,00"
        ).inOrder()
        assertThat(result.receiptItems.map { it.qty }).containsExactly(2.0, 3.0).inOrder()
        assertThat(result.receiptItems.map { it.totalCost }).containsExactly(1.78, 3.0).inOrder()
        assertThat(result.total).isEqualTo(4.78)
    }

    @Test
    fun `parse inline decimal quantity receipt lines`() {
        val ocrText = """
            1.50 @ 0.69 BANANAS 1.04
            1,25 @ 1,99 APPLES 2,49
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)

        val bananas = result.receiptItems.first { it.rawLine.startsWith("1.50") }
        assertThat(bananas.qty).isEqualTo(1.5)
        assertThat(bananas.totalCost).isEqualTo(1.04)

        val apples = result.receiptItems.first { it.rawLine.startsWith("1,25") }
        assertThat(apples.qty).isEqualTo(1.25)
        assertThat(apples.totalCost).isEqualTo(2.49)

        assertThat(result.total).isEqualTo(3.53)
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
    fun `parse inline weighted produce receipt lines`() {
        val ocrText = """
            BANANAS 1.50 lb @ $0.69/lb $1.04
            APPLES 1,25 lb @ 1,99/lb 2,49
        """.trimIndent()
        val deals = listOf(
            DealItem(
                id = 4,
                name = "Bananas",
                price = 0.69,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 0.69
            ),
            DealItem(
                id = 5,
                name = "Apples",
                price = 1.99,
                unit = "lb",
                dealType = "per_pound",
                store = "Kroger",
                pricePerUnit = 1.99
            )
        )

        val result = reconciler.reconcileReceipt(ocrText, deals, emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)

        val bananas = result.receiptItems.first { it.rawLine.startsWith("BANANAS") }
        assertThat(bananas.qty).isEqualTo(1.5)
        assertThat(bananas.totalCost).isEqualTo(1.04)
        assertThat(bananas.matchedType).isEqualTo("deal")
        assertThat(bananas.matchedItemId).isEqualTo(4)

        val apples = result.receiptItems.first { it.rawLine.startsWith("APPLES") }
        assertThat(apples.qty).isEqualTo(1.25)
        assertThat(apples.totalCost).isEqualTo(2.49)
        assertThat(apples.matchedType).isEqualTo("deal")
        assertThat(apples.matchedItemId).isEqualTo(5)

        assertThat(result.total).isEqualTo(3.53)
        assertThat(result.warnings).isEmpty()
    }

    @Test
    fun `parse receipt lines with leading decimal prices`() {
        val ocrText = """
            BANANAS 1.50 lb @ .69/lb 1.04
            1 @ .89 BLACK BEANS .89
            KROGER PASTA .99
            1 @ .99
            TOTAL 2.92
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(3)

        val bananas = result.receiptItems.first { it.rawLine.startsWith("BANANAS") }
        assertThat(bananas.qty).isEqualTo(1.5)
        assertThat(bananas.totalCost).isEqualTo(1.04)

        val blackBeans = result.receiptItems.first { it.rawLine.startsWith("1 @ .89") }
        assertThat(blackBeans.qty).isEqualTo(1.0)
        assertThat(blackBeans.totalCost).isEqualTo(0.89)

        val pasta = result.receiptItems.first { it.rawLine.startsWith("KROGER PASTA") }
        assertThat(pasta.qty).isEqualTo(1.0)
        assertThat(pasta.totalCost).isEqualTo(0.99)

        assertThat(result.receiptItems.map { it.rawLine }).doesNotContain("TOTAL 2.92")
        assertThat(result.total).isEqualTo(2.92)
    }

    @Test
    fun `parse receipt lines with explicit whole dollar prices`() {
        val ocrText = """
            RICE 5 LB        ${'$'}3
            2 @ ${'$'}1 BLACK BEANS ${'$'}2
            KROGER PASTA     ${'$'}3
            3 @ ${'$'}1
            BANANAS 1 lb @ ${'$'}1/lb ${'$'}1
            TOTAL            ${'$'}9
            KROGER FLOUR 5 LB
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(4)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "RICE 5 LB        ${'$'}3",
            "2 @ ${'$'}1 BLACK BEANS ${'$'}2",
            "KROGER PASTA     ${'$'}3",
            "BANANAS 1 lb @ ${'$'}1/lb ${'$'}1"
        ).inOrder()
        assertThat(result.receiptItems.map { it.totalCost }).containsExactly(3.0, 2.0, 3.0, 1.0).inOrder()
        assertThat(result.receiptItems.map { it.qty }).containsExactly(null, 2.0, 3.0, 1.0).inOrder()
        assertThat(result.total).isEqualTo(9.0)
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
            VISA DEBIT       $4.78
            CARD TENDER      $4.78
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(2)
        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "BLACK BEANS      $1.78",
            "KROGER PASTA     $3.00"
        ).inOrder()
        assertThat(result.total).isEqualTo(4.78)
    }

    @Test
    fun `ignore snap ebt and wic tender lines`() {
        val ocrText = """
            BLACK BEANS      $1.78
            SNAP EBT         $1.78
            EBT FOOD         $1.78
            WIC BENEFIT      $1.78
            TOTAL            $1.78
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems).hasSize(1)
        assertThat(result.receiptItems[0].rawLine).isEqualTo("BLACK BEANS      $1.78")
        assertThat(result.total).isEqualTo(1.78)
    }

    @Test
    fun `ignore coupon discount refund and reward lines`() {
        val ocrText = """
            BLACK BEANS      $1.78
            MFR COUPON      -$1.00
            STORE DISCOUNT   $0.50
            DIGITAL COUPON   0.25
            REWARDS SAVINGS  0.75
            YOU SAVED        $4.25
            SAVED TODAY      4.25
            TOTAL SAVED      $4.25
            REFUND          -2.00
            KROGER PASTA     $3.00
            TOTAL            $3.53
        """.trimIndent()

        val result = reconciler.reconcileReceipt(ocrText, emptyList(), emptyList(), "Kroger")

        assertThat(result.receiptItems.map { it.rawLine }).containsExactly(
            "BLACK BEANS      $1.78",
            "KROGER PASTA     $3.00"
        ).inOrder()
        assertThat(result.total).isEqualTo(4.78)
    }
}
