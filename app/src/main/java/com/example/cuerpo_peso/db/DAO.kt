package com.example.cuerpo_peso.db

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Dao
interface DAO {
    //imagen
    @Query("select * from imagen order by orden asc")
    suspend fun getImagenes(): List<imagen>

    @Query("select * from imagen where uri = :imageUri")
    suspend fun getImageByUri(imageUri: String): imagen

//    @Query("select * from imagen where dia =:dia and mes =:mes")
//    suspend fun getImagenesByFecha(dia: Int, mes: Int): List<imagen>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(imagen: imagen)

    @Delete
    suspend fun deleteImage(img: imagen)

    @Update
    suspend fun updateImage(img: imagen)

    //imagenes_texto
    @Query("select * from imagenes_list")
    suspend fun getImagenesList(): List<imagenes_list>

    @Query("select * from imagenes_list where dia =:dia and mes =:mes")
    suspend fun getImagenesListByFecha(dia: Int, mes: Int): imagenes_list

    @Query("select peso from imagenes_list where dia =:dia and mes =:mes")
    suspend fun getPesoImagenesListByFecha(dia: Int, mes: Int): Double

    @Query("select distinct dia from imagenes_list")
    suspend fun getDiasImagenesList(): List<Int>

    @Query("select distinct mes from imagenes_list")
    suspend fun getMesesImagenesList(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImagenesList(imgList: imagenes_list)

    @Delete
    suspend fun deleteImagenesList(imgList: imagenes_list)

    //Para  el backup
    @Query("SELECT * FROM imagen")
    suspend fun getImagenesForBackup(): List<imagen>

    @Query("SELECT * FROM imagenes_list")
    suspend fun getImagenesListForBackup(): List<imagenes_list>

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun backupDatabase(context: Context) {
        val db = DB.getDatabase(context)
        val exportDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "backupCuerpoPeso"
        )
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val fileImagen = File(exportDir, "imagen.csv")
        val fileImagenesList = File(exportDir, "imagenes_list.csv")
        val outputStreamImagen = withContext(Dispatchers.IO) {
            FileOutputStream(fileImagen)
        }

        val outputStreamImagenesList = withContext(Dispatchers.IO) {
            FileOutputStream(fileImagenesList)
        }

        val writerImagen = OutputStreamWriter(outputStreamImagen)
        val writerImagenesList = OutputStreamWriter(outputStreamImagenesList)

        // Exportar tabla imagen
        db.DAO().getImagenesForBackup().forEach { imagen ->
            writerImagen.write("${imagen.id},${imagen.dia},${imagen.mes},${imagen.nombre_imagen},${imagen.uri},${imagen.orden}\n")
        }
        withContext(Dispatchers.IO) {
            writerImagen.flush()
        }
        withContext(Dispatchers.IO) {
            writerImagen.close()
        }

        // Exportar tabla imagenes_list
        db.DAO().getImagenesListForBackup().forEach { imagenesList ->
            writerImagenesList.write("${imagenesList.id},${imagenesList.dia},${imagenesList.mes},${imagenesList.imagen1},${imagenesList.imagen2},${imagenesList.imagen3},${imagenesList.peso}\n")
        }
        withContext(Dispatchers.IO) {
            writerImagenesList.flush()
        }
        withContext(Dispatchers.IO) {
            writerImagenesList.close()
        }
        val folderPathToZip: File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Cuerpo_Peso"
        )
        val zipPath: File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "BackupCuerpoPeso/backup.zip"
        )
        zipFolder(folderPathToZip.path, zipPath.path)
        //Fin backup ficheros imagenes
        Log.e("Backup", "Backup hecha")
    }

    fun zipFolder(folderPath: String, zipPath: String) {
        val folder = File(folderPath)
        if (!folder.exists()) {
            return
        }

        val files = folder.listFiles()
        val outputStream = FileOutputStream(zipPath)
        val zipOutputStream = ZipOutputStream(outputStream)

        for (file in files) {
            if (file.isDirectory) {
                zipSubFolder(file, file.name, zipOutputStream)
            } else {
                addFileToZip(file, zipOutputStream)
            }
        }

        zipOutputStream.close()
    }

    private fun zipSubFolder(folder: File, parentPath: String, zipOutputStream: ZipOutputStream) {
        val files = folder.listFiles()
        for (file in files) {
            val path = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipSubFolder(file, path, zipOutputStream)
            } else {
                addFileToZip(file, path, zipOutputStream)
            }
        }
    }

    private fun addFileToZip(file: File, zipOutputStream: ZipOutputStream) {
        addFileToZip(file, file.name, zipOutputStream)
    }

    private fun addFileToZip(file: File, fileName: String, zipOutputStream: ZipOutputStream) {
        val zipEntry = ZipEntry(fileName)
        zipOutputStream.putNextEntry(zipEntry)
        val inputStream = FileInputStream(file)
        inputStream.copyTo(zipOutputStream)
        inputStream.close()
        zipOutputStream.closeEntry()
    }


    suspend fun restoreDatabase(context: Context) {

    }
}