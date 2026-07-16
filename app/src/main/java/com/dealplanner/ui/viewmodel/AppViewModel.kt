package com.dealplanner.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.graphics.pdf.PdfRenderer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dealplanner.ai.GeminiPantryVisionClient
import com.dealplanner.data.database.AppDatabase
import com.dealplanner.data.model.*
import com.dealplanner.data.repository.AppRepository
import com.dealplanner.domain.*
import com.dealplanner.lookup.OpenFoodFactsBarcodeClient
import com.dealplanner.lookup.OpenFoodFactsBarcodeClient.BarcodeLookupResult
import com.dealplanner.ocr.TextRecognitionHelper
import com.dealplanner.parser.DealsParser
import com.dealplanner.parser.PantryPhraseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = AppRepository(
        pantryDao = database.pantryDao(),
        dealDao = database.dealDao(),
        receiptDao = database.receiptDao(),
        mealPlanDao = database.mealPlanDao(),
        budgetDao = database.budgetDao(),
        paramsDao = database.paramsDao(),
        couponModifierDao = database.couponModifierDao()
    )

    private val pantryParser = PantryPhraseParser()
    private val dealsParser = DealsParser()
    private val mealPlanningEngine = MealPlanningEngine()
    private val budgetEngine = BudgetEngine()
    private val receiptReconciler = ReceiptReconciler()
    private val textRecognitionHelper = TextRecognitionHelper()
    private val pantryVisionClient = GeminiPantryVisionClient()
    private val barcodeLookupClient = OpenFoodFactsBarcodeClient()

    val aiVisionConfigured: Boolean = pantryVisionClient.isConfigured()
    val aiVisionModel: String = pantryVisionClient.modelName

    // Flows
    val pantryItems = repository.allPantryItems.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val deals = repository.allDeals.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val receipts = repository.allReceipts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val mealPlans = repository.allMealPlans.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val budgetState = repository.budgetState.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val params = repository.params.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _budgetAnalysis = MutableStateFlow<BudgetEngine.BudgetAnalysis?>(null)
    val budgetAnalysis: StateFlow<BudgetEngine.BudgetAnalysis?> = _budgetAnalysis.asStateFlow()

    private val _shoppingList = MutableStateFlow<List<MealPlanningEngine.ShoppingListItem>>(emptyList())
    val shoppingList: StateFlow<List<MealPlanningEngine.ShoppingListItem>> = _shoppingList.asStateFlow()

    private val _pantryPhotoStatus = MutableStateFlow<String?>(null)
    val pantryPhotoStatus: StateFlow<String?> = _pantryPhotoStatus.asStateFlow()

    private val _dealsScanStatus = MutableStateFlow<String?>(null)
    val dealsScanStatus: StateFlow<String?> = _dealsScanStatus.asStateFlow()

    private val _receiptScanStatus = MutableStateFlow<String?>(null)
    val receiptScanStatus: StateFlow<String?> = _receiptScanStatus.asStateFlow()

    private val _aiVisionConnectionStatus = MutableStateFlow<String?>(null)
    val aiVisionConnectionStatus: StateFlow<String?> = _aiVisionConnectionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            initializeDefaults()
            updateBudgetAnalysis()
        }
    }

    // Pantry operations
    fun addPantryPhrase(phrase: String) {
        viewModelScope.launch {
            val result = pantryParser.parse(phrase)
            upsertPantryItem(result.item)
        }
    }

    fun addPantryBarcode(barcode: String) {
        viewModelScope.launch {
            val cleanedBarcode = barcodeLookupClient.normalizeBarcode(barcode)
            if (cleanedBarcode.isBlank()) {
                _pantryPhotoStatus.value = "No barcode found."
                return@launch
            }

            _pantryPhotoStatus.value = "Looking up barcode..."

            val lookupResult = barcodeLookupClient.lookupBarcode(cleanedBarcode)
            val item = lookupResult.toPantryItem(cleanedBarcode)
            val mergedExisting = upsertPantryItem(item)
            _pantryPhotoStatus.value = barcodeStatusMessage(lookupResult, mergedExisting)
        }
    }

    fun updatePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.updatePantryItem(item)
        }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deletePantryItem(item)
        }
    }

    fun reportPantryPhotoCaptureCanceled() {
        _pantryPhotoStatus.value = "Pantry photo canceled."
    }

    fun reportPantryGallerySelectionCanceled() {
        _pantryPhotoStatus.value = "Pantry gallery selection canceled."
    }

    fun reportPantryCameraPermissionDenied() {
        _pantryPhotoStatus.value = "Camera permission is needed to take pantry photos."
    }

    fun reportPantryBarcodeScanCanceled() {
        _pantryPhotoStatus.value = "Barcode scan canceled."
    }

    fun reportPantryBarcodePermissionDenied() {
        _pantryPhotoStatus.value = "Camera permission is needed to scan barcodes."
    }

    fun processPantryPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            importPantryPhoto(bitmap)
        }
    }

    fun processPantryPhotoUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri)
                importPantryPhoto(bitmap)
            } catch (e: Exception) {
                _pantryPhotoStatus.value = "Could not open that pantry image. Try another photo."
            }
        }
    }

    private suspend fun importPantryPhoto(bitmap: Bitmap) {
        _pantryPhotoStatus.value = "Reading pantry photo..."

        val importedWithAi = if (pantryVisionClient.isConfigured()) {
            try {
                val result = pantryVisionClient.analyzePantryPhoto(bitmap)
                val items = result.items.mapNotNull { it.toPantryItem(result.warnings) }

                if (items.isNotEmpty()) {
                    val upsertResult = upsertPantryItems(items)
                    _pantryPhotoStatus.value = buildString {
                        append("Added/updated ${items.size} photo item")
                        if (items.size != 1) append("s")
                        if (items.any { it.needsVerify }) append(" with VERIFY checks")
                        if (upsertResult.updated > 0) {
                            append(" (${upsertResult.updated} merged)")
                        }
                    }
                    true
                } else {
                    _pantryPhotoStatus.value = "AI did not identify pantry items; trying label OCR..."
                    false
                }
            } catch (e: Exception) {
                _pantryPhotoStatus.value = "AI photo read failed; trying label OCR..."
                false
            }
        } else {
            false
        }

        if (!importedWithAi) {
            try {
                val ocrText = textRecognitionHelper.processImage(bitmap)
                importPantryOcrText(ocrText)
            } catch (e: Exception) {
                _pantryPhotoStatus.value = "Could not read that photo. Try a closer label shot."
            }
        }
    }

    private suspend fun importPantryOcrText(ocrText: String) {
        val phrase = ocrText.lines()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .filterNot { line ->
                line.contains("nutrition", ignoreCase = true) ||
                    line.contains("calories", ignoreCase = true) ||
                    line.contains("serving", ignoreCase = true) ||
                    line.matches(Regex("""\d+%"""))
            }
            .distinct()
            .take(8)
            .joinToString(" ")

        if (phrase.isBlank()) {
            _pantryPhotoStatus.value = "No readable label text found. Add manually or try another photo."
            return
        }

        val result = pantryParser.parse(phrase)
        val questions = mutableListOf<String>()
        if (result.item.brand == null) questions.add("What is the brand? Use Generic if none.")
        if (result.item.size == null && result.item.unit == null) questions.add("How much is there?")
        if (result.item.bestBy == null) questions.add("What is the expiration or best-by date?")

        val importedItem = result.item.copy(
                brand = result.item.brand ?: "Generic",
                needsVerify = true,
                notes = mergeNotes(
                    result.item.notes,
                    "Photo OCR import",
                    questions.joinToString(" ")
                )
            )
        val mergedExisting = upsertPantryItem(importedItem)

        _pantryPhotoStatus.value = if (mergedExisting) {
            "Updated photo item with VERIFY checks"
        } else {
            "Added photo item with VERIFY checks"
        }
    }

    // Deals operations
    fun processDealsOCR(ocrText: String, store: String = "Unknown") {
        viewModelScope.launch {
            val cleanedText = ocrText.trim()
            if (cleanedText.isBlank()) {
                _dealsScanStatus.value = "No flyer text found."
                return@launch
            }

            val result = dealsParser.parse(cleanedText, store.ifBlank { "Unknown" })
            if (result.deals.isEmpty()) {
                _dealsScanStatus.value = "No deals found. Try clearer flyer text."
            } else {
                repository.insertDeals(result.deals)
                _dealsScanStatus.value = "Added ${result.deals.size} flyer deal${if (result.deals.size == 1) "" else "s"}"
            }
        }
    }

    fun updateDeal(deal: DealItem) {
        viewModelScope.launch {
            repository.updateDeal(deal)
        }
    }

    fun deleteDeal(deal: DealItem) {
        viewModelScope.launch {
            repository.deleteDeal(deal)
        }
    }

    fun reportDealsPhotoCaptureCanceled() {
        _dealsScanStatus.value = "Flyer photo canceled."
    }

    fun reportDealsGallerySelectionCanceled() {
        _dealsScanStatus.value = "Flyer image selection canceled."
    }

    fun reportDealsPdfSelectionCanceled() {
        _dealsScanStatus.value = "Flyer PDF selection canceled."
    }

    fun reportDealsCameraPermissionDenied() {
        _dealsScanStatus.value = "Camera permission is needed to take flyer photos."
    }

    fun processDealsPhoto(bitmap: Bitmap, store: String = "Unknown") {
        viewModelScope.launch {
            importDealsPhoto(bitmap, store)
        }
    }

    fun processDealsPhotoUri(uri: Uri, store: String = "Unknown") {
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri)
                importDealsPhoto(bitmap, store)
            } catch (e: Exception) {
                _dealsScanStatus.value = "Could not open that flyer image. Try another photo or screenshot."
            }
        }
    }

    fun processDealsPdfUri(uri: Uri, store: String = "Unknown") {
        viewModelScope.launch {
            importDealsPdf(uri, store)
        }
    }

    private suspend fun importDealsPhoto(bitmap: Bitmap, store: String) {
        _dealsScanStatus.value = "Reading flyer photo..."

        try {
            val ocrText = textRecognitionHelper.processImage(bitmap)
            val result = dealsParser.parse(ocrText, store.ifBlank { "Unknown" })

            if (result.deals.isEmpty()) {
                _dealsScanStatus.value = "No deals found. Try a flatter, closer flyer photo."
            } else {
                repository.insertDeals(result.deals)
                _dealsScanStatus.value = "Added ${result.deals.size} flyer deal${if (result.deals.size == 1) "" else "s"}"
            }
        } catch (e: Exception) {
            _dealsScanStatus.value = "Could not read that flyer photo. Try again with better lighting."
        }
    }

    private suspend fun importDealsPdf(uri: Uri, store: String) {
        _dealsScanStatus.value = "Reading flyer PDF..."

        try {
            val pageTexts = renderPdfPages(uri).mapIndexed { index, bitmap ->
                _dealsScanStatus.value = "Reading flyer PDF page ${index + 1}..."
                textRecognitionHelper.processImage(bitmap)
            }
            val result = dealsParser.parse(pageTexts.joinToString("\n\n"), store.ifBlank { "Unknown" })

            if (result.deals.isEmpty()) {
                _dealsScanStatus.value = "No deals found in that PDF. Try flyer photos instead."
            } else {
                repository.insertDeals(result.deals)
                _dealsScanStatus.value = "Added ${result.deals.size} PDF deal${if (result.deals.size == 1) "" else "s"}"
            }
        } catch (e: Exception) {
            _dealsScanStatus.value = "Could not read that PDF. Try screenshots or flyer photos."
        }
    }

    // Meal planning
    fun generateMealPlan() {
        viewModelScope.launch {
            val currentParams = repository.getParams() ?: Params()
            val pantry = repository.getAllPantryItems()
            val currentDeals = repository.getAllDeals()

            val request = MealPlanningEngine.MealPlanRequest(
                params = currentParams,
                pantryItems = pantry,
                deals = currentDeals
            )

            val result = mealPlanningEngine.generateMealPlan(request)

            // Clear old plans and insert new ones
            repository.deleteOldMealPlans(LocalDate.now().minusDays(1))
            repository.insertMealPlans(result.mealPlans)

            _shoppingList.value = result.shoppingList
        }
    }

    fun updateMealPlan(plan: MealPlan) {
        viewModelScope.launch {
            repository.updateMealPlan(plan)
        }
    }

    // Budget operations
    fun updateBudget(budget: BudgetState) {
        viewModelScope.launch {
            repository.updateBudget(budget)
            updateBudgetAnalysis()
        }
    }

    private suspend fun updateBudgetAnalysis() {
        val budget = repository.getBudget() ?: return
        val receipts = repository.getAllReceipts()
        val analysis = budgetEngine.analyzeBudget(budget, receipts)
        _budgetAnalysis.value = analysis
    }

    // Receipt reconciliation
    fun processReceiptOCR(ocrText: String, store: String = "Unknown") {
        viewModelScope.launch {
            importReceiptOcrText(ocrText, store)
        }
    }

    fun processReceiptPhoto(bitmap: Bitmap, store: String = "Unknown") {
        viewModelScope.launch {
            importReceiptPhoto(bitmap, store)
        }
    }

    fun processReceiptPhotoUri(uri: Uri, store: String = "Unknown") {
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri)
                importReceiptPhoto(bitmap, store)
            } catch (e: Exception) {
                _receiptScanStatus.value = "Could not open that receipt image. Try another photo or screenshot."
            }
        }
    }

    fun reportReceiptPhotoCaptureCanceled() {
        _receiptScanStatus.value = "Receipt photo canceled."
    }

    fun reportReceiptGallerySelectionCanceled() {
        _receiptScanStatus.value = "Receipt gallery selection canceled."
    }

    fun reportReceiptCameraPermissionDenied() {
        _receiptScanStatus.value = "Camera permission is needed to take receipt photos."
    }

    fun updateReceipt(item: ReceiptItem) {
        viewModelScope.launch {
            val existing = repository.getReceipt(item.id)
            repository.updateReceipt(item)
            if (existing != null) {
                updateBudgetForReceiptDelta(item.totalCost - existing.totalCost)
                updatePantryForReceiptChange(existing, item)
            }
            updateBudgetAnalysis()
        }
    }

    fun deleteReceipt(item: ReceiptItem) {
        viewModelScope.launch {
            repository.deleteReceipt(item)
            updateBudgetForReceiptDelta(-item.totalCost)
            applyPantryReceiptDelta(item, -1.0)
            updateBudgetAnalysis()
        }
    }

    private suspend fun importReceiptPhoto(bitmap: Bitmap, store: String) {
        _receiptScanStatus.value = "Reading receipt photo..."

        try {
            val ocrText = textRecognitionHelper.processImage(bitmap)
            importReceiptOcrText(ocrText, store)
        } catch (e: Exception) {
            _receiptScanStatus.value = "Could not read that receipt photo. Try again with better lighting."
        }
    }

    private suspend fun importReceiptOcrText(ocrText: String, store: String) {
        val cleanedText = ocrText.trim()
        if (cleanedText.isBlank()) {
            _receiptScanStatus.value = "No receipt text found."
            return
        }

        try {
            val currentDeals = repository.getAllDeals()
            val pantry = repository.getAllPantryItems()
            val result = receiptReconciler.reconcileReceipt(cleanedText, currentDeals, pantry, store.ifBlank { "Unknown" })

            if (result.receiptItems.isEmpty()) {
                _receiptScanStatus.value = "No receipt line items found. Try a clearer photo or paste OCR text."
                return
            }

            repository.insertReceipts(result.receiptItems)
            result.pantryUpdates.forEach { updatedItem ->
                repository.updatePantryItem(updatedItem)
            }

            val currentBudget = repository.getBudget()
            if (currentBudget != null) {
                val updatedBudget = budgetEngine.updateBudgetWithReceipt(currentBudget, result.total)
                repository.updateBudget(updatedBudget)
                updateBudgetAnalysis()
            }

            _receiptScanStatus.value = buildString {
                append("Added ${result.receiptItems.size} receipt item")
                if (result.receiptItems.size != 1) append("s")
                append(" (${ "$%.2f".format(result.total) })")
                if (result.receiptItems.any { it.needsReview }) append(" with REVIEW checks")
            }
        } catch (e: Exception) {
            _receiptScanStatus.value = "Could not process that receipt."
        }
    }

    private suspend fun updateBudgetForReceiptDelta(delta: Double) {
        if (delta == 0.0) return
        val currentBudget = repository.getBudget() ?: return
        repository.updateBudget(
            budgetEngine.adjustBudgetForReceiptChange(currentBudget, delta)
        )
    }

    private suspend fun updatePantryForReceiptChange(
        oldReceipt: ReceiptItem,
        newReceipt: ReceiptItem
    ) {
        applyPantryReceiptDelta(oldReceipt, -1.0)
        applyPantryReceiptDelta(newReceipt, 1.0)
    }

    private suspend fun applyPantryReceiptDelta(receipt: ReceiptItem, direction: Double) {
        if (receipt.matchedType != "pantry") return
        val pantryItemId = receipt.matchedItemId ?: return
        val receiptQty = receipt.qty ?: return
        val pantryItem = repository.getPantryItem(pantryItemId) ?: return

        repository.updatePantryItem(
            pantryItem.copy(
                qty = (pantryItem.qty + (receiptQty * direction)).coerceAtLeast(0.0)
            )
        )
    }

    // Params operations
    fun updateParams(params: Params) {
        viewModelScope.launch {
            repository.updateParams(params)
        }
    }

    fun testAiVisionConnection() {
        viewModelScope.launch {
            _aiVisionConnectionStatus.value = "Testing Gemini connection..."
            val result = pantryVisionClient.testConnection()
            _aiVisionConnectionStatus.value = result.message
        }
    }

    // Demo data seeding
    fun loadDemoData() {
        viewModelScope.launch {
            // Clear existing data
            repository.deleteAllPantryItems()
            repository.deleteAllDeals()
            repository.deleteAllReceipts()
            repository.deleteAllMealPlans()

            // Add pantry anchors
            val pantryAnchors = listOf(
                PantryItem(item = "rice", qty = 5.0, unit = "lb", location = "pantry", form = "dried"),
                PantryItem(item = "pasta", qty = 3.0, unit = "lb", location = "pantry", form = "dried"),
                PantryItem(item = "oats", qty = 2.0, unit = "lb", location = "pantry", form = "dried"),
                PantryItem(item = "black beans", qty = 4.0, unit = "can", location = "pantry", form = "canned", size = "15 oz"),
                PantryItem(item = "olive oil", qty = 1.0, unit = "bottle", location = "pantry")
            )
            repository.insertPantryItems(pantryAnchors)

            // Add demo deals
            val demoDeals = listOf(
                DealItem(
                    name = "Pork Shoulder",
                    price = 3.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.85,
                    pricePerUnit = 3.99,
                    discountPercent = 30.0
                ),
                DealItem(
                    name = "Broccoli Crowns",
                    price = 1.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Kroger",
                    dealScore = 0.75,
                    pricePerUnit = 1.99,
                    discountPercent = 20.0
                ),
                DealItem(
                    name = "Mandarin Oranges",
                    price = 3.99,
                    unit = "bag",
                    dealType = "per_unit",
                    store = "Kroger",
                    dealScore = 0.70,
                    pricePerUnit = 3.99,
                    sizeText = "3 lb bag"
                ),
                DealItem(
                    name = "Chicken Breast",
                    price = 2.99,
                    unit = "lb",
                    dealType = "per_pound",
                    store = "Walmart",
                    dealScore = 0.80,
                    pricePerUnit = 2.99,
                    discountPercent = 25.0
                )
            )
            repository.insertDeals(demoDeals)

            // Reset demo params and budget so Load Demo is a deterministic phone-test baseline.
            repository.insertParams(Params())
            repository.insertBudget(
                BudgetState(
                    startingBudget = 292.0,
                    spentToDate = 45.0,
                    dailyEnvelope = 10.0,
                    breakfastAnchorCost = 0.55
                )
            )
            updateBudgetAnalysis()

            // Generate meal plan
            generateMealPlan()
        }
    }

    private suspend fun initializeDefaults() {
        if (repository.getParams() == null) {
            repository.insertParams(Params())
        }
        if (repository.getBudget() == null) {
            repository.insertBudget(
                BudgetState(
                    startingBudget = 292.0,
                    dailyEnvelope = 10.0,
                    breakfastAnchorCost = 0.55
                )
            )
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largestDimension = maxOf(info.size.width, info.size.height)
                if (largestDimension > MAX_INPUT_IMAGE_DIMENSION_PX) {
                    val scale = MAX_INPUT_IMAGE_DIMENSION_PX.toDouble() / largestDimension.toDouble()
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                .scaledToMaxDimension(MAX_INPUT_IMAGE_DIMENSION_PX)
        }
    }

    private fun Bitmap.scaledToMaxDimension(maxDimension: Int): Bitmap {
        val largestDimension = maxOf(width, height)
        if (largestDimension <= maxDimension) return this

        val scale = maxDimension.toDouble() / largestDimension.toDouble()
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private suspend fun renderPdfPages(uri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Could not open PDF")

        descriptor.use { parcelFileDescriptor ->
            PdfRenderer(parcelFileDescriptor).use { renderer ->
                val maxPages = minOf(renderer.pageCount, 12)
                (0 until maxPages).map { pageIndex ->
                    renderer.openPage(pageIndex).use { page ->
                        val scale = minOf(
                            PDF_RENDER_SCALE.toDouble(),
                            MAX_INPUT_IMAGE_DIMENSION_PX.toDouble() / maxOf(page.width, page.height).toDouble()
                        )
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).roundToInt().coerceAtLeast(1),
                            (page.height * scale).roundToInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }
    }

    private fun GeminiPantryVisionClient.PantryVisionItem.toPantryItem(warnings: List<String>): PantryItem? {
        val productName = product?.trim()?.ifBlank { null } ?: return null
        val questionNotes = questions.joinToString(" ")
        val warningNotes = warnings.joinToString(" ")
        val missingBrand = brand.isNullOrBlank()
        val missingAmount = quantity == null || unit.isNullOrBlank()
        val missingDate = expirationDate.isNullOrBlank()

        return PantryItem(
            item = productName,
            qty = quantity ?: 1.0,
            unit = unit?.takeUnless { it == "unknown" },
            size = size,
            brand = brand?.takeUnless { it.equals("unknown", ignoreCase = true) } ?: "Generic",
            location = location?.takeUnless { it == "unknown" } ?: "pantry",
            opened = parseDateOrNull(openedDate),
            bestBy = parseDateOrNull(expirationDate),
            notes = mergeNotes("AI photo import", questionNotes, warningNotes),
            needsVerify = confidence < 0.85 || questions.isNotEmpty() || missingBrand || missingAmount || missingDate
        )
    }

    private fun BarcodeLookupResult.toPantryItem(barcode: String): PantryItem {
        return when (this) {
            is BarcodeLookupResult.Found -> PantryItem(
                item = product.name,
                qty = 1.0,
                unit = "count",
                size = product.quantity,
                brand = product.brand,
                location = "pantry",
                notes = mergeNotes(
                    "Barcode: ${product.barcode}",
                    "Product lookup: Open Food Facts",
                    "Review quantity, location, and expiration."
                ),
                needsVerify = true
            )
            BarcodeLookupResult.NotFound -> fallbackBarcodePantryItem(
                barcode = barcode,
                lookupNote = "Product lookup did not find this code."
            )
            is BarcodeLookupResult.Error -> fallbackBarcodePantryItem(
                barcode = barcode,
                lookupNote = "Product lookup unavailable. ${message.trim()}".trim()
            )
        }
    }

    private fun fallbackBarcodePantryItem(barcode: String, lookupNote: String): PantryItem {
        return PantryItem(
            item = "Scanned barcode item",
            qty = 1.0,
            unit = "count",
            location = "pantry",
            notes = mergeNotes(
                "Barcode: $barcode",
                lookupNote,
                "Review item name, brand, size, and expiration."
            ),
            needsVerify = true
        )
    }

    private fun barcodeStatusMessage(
        lookupResult: BarcodeLookupResult,
        mergedExisting: Boolean
    ): String {
        return when (lookupResult) {
            is BarcodeLookupResult.Found -> {
                val action = if (mergedExisting) "Updated" else "Added"
                "$action ${lookupResult.product.name} from barcode lookup with VERIFY checks"
            }
            BarcodeLookupResult.NotFound -> {
                val action = if (mergedExisting) "Updated" else "Added"
                "$action barcode item with VERIFY checks; no product lookup match found."
            }
            is BarcodeLookupResult.Error -> {
                val action = if (mergedExisting) "Updated" else "Added"
                "$action barcode item with VERIFY checks; product lookup unavailable."
            }
        }
    }

    private fun parseDateOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private suspend fun upsertPantryItem(item: PantryItem): Boolean {
        val existing = repository.getAllPantryItems()
            .firstOrNull { pantryParser.areDuplicates(it, item) }

        return if (existing == null) {
            repository.insertPantryItem(item)
            false
        } else {
            repository.updatePantryItem(pantryParser.mergeDuplicateItems(existing, item))
            true
        }
    }

    private suspend fun upsertPantryItems(items: List<PantryItem>): PantryUpsertResult {
        var inserted = 0
        var updated = 0
        items.forEach { item ->
            if (upsertPantryItem(item)) {
                updated++
            } else {
                inserted++
            }
        }
        return PantryUpsertResult(inserted = inserted, updated = updated)
    }

    private fun mergeNotes(vararg values: String?): String? {
        return values
            .mapNotNull { it?.trim()?.ifBlank { null } }
            .distinct()
            .joinToString("; ")
            .ifBlank { null }
    }

    private data class PantryUpsertResult(
        val inserted: Int,
        val updated: Int
    )

    private companion object {
        private const val MAX_INPUT_IMAGE_DIMENSION_PX = 3072
        private const val PDF_RENDER_SCALE = 2
    }
}
