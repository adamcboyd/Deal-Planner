package com.dealplanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dealplanner.data.model.ReceiptItem
import com.dealplanner.ui.camera.CapturePhotoUriFactory
import com.dealplanner.ui.state.ManualInputClearDecision
import com.dealplanner.ui.state.ManualInputClearPolicy
import com.dealplanner.ui.viewmodel.AppViewModel
import com.dealplanner.util.toFlexibleDoubleOrNull
import java.time.LocalDate
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val receipts by viewModel.receipts.collectAsState()
    val receiptScanStatus by viewModel.receiptScanStatus.collectAsState()
    var storeName by remember { mutableStateOf("Unknown") }
    var receiptText by remember { mutableStateOf("") }
    var clearReceiptTextOnSuccess by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) {
            viewModel.processReceiptPhotoUri(uri, storeName)
        } else {
            viewModel.reportReceiptPhotoCaptureCanceled()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processReceiptPhotoUri(uri, storeName)
        } else {
            viewModel.reportReceiptGallerySelectionCanceled()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processReceiptPdfUri(uri, storeName)
        } else {
            viewModel.reportReceiptPdfSelectionCanceled()
        }
    }

    fun launchReceiptCamera() {
        try {
            val uri = CapturePhotoUriFactory.create(context, "receipt")
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            pendingCameraUri = null
            viewModel.reportReceiptPhotoLaunchFailed()
        }
    }

    fun launchReceiptGalleryPicker() {
        try {
            photoPickerLauncher.launch("image/*")
        } catch (e: Exception) {
            viewModel.reportReceiptGalleryLaunchFailed()
        }
    }

    fun launchReceiptPdfPicker() {
        try {
            pdfPickerLauncher.launch("application/pdf")
        } catch (e: Exception) {
            viewModel.reportReceiptPdfLaunchFailed()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchReceiptCamera()
        } else {
            viewModel.reportReceiptCameraPermissionDenied()
        }
    }

    fun requestReceiptCameraPermission() {
        try {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } catch (e: Exception) {
            viewModel.reportReceiptCameraPermissionRequestFailed()
        }
    }

    LaunchedEffect(receiptScanStatus) {
        when (ManualInputClearPolicy.forReceiptStatus(receiptScanStatus, clearReceiptTextOnSuccess)) {
            ManualInputClearDecision.ClearText -> {
                receiptText = ""
                clearReceiptTextOnSuccess = false
            }
            ManualInputClearDecision.StopWaiting -> {
                clearReceiptTextOnSuccess = false
            }
            ManualInputClearDecision.None -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Receipts", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Import receipts to reconcile purchases, pantry updates, and budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = receiptText,
                    onValueChange = { receiptText = it },
                    label = { Text("Paste receipt OCR text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        clearReceiptTextOnSuccess = receiptText.isNotBlank()
                        viewModel.processReceiptOCR(receiptText, storeName)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Process Text")
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
                                launchReceiptCamera()
                            } else {
                                requestReceiptCameraPermission()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Photo")
                    }
                    OutlinedButton(
                        onClick = { launchReceiptGalleryPicker() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { launchReceiptPdfPicker() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF")
                }
                if (receiptScanStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = receiptScanStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (receipts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Text(
                            "No receipts yet. Add a receipt photo, gallery image, PDF, or pasted text.",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            items(receipts) { receipt ->
                ReceiptItemCard(
                    receipt = receipt,
                    onUpdate = { viewModel.updateReceipt(it) },
                    onDelete = { viewModel.deleteReceipt(receipt) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReceiptItemCard(
    receipt: ReceiptItem,
    onUpdate: (ReceiptItem) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (receipt.needsReview) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
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
                        text = receipt.rawLine,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (receipt.needsReview) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Needs review",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"$%.2f".format(receipt.totalCost)} • ${receipt.store ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Match: ${receipt.matchedType ?: "unmatched"} • Confidence: ${"%.0f".format(receipt.confidence * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        ReceiptItemEditDialog(
            receipt = receipt,
            onDismiss = { showEditDialog = false },
            onSave = { updatedReceipt ->
                onUpdate(updatedReceipt)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptItemEditDialog(
    receipt: ReceiptItem,
    onDismiss: () -> Unit,
    onSave: (ReceiptItem) -> Unit
) {
    var rawLine by remember(receipt.id) { mutableStateOf(receipt.rawLine) }
    var qty by remember(receipt.id) { mutableStateOf(receipt.qty?.toString().orEmpty()) }
    var totalCost by remember(receipt.id) { mutableStateOf(receipt.totalCost.toString()) }
    var store by remember(receipt.id) { mutableStateOf(receipt.store.orEmpty()) }
    var matchedType by remember(receipt.id) { mutableStateOf(receipt.matchedType.orEmpty()) }
    var matchedItemId by remember(receipt.id) { mutableStateOf(receipt.matchedItemId?.toString().orEmpty()) }
    var confidence by remember(receipt.id) { mutableStateOf(receipt.confidence.toString()) }
    var date by remember(receipt.id) { mutableStateOf(receipt.date.toString()) }
    var needsReview by remember(receipt.id) { mutableStateOf(receipt.needsReview) }
    val parsedQty = qty.toFlexibleDoubleOrNull()
    val isQtyValid = qty.isBlank() || (parsedQty != null && parsedQty >= 0.0)
    val parsedTotalCost = totalCost.toFlexibleDoubleOrNull()
    val isTotalCostValid = parsedTotalCost != null && parsedTotalCost >= 0.0
    val parsedMatchedItemId = matchedItemId.toLongOrNull()
    val isMatchedItemIdValid = matchedItemId.isBlank() || (parsedMatchedItemId != null && parsedMatchedItemId >= 0L)
    val parsedConfidence = confidence.toFlexibleDoubleOrNull()
    val isConfidenceValid = parsedConfidence != null && parsedConfidence in 0.0..1.0
    val parsedDate = date.toLocalDateOrNull()
    val isDateValid = parsedDate != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Receipt Item") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = rawLine,
                        onValueChange = { rawLine = it },
                        label = { Text("Receipt line") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            isError = !isQtyValid,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = totalCost,
                            onValueChange = { totalCost = it },
                            label = { Text("Total") },
                            modifier = Modifier.weight(1f),
                            isError = !isTotalCostValid,
                            singleLine = true
                        )
                    }
                    if (!isQtyValid || !isTotalCostValid) {
                        Text(
                            "Use non-negative quantity and total values, or leave quantity blank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = store,
                        onValueChange = { store = it },
                        label = { Text("Store") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = matchedType,
                            onValueChange = { matchedType = it },
                            label = { Text("Match type") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = matchedItemId,
                            onValueChange = { matchedItemId = it },
                            label = { Text("Match ID") },
                            modifier = Modifier.weight(1f),
                            isError = !isMatchedItemIdValid,
                            singleLine = true
                        )
                    }
                    if (!isMatchedItemIdValid) {
                        Text(
                            "Use a whole-number match ID or leave blank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = confidence,
                            onValueChange = { confidence = it },
                            label = { Text("Confidence 0-1") },
                            modifier = Modifier.weight(1f),
                            isError = !isConfidenceValid,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            isError = !isDateValid,
                            singleLine = true
                        )
                    }
                    if (!isConfidenceValid) {
                        Text(
                            "Use a confidence value from 0 to 1.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (!isDateValid) {
                        Text(
                            "Use YYYY-MM-DD.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Needs review", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = needsReview,
                            onCheckedChange = { needsReview = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = rawLine.isNotBlank() &&
                    isQtyValid &&
                    isTotalCostValid &&
                    isMatchedItemIdValid &&
                    isConfidenceValid &&
                    isDateValid,
                onClick = {
                    onSave(
                        receipt.copy(
                            rawLine = rawLine.trim(),
                            matchedItemId = parsedMatchedItemId,
                            matchedType = matchedType.trim().ifBlank { null },
                            qty = parsedQty,
                            totalCost = parsedTotalCost ?: receipt.totalCost,
                            date = parsedDate ?: receipt.date,
                            confidence = parsedConfidence ?: receipt.confidence,
                            store = store.trim().ifBlank { null },
                            needsReview = needsReview
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

private fun String.toLocalDateOrNull(): LocalDate? {
    if (isBlank()) return null
    return try {
        LocalDate.parse(trim())
    } catch (_: DateTimeParseException) {
        null
    }
}
