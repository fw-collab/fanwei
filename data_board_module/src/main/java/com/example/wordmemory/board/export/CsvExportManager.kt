package com.example.wordmemory.board.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.wordmemory.data.local.entity.ReviewRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CsvExportInfo(
    val uri: Uri,
    val fileName: String,
    val location: String,
    val recordCount: Int,
)

/**
 * 复习记录 CSV 导出器。
 *
 * Android 10+ 使用 MediaStore.Downloads 写入公共下载目录，不需要存储权限。
 * Android 9 及以下没有 MediaStore.Downloads，降级到应用专属 Download 目录，
 * 通过 FileProvider 提供可读 URI，也不申请存储权限。
 */
class CsvExportManager(private val context: Context) {

    suspend fun export(records: List<ReviewRecord>): Result<CsvExportInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "review_records_${
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                }.csv"
                val csv = buildCsv(records)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportToMediaStore(fileName, csv, records.size)
                } else {
                    exportToAppDownload(fileName, csv, records.size)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun exportToMediaStore(
        fileName: String,
        csv: String,
        recordCount: Int,
    ): Result<CsvExportInfo> {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore 创建文件失败")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(csv.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("无法打开 CSV 输出流")

            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)

            return Result.success(
                CsvExportInfo(
                    uri = uri,
                    fileName = fileName,
                    location = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME/$fileName",
                    recordCount = recordCount,
                )
            )
        } catch (e: Exception) {
            // 写入失败时清理 MediaStore 中的半成品，避免留下 pending 文件
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun exportToAppDownload(
        fileName: String,
        csv: String,
        recordCount: Int,
    ): Result<CsvExportInfo> {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "exports")
        val dir = File(baseDir, FOLDER_NAME)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("无法创建导出目录")
        }

        val file = File(dir, fileName)
        file.writeText(csv, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.board.fileprovider",
            file,
        )

        return Result.success(
            CsvExportInfo(
                uri = uri,
                fileName = fileName,
                location = file.absolutePath,
                recordCount = recordCount,
            )
        )
    }

    /** 复习记录表全部字段：id / word_id / reviewed_at / result / quality / duration_seconds。 */
    private fun buildCsv(records: List<ReviewRecord>): String {
        val builder = StringBuilder()
        builder.append("id,word_id,reviewed_at,result,quality,duration_seconds\r\n")
        records.forEach { record ->
            builder.append(record.id).append(',')
                .append(record.wordId).append(',')
                .append(record.reviewedAt).append(',')
                .append(if (record.result) 1 else 0).append(',')
                .append(record.quality).append(',')
                .append(record.durationSeconds)
                .append("\r\n")
        }
        return builder.toString()
    }

    companion object {
        private const val MIME_TYPE = "text/csv"
        private const val FOLDER_NAME = "WordMemory"
    }
}
