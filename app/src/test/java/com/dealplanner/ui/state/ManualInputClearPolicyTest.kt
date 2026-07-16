package com.dealplanner.ui.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ManualInputClearPolicyTest {

    @Test
    fun `barcode text clears only after added or updated barcode status`() {
        assertThat(
            ManualInputClearPolicy.forBarcodeStatus(
                status = "Added barcode item with VERIFY checks; no product lookup match found.",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.ClearText)

        assertThat(
            ManualInputClearPolicy.forBarcodeStatus(
                status = "Updated Great Value Black Beans from barcode lookup with VERIFY checks",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.ClearText)
    }

    @Test
    fun `barcode text stays available after no barcode failure`() {
        assertThat(
            ManualInputClearPolicy.forBarcodeStatus(
                status = "No barcode found.",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.StopWaiting)
    }

    @Test
    fun `barcode text keeps waiting during lookup or unrelated status`() {
        assertThat(
            ManualInputClearPolicy.forBarcodeStatus(
                status = "Looking up barcode...",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.None)

        assertThat(
            ManualInputClearPolicy.forBarcodeStatus(
                status = "Added black beans",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.None)
    }

    @Test
    fun `flyer pasted text clears after successful flyer import`() {
        assertThat(
            ManualInputClearPolicy.forFlyerStatus(
                status = "Added 3 flyer deals",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.ClearText)
    }

    @Test
    fun `flyer pasted text stays available after parse failure`() {
        assertThat(
            ManualInputClearPolicy.forFlyerStatus(
                status = "No deals found. Try clearer flyer text.",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.StopWaiting)

        assertThat(
            ManualInputClearPolicy.forFlyerStatus(
                status = "Could not process that flyer text.",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.StopWaiting)
    }

    @Test
    fun `receipt pasted text clears after successful receipt import`() {
        assertThat(
            ManualInputClearPolicy.forReceiptStatus(
                status = "Added 2 receipt items ($3.56) with REVIEW checks",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.ClearText)
    }

    @Test
    fun `receipt pasted text stays available after parse failure`() {
        assertThat(
            ManualInputClearPolicy.forReceiptStatus(
                status = "No receipt line items found. Try a clearer photo or paste OCR text.",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.StopWaiting)

        assertThat(
            ManualInputClearPolicy.forReceiptStatus(
                status = "Could not process that receipt.",
                clearOnSuccess = true
            )
        ).isEqualTo(ManualInputClearDecision.StopWaiting)
    }

    @Test
    fun `clear policy does nothing when clear was not requested`() {
        assertThat(
            ManualInputClearPolicy.forFlyerStatus(
                status = "Added 1 flyer deal",
                clearOnSuccess = false
            )
        ).isEqualTo(ManualInputClearDecision.None)

        assertThat(
            ManualInputClearPolicy.forReceiptStatus(
                status = "Could not process that receipt.",
                clearOnSuccess = false
            )
        ).isEqualTo(ManualInputClearDecision.None)
    }
}
