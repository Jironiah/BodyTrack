package com.example.cuerpo_peso.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.cuerpo_peso.AdapterFotosActuales
import com.example.cuerpo_peso.databinding.FragmentPreviewFotosBinding
import com.example.cuerpo_peso.db.DB
import com.example.cuerpo_peso.db.imagen
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext


class preview_fotos : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentPreviewFotosBinding

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var db: DB
    private var job: Job = Job()

    private val REQUEST_PER_CAMERA: Int = 100

    private var totsPermisos = false

    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imagenList: ArrayList<imagen>
    private lateinit var adapterFotosActuales: AdapterFotosActuales


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPreviewFotosBinding.inflate(inflater)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btCapturaPhoto.setOnClickListener {
            takePhoto()
            Toast.makeText(requireContext(), "Imagen Capturada", Toast.LENGTH_SHORT).show()
        }
        comprovaPermisCamera()

        if (totsPermisos) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "No tienes permiso de tomar fotos", Toast.LENGTH_SHORT)
                .show()
        }
        return binding.root
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {
        var preview = Preview.Builder().build()

        var cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview.setSurfaceProvider(binding.pvImagenesCuerpo.surfaceProvider)

        imageCapture = ImageCapture.Builder().build()

        cameraProvider!!.unbindAll()

        val camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner, cameraSelector, preview, imageCapture
        )
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    private fun comprovaPermisCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            totsPermisos = true
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(), Manifest.permission.CAMERA
                )
            ) totsPermisos = false
            else {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    ), REQUEST_PER_CAMERA
                )
            }
        }
    }

    private fun getFile(dirPath: File): Int {
        var count = 0
        val f = dirPath
        val files = f.listFiles()
        if (files != null) for (i in files.indices) {
            count++
            val file = files[i]
            if (file.isDirectory()) {
                getFile(file)
            }
        }
        return count
    }

    private fun takePhoto() {
        val (dia, mes) = obtenerDiaYMesActual()
        val directoryPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Cuerpo_Peso/ImagenesCuerpo/$mes/$dia"
        )
        if (!directoryPath.exists()) {
            directoryPath.mkdirs()
        }

        //Si en la carpeta ImagenesCuerpo hay 3 imagenes no permitirá hacer más fotos
        if (getFile(directoryPath) < 3) {
            val imageCapture = imageCapture ?: return

            var name: String? = null
            if (getFile(directoryPath) == 0) {
                name = "Frente"
            } else if (getFile(directoryPath) == 1) {
                name = "Lado"
            } else if (getFile(directoryPath) == 2) {
                name = "Espalda"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(
                    MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Cuerpo_Peso/ImagenesCuerpo/$mes/$dia"
                )
            }
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            requireContext(), "No se ha podido almacenar", Toast.LENGTH_SHORT
                        ).show()
                        Log.e("onErrorCamera", exception.message.toString())
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val nameListFiles = ArrayList<String>()
                        directoryPath.listFiles()?.forEach { file ->
                            nameListFiles.add(file.name)
                        }
                        var image =
                            imagen(null, dia, mes, "", outputFileResults.savedUri.toString(), null)
                        if (nameListFiles.size == 1) {
                            image = imagen(
                                null, dia, mes, "Frente", outputFileResults.savedUri.toString(), 1
                            )
                        } else if (nameListFiles.size == 2) {
                            image = imagen(
                                null, dia, mes, "Lado", outputFileResults.savedUri.toString(), 2
                            )
                        } else if (nameListFiles.size == 3) {
                            image = imagen(
                                null, dia, mes, "Espalda", outputFileResults.savedUri.toString(), 3
                            )
                        }
//                        val image = imagen(
//                            null, dia, mes, "Imagen_Cuerpo", outputFileResults.savedUri.toString()
//                        )

                        runBlocking {
                            val corrutina = launch {
                                db = DB.getDatabase(requireContext())
                                imagenList = db.DAO().getImagenes() as ArrayList<imagen>

                                if (imagenList.size > 2) {
                                    Toast.makeText(
                                        requireContext(),
                                        "No puedes hacer más de 3 fotos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    db.DAO().insertImage(image)
                                }
                            }
                            corrutina.join()
                            adapterFotosActuales =
                                AdapterFotosActuales(imagenList, coroutineContext)
                            adapterFotosActuales.updateList(imagenList)
                        }
                    }
                })
            //Toast.makeText(requireContext(), getFile(directoryPath).toString(), Toast.LENGTH_SHORT)
            //    .show()
        } else {
            Toast.makeText(requireContext(), "No puedes hacer más de 3 fotos", Toast.LENGTH_SHORT)
                .show()
        }

    }

    /**
     * Función que devuelve el dia y el mes actual
     * @return dos variables int que seran la fecha (dia y mes)
     */
    fun obtenerDiaYMesActual(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val dia = calendar.get(Calendar.DAY_OF_MONTH)
        val mes = calendar.get(Calendar.MONTH) + 1 // Los meses comienzan desde 0, por eso se suma 1

        return Pair(dia, mes)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PER_CAMERA) {
            if (grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED) totsPermisos =
                true
            else totsPermisos = false
        }
    }
}