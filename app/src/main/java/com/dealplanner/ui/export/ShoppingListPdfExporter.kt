package com.dealplanner.ui.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.dealplanner.domain.MealPlanningEngine
import com.dealplanner.domain.ShoppingListExportFormatter
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShoppingListPdfExporter(
    private val formatter: ShoppingListExportFormatter = ShoppingListExportFormatter()
) {
    fun export(
        context: Context,
        items: List<MealPlanningEngine.ShoppingListItem>,
        generatedOn: LocalDate = LocalDate.now()
    ): Uri {
        require(items.isNotEmpty()) { "Shopping list export requires at least one item." }

        val report = formatter.buildReport(items, generatedOn)
        val exportDir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val exportFile = File(
            exportDir,
            "shopping-list-${generatedOn.format(DateTimeFormatter.BASIC_ISO_DATE)}-${System.currentTimeMillis()}.pdf"
        )
        val document = PdfDocument()

        try {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(40, 40, 40)
                textSize = 11f
            }
            val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(95, 95, 95)
                textSize = 9f
            }

            var pageNumber = 1
            var page = document.startPage(newPageInfo(pageNumber))
            var canvas = page.canvas
            var y = TOP_MARGIN

            fun drawFooter() {
                canvas.drawLine(
                    LEFT_MARGIN.toFloat(),
                    (PAGE_HEIGHT - FOOTER_TOP).toFloat(),
                    (PAGE_WIDTH - RIGHT_MARGIN).toFloat(),
                    (PAGE_HEIGHT - FOOTER_TOP).toFloat(),
                    smallPaint
                )
                canvas.drawText(
                    "Deal Planner",
                    LEFT_MARGIN.toFloat(),
                    (PAGE_HEIGHT - 24).toFloat(),
                    smallPaint
                )
                canvas.drawText(
                    "Page $pageNumber",
                    (PAGE_WIDTH - RIGHT_MARGIN - 48).toFloat(),
                    (PAGE_HEIGHT - 24).toFloat(),
                    smallPaint
                )
            }

            fun finishPage() {
                drawFooter()
                document.finishPage(page)
            }

            fun startPage() {
                pageNumber += 1
                page = document.startPage(newPageInfo(pageNumber))
                canvas = page.canvas
                y = TOP_MARGIN
            }

            fun ensureSpace(requiredHeight: Int) {
                if (y + requiredHeight <= PAGE_HEIGHT - BOTTOM_MARGIN) return
                finishPage()
                startPage()
            }

            fun drawWrappedText(
                text: String,
                paint: Paint,
                indent: Int = 0,
                lineHeight: Int = BODY_LINE_HEIGHT,
                bottomSpacing: Int = 0
            ) {
                val lines = wrapText(text, paint, CONTENT_WIDTH - indent)
                ensureSpace(lines.size * lineHeight + bottomSpacing)
                lines.forEach { line ->
                    canvas.drawText(line, (LEFT_MARGIN + indent).toFloat(), y.toFloat(), paint)
                    y += lineHeight
                }
                y += bottomSpacing
            }

            drawWrappedText(report.title, titlePaint, lineHeight = 28, bottomSpacing = 6)
            drawWrappedText("Generated: ${report.generatedOnText}", bodyPaint, bottomSpacing = 8)
            drawWrappedText("Estimated total: ${report.totalEstimatedCostText}", headingPaint, bottomSpacing = 14)

            report.rows.forEachIndexed { index, row ->
                ensureSpace(ITEM_MIN_HEIGHT)
                drawWrappedText(
                    "${index + 1}. ${row.quantityText} ${row.name} - ${row.estimatedCostText}",
                    headingPaint,
                    lineHeight = HEADING_LINE_HEIGHT,
                    bottomSpacing = 2
                )
                row.detailLines.forEach { detail ->
                    drawWrappedText(detail, bodyPaint, indent = 16, bottomSpacing = 1)
                }
                y += 8
            }

            drawWrappedText("Estimated total: ${report.totalEstimatedCostText}", headingPaint, bottomSpacing = 4)
            finishPage()

            exportFile.outputStream().use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
    }

    private fun newPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            if (paint.measureText(word) > maxWidth) {
                if (currentLine.isNotBlank()) {
                    lines.add(currentLine)
                    currentLine = ""
                }
                lines.addAll(splitLongWord(word, paint, maxWidth))
                return@forEach
            }

            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotBlank()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun splitLongWord(word: String, paint: Paint, maxWidth: Int): List<String> {
        val parts = mutableListOf<String>()
        var remaining = word
        while (remaining.isNotEmpty()) {
            var end = remaining.length
            while (end > 1 && paint.measureText(remaining.substring(0, end)) > maxWidth) {
                end -= 1
            }
            parts.add(remaining.substring(0, end))
            remaining = remaining.substring(end)
        }
        return parts
    }

    private companion object {
        const val EXPORT_DIR = "shopping_list_exports"
        const val PAGE_WIDTH = 612
        const val PAGE_HEIGHT = 792
        const val LEFT_MARGIN = 48
        const val RIGHT_MARGIN = 48
        const val TOP_MARGIN = 48
        const val BOTTOM_MARGIN = 64
        const val FOOTER_TOP = 44
        const val CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
        const val HEADING_LINE_HEIGHT = 17
        const val BODY_LINE_HEIGHT = 14
        const val ITEM_MIN_HEIGHT = 84
    }
}
