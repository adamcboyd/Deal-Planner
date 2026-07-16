package com.dealplanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dealplanner.data.model.DealItem
import com.dealplanner.ui.camera.CapturePhotoUriFactory
import com.dealplanner.ui.viewmodel.AppViewModel
import com.dealplanner.util.toFlexibleDoubleOrNull
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun DealsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val deals by viewModel.deals.collectAsState()
    val dealsScanStatus by viewModel.dealsScanStatus.collectAsState()
    var storeName by remember { mutableStateOf("Unknown") }
    var flyerText by remember { mutableStateOf("") }
    var clearFlyerTextOnSuccess by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) {
            viewModel.processDealsPhotoUri(uri, storeName)
        } else {
            viewModel.reportDealsPhotoCaptureCanceled()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processDealsPhotoUri(uri, storeName)
        } else {
            viewModel.reportDealsGallerySelectionCanceled()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processDealsPdfUri(uri, storeName)
        } else {
            viewModel.reportDealsPdfSelectionCanceled()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = CapturePhotoUriFactory.create(context, "flyer")
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.reportDealsCameraPermissionDenied()
        }
    }

    LaunchedEffect(dealsScanStatus) {
        val status = dealsScanStatus.orEmpty()
        if (!clearFlyerTextOnSuccess) return@LaunchedEffect

        when {
            status.startsWith("Added ") && status.contains("flyer deal") -> {
                flyerText = ""
                clearFlyerTextOnSuccess = false
            }
            status.startsWith("No ") || status.startsWith("Could not") -> {
                clearFlyerTextOnSuccess = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with add button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Deals", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Import flyers to add deals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Demo: Use 'Load Demo' button to populate sample deals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = flyerText,
                    onValueChange = { flyerText = it },
                    label = { Text("Paste flyer OCR text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        clearFlyerTextOnSuccess = flyerText.isNotBlank()
                        viewModel.processDealsOCR(flyerText, storeName)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Process Text")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val uri = CapturePhotoUriFactory.create(context, "flyer")
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Flyer Photo")
                    }
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Flyer Image")
                    }
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Flyer PDF")
                    }
                }
                if (dealsScanStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dealsScanStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Deals list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(deals) { deal ->
                DealItemCard(
                    deal = deal,
                    onUpdate = { viewModel.updateDeal(it) },
                    onDelete = { viewModel.deleteDeal(deal) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (deals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Text(
                            "No deals yet. Use the Demo button or scan a flyer.",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DealItemCard(
    deal: DealItem,
    onUpdate: (DealItem) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (deal.confidence < 0.7) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = deal.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (deal.couponFlag) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("Coupon", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"$%.2f".format(deal.price)}${if (deal.unit != null) "/${deal.unit}" else ""}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Deal Score",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${"%.0f".format(deal.dealScore * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deal.sizeText != null) {
                    Text(
                        text = "Size: ${deal.sizeText}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Store: ${deal.store} • Type: ${deal.dealType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (deal.discountPercent > 0) {
                    Text(
                        text = "${deal.discountPercent.toInt()}% off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
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
        DealItemEditDialog(
            deal = deal,
            onDismiss = { showEditDialog = false },
            onSave = { updatedDeal ->
                onUpdate(updatedDeal)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealItemEditDialog(
    deal: DealItem,
    onDismiss: () -> Unit,
    onSave: (DealItem) -> Unit
) {
    var name by remember(deal.id) { mutableStateOf(deal.name) }
    var brand by remember(deal.id) { mutableStateOf(deal.brand.orEmpty()) }
    var sizeText by remember(deal.id) { mutableStateOf(deal.sizeText.orEmpty()) }
    var price by remember(deal.id) { mutableStateOf(deal.price.toString()) }
    var unit by remember(deal.id) { mutableStateOf(deal.unit.orEmpty()) }
    var store by remember(deal.id) { mutableStateOf(deal.store) }
    var dealType by remember(deal.id) { mutableStateOf(deal.dealType) }
    var pricePerUnit by remember(deal.id) { mutableStateOf(deal.pricePerUnit.toString()) }
    var discountPercent by remember(deal.id) { mutableStateOf(deal.discountPercent.toString()) }
    var dealScore by remember(deal.id) { mutableStateOf(deal.dealScore.toString()) }
    var confidence by remember(deal.id) { mutableStateOf(deal.confidence.toString()) }
    var limit by remember(deal.id) { mutableStateOf(deal.limit?.toString().orEmpty()) }
    var validUntil by remember(deal.id) { mutableStateOf(deal.validUntil?.toString().orEmpty()) }
    var couponFlag by remember(deal.id) { mutableStateOf(deal.couponFlag) }
    val parsedValidUntil = validUntil.toLocalDateOrNull()
    val isValidUntilValid = validUntil.isBlank() || parsedValidUntil != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Deal") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Deal item") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") },
                            modifier = Modifier.weight(1f),
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
                        value = sizeText,
                        onValueChange = { sizeText = it },
                        label = { Text("Package size") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dealType,
                            onValueChange = { dealType = it },
                            label = { Text("Deal type") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = limit,
                            onValueChange = { limit = it },
                            label = { Text("Limit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pricePerUnit,
                            onValueChange = { pricePerUnit = it },
                            label = { Text("PPU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = discountPercent,
                            onValueChange = { discountPercent = it },
                            label = { Text("% off") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dealScore,
                            onValueChange = { dealScore = it },
                            label = { Text("Score 0-1") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = confidence,
                            onValueChange = { confidence = it },
                            label = { Text("Confidence 0-1") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = validUntil,
                        onValueChange = { validUntil = it },
                        label = { Text("Valid until YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isValidUntilValid,
                        singleLine = true
                    )
                    if (!isValidUntilValid) {
                        Text(
                            "Use YYYY-MM-DD or leave blank.",
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
                        Text("Coupon required", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = couponFlag,
                            onCheckedChange = { couponFlag = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() &&
                    store.isNotBlank() &&
                    price.toFlexibleDoubleOrNull() != null &&
                    isValidUntilValid,
                onClick = {
                    val parsedPrice = price.toFlexibleDoubleOrNull() ?: deal.price
                    onSave(
                        deal.copy(
                            name = name.trim(),
                            brand = brand.trim().ifBlank { null },
                            sizeText = sizeText.trim().ifBlank { null },
                            price = parsedPrice,
                            unit = unit.trim().ifBlank { null },
                            dealType = dealType.trim().ifBlank { "per_unit" },
                            limit = limit.toIntOrNull(),
                            couponFlag = couponFlag,
                            store = store.trim(),
                            confidence = confidence.toFlexibleDoubleOrNull()?.coerceIn(0.0, 1.0) ?: deal.confidence,
                            dealScore = dealScore.toFlexibleDoubleOrNull()?.coerceIn(0.0, 1.0) ?: deal.dealScore,
                            pricePerUnit = pricePerUnit.toFlexibleDoubleOrNull() ?: parsedPrice,
                            discountPercent = discountPercent.toFlexibleDoubleOrNull() ?: deal.discountPercent,
                            validUntil = parsedValidUntil
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
