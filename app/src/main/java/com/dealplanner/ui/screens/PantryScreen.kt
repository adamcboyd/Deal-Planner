package com.dealplanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dealplanner.data.model.PantryItem
import com.dealplanner.ui.camera.CapturePhotoUriFactory
import com.dealplanner.ui.state.ManualInputClearDecision
import com.dealplanner.ui.state.ManualInputClearPolicy
import com.dealplanner.ui.state.PantryItemInputValidator
import com.dealplanner.ui.viewmodel.AppViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val pantryItems by viewModel.pantryItems.collectAsState()
    val pendingPantryReviewItems by viewModel.pendingPantryReviewItems.collectAsState()
    val pantryPhotoStatus by viewModel.pantryPhotoStatus.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var barcodeText by remember { mutableStateOf("") }
    var clearBarcodeTextOnSuccess by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) {
            viewModel.processPantryPhotoUri(uri)
        } else {
            viewModel.reportPantryPhotoCaptureCanceled()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processPantryPhotoUri(uri)
        } else {
            viewModel.reportPantryGallerySelectionCanceled()
        }
    }

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val contents = result.contents?.trim().orEmpty()
        if (contents.isNotBlank()) {
            viewModel.addPantryBarcode(contents)
        } else {
            viewModel.reportPantryBarcodeScanCanceled()
        }
    }

    fun launchBarcodeScanner() {
        try {
            val options = ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
                .setPrompt("Scan pantry barcode")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
            barcodeLauncher.launch(options)
        } catch (e: Exception) {
            viewModel.reportPantryBarcodeScannerLaunchFailed()
        }
    }

    fun launchPantryCamera() {
        try {
            val uri = CapturePhotoUriFactory.create(context, "pantry")
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            pendingCameraUri = null
            viewModel.reportPantryPhotoLaunchFailed()
        }
    }

    fun launchPantryGalleryPicker() {
        try {
            photoPickerLauncher.launch("image/*")
        } catch (e: Exception) {
            viewModel.reportPantryGalleryLaunchFailed()
        }
    }

    val barcodePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchBarcodeScanner()
        } else {
            viewModel.reportPantryBarcodePermissionDenied()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchPantryCamera()
        } else {
            viewModel.reportPantryCameraPermissionDenied()
        }
    }

    fun requestPantryCameraPermission() {
        try {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } catch (e: Exception) {
            viewModel.reportPantryCameraPermissionRequestFailed()
        }
    }

    fun requestPantryBarcodePermission() {
        try {
            barcodePermissionLauncher.launch(Manifest.permission.CAMERA)
        } catch (e: Exception) {
            viewModel.reportPantryBarcodePermissionRequestFailed()
        }
    }

    LaunchedEffect(pantryPhotoStatus) {
        when (ManualInputClearPolicy.forBarcodeStatus(pantryPhotoStatus, clearBarcodeTextOnSuccess)) {
            ManualInputClearDecision.ClearText -> {
                barcodeText = ""
                clearBarcodeTextOnSuccess = false
            }
            ManualInputClearDecision.StopWaiting -> {
                clearBarcodeTextOnSuccess = false
            }
            ManualInputClearDecision.None -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Input section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Pantry Item", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("e.g., '2 cans black beans 15oz'") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addPantryPhrase(inputText)
                        if (inputText.isNotBlank()) {
                            inputText = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                launchPantryCamera()
                            } else {
                                requestPantryCameraPermission()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Photo")
                    }
                    OutlinedButton(
                        onClick = { launchPantryGalleryPicker() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = barcodeText,
                    onValueChange = { barcodeText = it },
                    label = { Text("Barcode / UPC") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                launchBarcodeScanner()
                            } else {
                                requestPantryBarcodePermission()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan")
                    }
                    OutlinedButton(
                        onClick = {
                            clearBarcodeTextOnSuccess = barcodeText.isNotBlank()
                            viewModel.addPantryBarcode(barcodeText)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Code")
                    }
                }
                if (pantryPhotoStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pantryPhotoStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (pendingPantryReviewItems.isNotEmpty()) {
            PendingPantryReviewSection(
                items = pendingPantryReviewItems,
                onSaveAll = { viewModel.savePendingPantryReviewItems() },
                onClear = { viewModel.clearPendingPantryReviewItems() },
                onRemove = { index -> viewModel.removePendingPantryReviewItem(index) },
                onUpdate = { index, item -> viewModel.updatePendingPantryReviewItem(index, item) }
            )
        }

        // Pantry list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(pantryItems) { item ->
                PantryItemCard(
                    item = item,
                    onDelete = { viewModel.deletePantryItem(item) },
                    onUpdate = { viewModel.updatePantryItem(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PendingPantryReviewSection(
    items: List<PantryItem>,
    onSaveAll: () -> Unit,
    onClear: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, PantryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Review Pantry Imports",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Edit or remove photo items before saving them to Pantry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            Button(
                onClick = onSaveAll,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save All")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items) { index, item ->
                PantryItemCard(
                    item = item,
                    onDelete = { onRemove(index) },
                    onUpdate = { updatedItem -> onUpdate(index, updatedItem) }
                )
            }
        }
    }
}

@Composable
fun PantryItemCard(
    item: PantryItem,
    onDelete: () -> Unit,
    onUpdate: (PantryItem) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (item.needsVerify) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.item,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (item.needsVerify) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Needs verification",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append("${item.qty}")
                        if (item.unit != null) append(" ${item.unit}")
                        if (item.size != null) append(" • ${item.size}")
                        if (item.brand != null) append(" • ${item.brand}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.location != null) {
                    Text(
                        text = "Location: ${item.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.bestBy != null) {
                    Text(
                        text = "Best by: ${item.bestBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (item.notes != null) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    if (showEditDialog) {
        PantryItemEditDialog(
            item = item,
            onDismiss = { showEditDialog = false },
            onSave = { updatedItem ->
                onUpdate(updatedItem)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryItemEditDialog(
    item: PantryItem,
    onDismiss: () -> Unit,
    onSave: (PantryItem) -> Unit
) {
    var itemName by remember(item) { mutableStateOf(item.item) }
    var quantity by remember(item) { mutableStateOf(item.qty.toString()) }
    var unit by remember(item) { mutableStateOf(item.unit.orEmpty()) }
    var size by remember(item) { mutableStateOf(item.size.orEmpty()) }
    var brand by remember(item) { mutableStateOf(item.brand.orEmpty()) }
    var location by remember(item) { mutableStateOf(item.location.orEmpty()) }
    var bestBy by remember(item) { mutableStateOf(item.bestBy?.toString().orEmpty()) }
    var notes by remember(item) { mutableStateOf(item.notes.orEmpty()) }
    var needsVerify by remember(item) { mutableStateOf(item.needsVerify) }
    val quantityValidation = PantryItemInputValidator.validateQuantity(quantity)
    val bestByValidation = PantryItemInputValidator.validateBestByDate(bestBy)
    val isQuantityValid = quantityValidation.isValid
    val isBestByValid = bestByValidation.isValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Pantry Item") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            isError = !isQuantityValid,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    if (!isQuantityValid) {
                        Text(
                            quantityValidation.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = size,
                        onValueChange = { size = it },
                        label = { Text("Package size") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = bestBy,
                        onValueChange = { bestBy = it },
                        label = { Text("Best by YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isBestByValid,
                        singleLine = true
                    )
                    if (!isBestByValid) {
                        Text(
                            bestByValidation.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Needs verification", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = needsVerify,
                            onCheckedChange = { needsVerify = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = itemName.isNotBlank() && isQuantityValid && isBestByValid,
                onClick = {
                    onSave(
                        item.copy(
                            item = itemName.trim(),
                            qty = quantityValidation.parsedValue ?: item.qty,
                            unit = unit.trim().ifBlank { null },
                            size = size.trim().ifBlank { null },
                            brand = brand.trim().ifBlank { null },
                            location = location.trim().ifBlank { null },
                            bestBy = bestByValidation.parsedValue,
                            notes = notes.trim().ifBlank { null },
                            needsVerify = needsVerify
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
