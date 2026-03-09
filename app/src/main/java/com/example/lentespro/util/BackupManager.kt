package com.example.lentespro.util

import android.content.Context
import android.net.Uri
import com.example.lentespro.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Gestor de copias de seguridad para LentesPro.
 * Permite empaquetar la base de datos Room (.db, -wal, -shm) 
 * y guardarla en el destino que el usuario elija (Google Drive, WhatsApp, etc).
 */
object BackupManager {

    /**
     * Exporta la base de datos actual como un solo archivo .db consolidado.
     * Si usas SQLite moderno (WAL), es mejor forzar un checkpoint primero.
     */
    fun exportBackup(context: Context, targetUri: Uri) {
        try {
            // 1. Asegurar que los datos temporales se graben en el archivo .db
            AppDatabase.get(context).checkpoint()

            // 2. Localizar el archivo de base de datos
            val dbFile = context.getDatabasePath("lentespro.db")

            // 3. Escribir en el URI de destino (el usuario eligió Drive, carpeta local, etc)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Restaura una base de datos desde un archivo .db seleccionado por el usuario.
     * ¡OJO! Esto sobrescribe el inventario actual. 
     */
    fun importBackup(context: Context, sourceUri: Uri) {
        try {
            // 1. Cerrar la base de datos actual antes de sobrescribir
            AppDatabase.get(context).close()

            val dbFile = context.getDatabasePath("lentespro.db")
            
            // Borrar archivos auxiliares para evitar conflictos
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
