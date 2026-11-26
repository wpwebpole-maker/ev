package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.AppSettings
import com.example.data.ApplicationDocument
import com.example.data.DocumentType
import com.example.data.InsuranceApplication
import com.example.data.PaymentRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595   // A4 width in PostScript points (210mm)
    private const val PAGE_HEIGHT = 842  // A4 height in PostScript points (297mm)
    private const val MARGIN = 32f

    fun generateApplicationPdf(
        context: Context,
        application: InsuranceApplication,
        documents: List<ApplicationDocument>,
        payment: PaymentRecord?,
        settings: AppSettings
    ): Result<File> {
        return try {
            val pdf = PdfDocument()
            val crimson = Color.rgb(155, 15, 26)
            val darkRed = Color.rgb(107, 7, 16)
            val gold = Color.rgb(242, 183, 5)
            val ink = Color.rgb(28, 22, 22)
            val muted = Color.rgb(100, 92, 92)
            val border = Color.rgb(220, 210, 205)

            val headerBgPaint = Paint().apply { color = darkRed }
            val goldLinePaint = Paint().apply { color = gold; strokeWidth = 3f }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 232, 170)
                textSize = 10.5f
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = crimson
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink
                textSize = 11f
            }
            val linePaint = Paint().apply { color = border; strokeWidth = 1f }
            val boxStrokePaint = Paint().apply {
                color = border
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            // --- PAGE 1: BRAND HEADER + CUSTOMER, VEHICLE & PAYMENT DETAILS ---
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            // Branded Header Bar
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 92f, headerBgPaint)
            canvas.drawLine(0f, 92f, PAGE_WIDTH.toFloat(), 92f, goldLinePaint)

            // Exact RAAG AUTOMOBILES Logo
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.raag_logo)
            if (logoBitmap != null) {
                val destRect = RectF(MARGIN, 14f, MARGIN + 64f, 78f)
                canvas.drawBitmap(logoBitmap, null, destRect, null)
                logoBitmap.recycle()
            }

            canvas.drawText(settings.businessName.uppercase(Locale.getDefault()), MARGIN + 78f, 40f, titlePaint)
            canvas.drawText("EV / Scooty Insurance Application Record", MARGIN + 78f, 58f, subTitlePaint)
            canvas.drawText("Helpline: ${settings.contactPhone}  |  UPI: ${settings.upiId}", MARGIN + 78f, 74f, subTitlePaint)

            val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val submittedDate = application.createdAt?.toDate()?.let { dateFmt.format(it) } ?: dateFmt.format(Date())

            var y = 118f

            // Summary Pill Banner
            val pillPaint = Paint().apply { color = Color.rgb(252, 245, 242) }
            canvas.drawRoundRect(RectF(MARGIN, y - 16f, PAGE_WIDTH - MARGIN, y + 32f), 8f, 8f, pillPaint)
            canvas.drawRoundRect(RectF(MARGIN, y - 16f, PAGE_WIDTH - MARGIN, y + 32f), 8f, 8f, boxStrokePaint)
            canvas.drawText("Application ID: ${application.applicationCode}", MARGIN + 12f, y + 2f, sectionPaint)
            canvas.drawText("Status: ${application.parsedStatus.labelEn}  |  Payment: ${application.parsedPaymentStatus.labelEn}", MARGIN + 12f, y + 20f, valuePaint)
            canvas.drawText("Date: $submittedDate", PAGE_WIDTH - MARGIN - 165f, y + 2f, labelPaint)

            y += 56f

            fun drawSectionHeader(title: String) {
                canvas.drawText(title, MARGIN, y, sectionPaint)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                y += 16f
            }

            fun drawTwoColumnRow(l1: String, v1: String, l2: String, v2: String) {
                val colWidth = (PAGE_WIDTH - MARGIN * 2) / 2f
                canvas.drawText(l1, MARGIN, y, labelPaint)
                canvas.drawText(l2, MARGIN + colWidth + 8f, y, labelPaint)
                y += 14f
                canvas.drawText(v1.ifBlank { "—" }.take(40), MARGIN, y, valuePaint)
                canvas.drawText(v2.ifBlank { "—" }.take(40), MARGIN + colWidth + 8f, y, valuePaint)
                y += 10f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                y += 16f
            }

            // Section 1: Customer Details
            drawSectionHeader("1. Customer Details / ग्राहक की जानकारी")
            drawTwoColumnRow("Full Name / पूरा नाम", application.fullName, "Mobile Number / मोबाइल नंबर", application.mobile)
            drawTwoColumnRow("Email Address", application.email, "City & PIN Code", listOf(application.city, application.pinCode).filter { it.isNotBlank() }.joinToString(" - "))
            canvas.drawText("Complete Address / पूरा पता", MARGIN, y, labelPaint)
            y += 14f
            canvas.drawText(application.address.ifBlank { "—" }.take(86), MARGIN, y, valuePaint)
            y += 10f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 22f

            // Section 2: Vehicle Details
            drawSectionHeader("2. EV / Scooty Vehicle Details")
            drawTwoColumnRow("Vehicle Brand", application.vehicleBrand, "Vehicle Model", application.vehicleModel)
            drawTwoColumnRow("Scooty 26 / Registration No.", application.registrationNumber, "Battery Number / बैटरी नंबर", application.batteryNumber)
            if (application.vehicleNotes.isNotBlank()) {
                canvas.drawText("Additional Vehicle Info", MARGIN, y, labelPaint)
                y += 14f
                canvas.drawText(application.vehicleNotes.take(86), MARGIN, y, valuePaint)
                y += 16f
            }
            y += 8f

            // Section 3: Payment Information
            drawSectionHeader("3. Payment & Verification Details")
            val amountDisplay = "₹${(payment?.amount ?: settings.defaultAmount).toLong()}"
            val paymentUpi = payment?.upiId?.ifBlank { settings.upiId } ?: settings.upiId
            val utr = payment?.utrNumber?.ifBlank { "Pending / Not Provided" } ?: "Not Submitted"
            val payStatusText = payment?.parsedStatus?.labelEn ?: application.parsedPaymentStatus.labelEn
            drawTwoColumnRow("Applicable Amount", amountDisplay, "UPI ID", paymentUpi)
            drawTwoColumnRow("UTR / Reference No.", utr, "Payment Verification Status", payStatusText)
            if (application.adminRemarks.isNotBlank()) {
                canvas.drawText("Admin Remarks", MARGIN, y, labelPaint)
                y += 14f
                canvas.drawText(application.adminRemarks.take(88), MARGIN, y, valuePaint)
                y += 18f
            }
            if (application.policyNumber.isNotBlank() && application.parsedStatus.name == "POLICY_ISSUED") {
                drawTwoColumnRow("Issued Policy Number", application.policyNumber, "Policy Status", "ISSUED BY RAAG AUTOMOBILES")
            }

            // Disclaimer footer on Page 1
            val footerY = PAGE_HEIGHT - 38f
            canvas.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, linePaint)
            val disclaimer = if (application.parsedStatus.name == "POLICY_ISSUED") {
                "Verified record generated by ${settings.businessName}. Policy No: ${application.policyNumber}"
            } else {
                "Note: Application submission does not constitute active insurance coverage until payment is verified and policy is officially issued."
            }
            canvas.drawText(disclaimer.take(98), MARGIN, footerY, labelPaint)
            canvas.drawText("Page 1", PAGE_WIDTH - MARGIN - 40f, footerY, labelPaint)
            pdf.finishPage(page)

            // --- PAGE 2+: EMBEDDED SUBMITTED PHOTOGRAPHS & PAYMENT SCREENSHOT ---
            val imageDocs = documents.filter { it.mimeType.startsWith("image/") && it.payloadBase64.isNotBlank() }
            if (imageDocs.isNotEmpty()) {
                val cellWidth = (PAGE_WIDTH - MARGIN * 2 - 16f) / 2f
                val cellHeight = 205f
                val perPage = 6

                imageDocs.chunked(perPage).forEach { chunk ->
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdf.startPage(pageInfo)
                    canvas = page.canvas

                    canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 54f, headerBgPaint)
                    canvas.drawLine(0f, 54f, PAGE_WIDTH.toFloat(), 54f, goldLinePaint)
                    canvas.drawText("${settings.businessName} — Submitted Photos", MARGIN, 34f, titlePaint)

                    var topY = 76f
                    chunk.forEachIndexed { idx, doc ->
                        val col = idx % 2
                        val row = idx / 2
                        val left = MARGIN + col * (cellWidth + 16f)
                        val top = topY + row * (cellHeight + 18f)
                        val cardRect = RectF(left, top, left + cellWidth, top + cellHeight)
                        canvas.drawRoundRect(cardRect, 6f, 6f, boxStrokePaint)

                        val bmp = ImageCompressionUtil.decodeBase64ToBitmap(doc.payloadBase64)
                        if (bmp != null) {
                            val imgArea = RectF(left + 6f, top + 6f, left + cellWidth - 6f, top + cellHeight - 28f)
                            val fitted = fitCenterRect(bmp, imgArea)
                            canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), fitted, null)
                            bmp.recycle()
                        }

                        val caption = DocumentType.fromRaw(doc.docType).labelEn
                        canvas.drawText(caption, left + 8f, top + cellHeight - 10f, labelPaint)
                    }

                    canvas.drawText("App ID: ${application.applicationCode}  |  Page $pageNumber", MARGIN, PAGE_HEIGHT - 24f, labelPaint)
                    pdf.finishPage(page)
                }
            }

            val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeCode = application.applicationCode.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val outFile = File(dir, "RAAG_EV_Insurance_${safeCode}.pdf")
            FileOutputStream(outFile).use { pdf.writeTo(it) }
            pdf.close()
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fitCenterRect(bmp: Bitmap, bounds: RectF): RectF {
        val bw = bmp.width.toFloat().coerceAtLeast(1f)
        val bh = bmp.height.toFloat().coerceAtLeast(1f)
        val scale = minOf(bounds.width() / bw, bounds.height() / bh)
        val w = bw * scale
        val h = bh * scale
        val left = bounds.left + (bounds.width() - w) / 2f
        val top = bounds.top + (bounds.height() - h) / 2f
        return RectF(left, top, left + w, top + h)
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    fun shareOrOpenPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Save or Share RAAG Insurance PDF"))
    }
}
