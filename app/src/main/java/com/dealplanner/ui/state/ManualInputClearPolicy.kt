package com.dealplanner.ui.state

enum class ManualInputClearDecision {
    None,
    ClearText,
    StopWaiting
}

object ManualInputClearPolicy {
    fun forBarcodeStatus(
        status: String?,
        clearOnSuccess: Boolean
    ): ManualInputClearDecision {
        if (!clearOnSuccess) return ManualInputClearDecision.None

        val value = status.orEmpty()
        val importSucceeded = value.startsWith("Added ") || value.startsWith("Updated ")
        return when {
            importSucceeded && value.contains("barcode", ignoreCase = true) -> ManualInputClearDecision.ClearText
            value == "No barcode found." -> ManualInputClearDecision.StopWaiting
            else -> ManualInputClearDecision.None
        }
    }

    fun forFlyerStatus(
        status: String?,
        clearOnSuccess: Boolean
    ): ManualInputClearDecision {
        return forTextImportStatus(status, clearOnSuccess, successNeedle = "flyer deal")
    }

    fun forReceiptStatus(
        status: String?,
        clearOnSuccess: Boolean
    ): ManualInputClearDecision {
        return forTextImportStatus(status, clearOnSuccess, successNeedle = "receipt item")
    }

    private fun forTextImportStatus(
        status: String?,
        clearOnSuccess: Boolean,
        successNeedle: String
    ): ManualInputClearDecision {
        if (!clearOnSuccess) return ManualInputClearDecision.None

        val value = status.orEmpty()
        return when {
            value.startsWith("Added ") && value.contains(successNeedle) -> ManualInputClearDecision.ClearText
            value.startsWith("No ") || value.startsWith("Could not") -> ManualInputClearDecision.StopWaiting
            else -> ManualInputClearDecision.None
        }
    }
}
