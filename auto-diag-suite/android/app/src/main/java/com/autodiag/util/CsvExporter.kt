package com.autodiag.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta el historial de datos en vivo a un archivo CSV y lo comparte vía
 * Intent (correo, Drive, WhatsApp, etc.). Sin dependencias externas ni base de
 * datos: escribe a cacheDir/exports y usa FileProvider para el Uri seguro.
 */
object CsvExporter {

    /**
     * @param history mapa PID -> serie de valores (misma longitud aproximada).
     * Devuelve el archivo generado o null si no había datos.
     */
    fun export(context: Context, history: Map<String, List<Float>>): File? {
        if (history.isEmpty()) return null

        val columns = history.keys.toList()
        val rows = history.values.maxOf { it.size }
        if (rows == 0) return null

        val sb = StringBuilder()
        sb.append("muestra,").append(columns.joinToString(",")).append('\n')
        for (i in 0 until rows) {
            sb.append(i)
            columns.forEach { col ->
                val series = history[col]
                val v = series?.getOrNull(i)
                sb.append(',').append(v?.toString() ?: "")
            }
            sb.append('\n')
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "autodiag_$stamp.csv")
        file.writeText(sb.toString())
        return file
    }

    /** Lanza el selector de compartir para el CSV generado. */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar datos CSV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
