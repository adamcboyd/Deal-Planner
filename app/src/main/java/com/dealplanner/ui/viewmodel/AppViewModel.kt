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
import com.dealplanner.ocr.TextRecognitionHelper
import com.dealplanner.parser.DealsParser
import com.dealplanner.parser.PantryPhraseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
            repository.insertPantryItem(result.item)
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

    fun processPantryPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            importPantryPhoto(bitmap)
        }
    }

    fun processPantryPhotoUri(uri: Uri) {
        viewModelScope.launch {
            val bitmap = loadBitmapFromUri(uri)
            importPantryPhoto(bitmap)
        }
    }

    private suspend fun importPantryPhoto(bitmap: Bitmap) {
        _pantryPhotoStatus.value = "Reading pantry photo..."

        val importedWithAi = if (pantryVisionClient.isConfigured()) {
            try {
                val result = pantryVisionClient.analyzePantryPhoto(bitmap)
                val items = result.items.mapNotNull { it.toPantryItem(result.warnings) }

                if (items.isNotEmpty()) {
                    repository.insertPantryItems(items)
                    _pantryPhotoStatus.value = buildString {
                        append("Added ${items.size} photo item")
                        if (items.size != 1) append("s")
                        if (items.any { it.needsVerify }) append(" with VERIFY checks")
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

        repository.insertPantryItem(
            result.item.copy(
                brand = result.item.brand ?: "Generic",
                needsVerify = true,
                notes = mergeNotes(
                    result.item.notes,
                    "Photo OCR import",
                    questions.joinToString(" ")
                )
            )
        )

        _pantryPhotoStatus.value = "Added photo item with VERIFY checks"
    }

    // Deals operations
    fun processDealsOCR(ocrText: String, store: String = "Unknown") {
        viewModelScope.launch {
            val result = dealsParser.parse(ocrText, store)
            repository.insertDeals(result.deals)
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

    fun processDealsPhoto(bitmap: Bitmap, store: String = "Unknown") {
        viewModelScope.launch {
            importDealsPhoto(bitmap, store)
        }
    }

    fun processDealsPhotoUri(uri: Uri, store: String = "Unknown") {
        viewModelScope.launch {
            val bitmap = loadBitmapFromUri(uri)
            importDealsPhoto(bitmap, store)
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
            val result = dealsParser.parse(ocrText, store)

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
            val result = dealsParser.parse(pageTexts.joinToString("\n\n"), store)

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
        val receipts = emptyList<ReceiptItem>() // Would load actual receipts
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
            val bitmap = loadBitmapFromUri(uri)
            importReceiptPhoto(bitmap, store)
        }
    }

    fun updateReceipt(item: ReceiptItem) {
        viewModelScope.launch {
            val existing = repository.getReceipt(item.id)
            repository.updateReceipt(item)
            if (existing != null) {
                updateBudgetForReceiptDelta(item.totalCost - existing.totalCost)
            }
            updateBudgetAnalysis()
        }
    }

    fun deleteReceipt(item: ReceiptItem) {
        viewModelScope.launch {
            repository.deleteReceipt(item)
            updateBudgetForReceiptDelta(-item.totalCost)
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

            // Initialize params if not exists
            if (repository.getParams() == null) {
                repository.insertParams(Params())
            }

            // Initialize budget if not exists
            if (repository.getBudget() == null) {
                repository.insertBudget(
                    BudgetState(
                        startingBudget = 292.0,
                        spentToDate = 45.0,
                        dailyEnvelope = 10.0,
                        breakfastAnchorCost = 0.55
                    )
                )
            }

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
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
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
                        val scale = 2
                        val bitmap = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
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

    private fun parseDateOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun mergeNotes(vararg values: String?): String? {
        return values
            .mapNotNull { it?.trim()?.ifBlank { null } }
            .distinct()
            .joinToString("; ")
            .ifBlank { null }
    }
}
