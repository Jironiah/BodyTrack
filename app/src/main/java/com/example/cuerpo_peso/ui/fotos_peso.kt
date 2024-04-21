package com.example.cuerpo_peso.ui

//import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cuerpo_peso.AdapterFotosActuales
import com.example.cuerpo_peso.R
import com.example.cuerpo_peso.databinding.FragmentFotosPesoBinding
import com.example.cuerpo_peso.db.DB
import com.example.cuerpo_peso.db.imagen
import com.example.cuerpo_peso.db.imagenes_list
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.CoroutineContext


class fotos_peso : Fragment(), CoroutineScope, OnBackStackChangedListener {

    private lateinit var imagen_list_actual: ArrayList<imagen>
    private lateinit var imagen_list_antigua: imagenes_list
    private lateinit var binding: FragmentFotosPesoBinding
    private lateinit var db: DB
    private var job: Job = Job()
    private lateinit var adapterFotosActuales: AdapterFotosActuales
    private lateinit var popupOpciones: PopupWindow
    private var popupBackupIsShowing = false
    private var popupEliminarIsShowing = false


    //    private val PICK_FILE_REQUEST_CODE = 101
    private val REQUEST_CODE_INSTALL_APK = 123


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBackStackChanged() {
        if (::popupOpciones.isInitialized) {
            popupOpciones.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        fragmentManager?.addOnBackStackChangedListener(this)
        // Inflate the layout for this fragment
        binding = FragmentFotosPesoBinding.inflate(inflater)
        initRecyclerViewImagenesActuales()
        //Esto guarda la imagen con el peso
        binding.btSaveImagenPeso.setOnClickListener {
            //Instanciar la fecha (dia y mes)
            val (dia, mes) = obtenerDiaYMesActual()
            //Coger peso
            val peso = binding.etPeso.text.toString().toDoubleOrNull()

            runBlocking {
                val corrutina = launch {
                    try {
                        db = DB.getDatabase(requireContext())
                        imagen_list_actual = db.DAO().getImagenes() as ArrayList<imagen>

                        //Comprobaciones de nulos y si no hay más de 3 fotos que no permita almacenar la lista de imagenes
                        if (peso != null || imagen_list_actual != null || mes != null || imagen_list_actual[0].uri != null || imagen_list_actual[1].uri != null || imagen_list_actual[2].uri != null || imagen_list_actual.size != null) {
                            if (imagen_list_actual.size == 3) {
                                db.DAO().insertImagenesList(
                                    imagenes_list(
                                        null,
                                        dia,
                                        mes,
                                        imagen_list_actual[0].uri,
                                        imagen_list_actual[1].uri,
                                        imagen_list_actual[2].uri,
                                        peso!!
                                    )
                                )
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Todavía no tienes las 3 fotos",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else if (peso == null || dia == null || mes == null || imagen_list_actual[0].uri == null || imagen_list_actual[1].uri == null || imagen_list_actual[2].uri == null) {
                            Toast.makeText(
                                requireContext(),
                                "El peso escrito no es correcto o la casilla está vacía",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Problema con la base de datos, contacta con Sean",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        if (peso != null || dia != null || mes != null || imagen_list_actual[0].uri != null || imagen_list_actual[1].uri != null || imagen_list_actual[2].uri != null || imagen_list_actual.size != null) {
                            if (imagen_list_actual.size == 3) {
                                // Crear la subcarpeta si no existe
                                val subcarpeta = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                    "Cuerpo_Peso/CuerpoPeso/$mes/$dia"
                                )
                                Log.e(
                                    "RutaCarpetaCuerpo_Peso",
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
                                )
                                if (!subcarpeta.exists()) {
                                    subcarpeta.mkdirs()
                                }
                                // Guardar las imágenes en la subcarpeta
                                var contador = 0
                                imagen_list_actual.forEach { img ->
                                    var nombreArchivo = ""
                                    when (contador) {
                                        0 -> {
                                            nombreArchivo = "Frente.jpg"
                                        }

                                        1 -> {
                                            nombreArchivo = "Lado.jpg"
                                        }

                                        2 -> {
                                            nombreArchivo = "Espalda.jpg"
                                        }
                                    }
                                    cambiarNombreImagenes(subcarpeta, nombreArchivo, img, contador)
                                    // Eliminar la imagen de la base de datos
                                    db.DAO().deleteImage(img)
                                    contador++
                                }
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "La carpeta no se ha creado",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        //Comprobar si el tamaño de la lista de imágenes no es 3 no permite crear los directorios

                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(), "No has introducido imágenes", Toast.LENGTH_LONG
                        ).show()
                        Log.e("Grabar imagenes_peso", e.message.toString())
                    }
                }
                corrutina.join()
                initRecyclerViewImagenesActuales()
            }
            //Limpiar etiqueta peso
            binding.etPeso.text.clear()
        }



        binding.btEliminarImagenes.setOnClickListener {
            mostrarPopupEliminar(container)
        }

        binding.btActualizarAplicacion.setOnClickListener {
            mostrarPopupBackup(container)
        }
        cargarImagenesAyer()
        return binding.root
    }


    private fun mostrarPopupEliminar(container: ViewGroup?) {
        if (!popupEliminarIsShowing) {
            popupEliminarIsShowing = true

            val inflater = requireActivity().layoutInflater
            val popupOpcionesView = inflater.inflate(R.layout.popup_eliminar_fotos, null)

            popupOpciones = PopupWindow(
                popupOpcionesView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Get the display size
            val display = requireActivity().windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)

            // Get the location of the container view
            val location = IntArray(2)
            container!!.getLocationOnScreen(location)

            // Calculate X and Y coordinates for centering the popup
            val popupX = location[0] + (container.width / 2) - (popupOpcionesView.width / 2)
            val popupY = location[1] + (container.height / 2) - (popupOpcionesView.height / 2)

            // Check if calculated Y position goes off-screen (above the top)
            val adjustedY = Math.max(0, popupY)

            popupOpciones.showAtLocation(container, Gravity.NO_GRAVITY, popupX, adjustedY)

            // Handle the popup menu items
            popupOpcionesView.findViewById<Button>(R.id.bt_cancelar_eliminar_imagenes)
                ?.setOnClickListener {
                    popupOpciones.dismiss()
                }

            popupOpciones.setOnDismissListener {
                popupEliminarIsShowing = false
            }
            popupOpcionesView.findViewById<Button>(R.id.bt_eliminar_imagenes)?.setOnClickListener {
                val (dia, mes) = obtenerDiaYMesActual()
                val subcarpeta = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Cuerpo_Peso/ImagenesCuerpo/$mes/$dia"
                )
                try {
                    val files = subcarpeta.listFiles()
                    val nameListFiles = ArrayList<String>()
                    if (files != null) {
                        files.forEach { file ->
                            nameListFiles.add(file.name)
                            file.delete()
                        }
                    }
                    runBlocking {
                        val corrutina = launch {
                            db = DB.getDatabase(requireContext())
                            imagen_list_actual = db.DAO().getImagenes() as ArrayList<imagen>
                            imagen_list_actual.forEach { imagen ->
                                db.DAO().deleteImage(imagen)
                            }
                        }
                        corrutina.join()
                    }
                } catch (e: Exception) {
                    Log.e("Eliminar Imagenes", "No se han eliminado las imagenes")
                }
                initRecyclerViewImagenesActuales()
                popupOpciones.dismiss()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarPopupBackup(container: ViewGroup?) {

        if (!popupBackupIsShowing) {
            popupBackupIsShowing = true

            val inflater = requireActivity().layoutInflater
            val popupOpcionesView = inflater.inflate(R.layout.popup_backup, null)

            popupOpciones = PopupWindow(
                popupOpcionesView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Get the display size
            val display = requireActivity().windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)

            // Get the location of the container view
            val location = IntArray(2)
            container!!.getLocationOnScreen(location)

            // Calculate X and Y coordinates for centering the popup
            val popupX = location[0] + (container.width / 2) - (popupOpcionesView.width / 2)
            val popupY = location[1] + (container.height / 2) - (popupOpcionesView.height / 2)

            // Check if calculated Y position goes off-screen (above the top)
            val adjustedY = Math.max(0, popupY)

            popupOpciones.showAtLocation(container, Gravity.NO_GRAVITY, popupX, adjustedY)


            // Handle the popup menu items
            popupOpcionesView.findViewById<Button>(R.id.btCancelarBackup)?.setOnClickListener {
                popupOpciones.dismiss()
            }

            popupOpciones.setOnDismissListener {
                popupBackupIsShowing = false
            }

            popupOpcionesView.findViewById<Button>(R.id.btHacerBackup)?.setOnClickListener {
                runBlocking {
                    val corrutina = launch {
                        db = DB.getDatabase(requireContext())
                        db.DAO().backupDatabase(requireContext())
                    }
                    corrutina.join()
                }
                popupOpciones.dismiss()
            }

            popupOpcionesView.findViewById<Button>(R.id.btRecuperarBackup)?.setOnClickListener {
                runBlocking {
                    val corrutina = launch {
                        db = DB.getDatabase(requireContext())
                        db.DAO().restoreDatabase(requireContext())
                    }
                    corrutina.join()
                }
                initRecyclerViewImagenesActuales()
                popupOpciones.dismiss()
            }
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

    /**
     * Función que almacena imagenes y les cambia el nombre
     * @param subcarpeta es la ruta del directorio donde almacenar las imagenes
     * @param nombreArchivo es el nombre de la imagen a almacenar y cambiar el nombre
     * @param img es la imagen a almacenar y modificar
     * @param contador permitirá identificar cada imagen
     *
     */
    private fun cambiarNombreImagenes(
        subcarpeta: File, nombreArchivo: String, img: imagen, contador: Int
    ) {
        try {
            val uriImagen = Uri.parse(img.uri)
            val inputStream = requireActivity().contentResolver.openInputStream(uriImagen)
            val archivoImagen = File(subcarpeta, "${Date().time}_$nombreArchivo")

            // Comprobar si el archivo ya existe
            if (!archivoImagen.exists()) {
                val outputStream = FileOutputStream(archivoImagen)
                val bytesLeidos = inputStream?.readBytes()
                outputStream.write(bytesLeidos)
                outputStream.close()
                inputStream?.close()

                var nuevoNombre = ""

                when (contador) {
                    0 -> {
                        // Renombrar el archivo
                        nuevoNombre = "Frente.jpg"
                    }

                    1 -> {
                        // Renombrar el archivo
                        nuevoNombre = "Lado.jpg"
                    }

                    2 -> {
                        // Renombrar el archivo
                        nuevoNombre = "Espalda.jpg"
                    }
                }
                val nuevoArchivo = File(subcarpeta, nuevoNombre)
                archivoImagen.renameTo(nuevoArchivo)
            }
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "Ha habido un problema al almacenar las imagenes",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    /**
     * Función que vacía el contenido de un directorio
     */
    private fun vaciarImagenesCuerpo() {
        // Eliminar contenido
        val directoryPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString() + "/Cuerpo_Peso/ImagenesCuerpo"
        val files = File(directoryPath).listFiles()
        files?.forEach { file ->
            file.delete()
        }
        Toast.makeText(requireContext(), "ImagenesCuerpo vacía", Toast.LENGTH_SHORT).show()
    }


    override fun onResume() {
        super.onResume()
        initRecyclerViewImagenesActuales()
    }

    /**
     *Función que recarga la información del RecyclerView con nueva información
     */
    private fun initRecyclerViewImagenesActuales() {
        runBlocking {
            val corrutina = launch {
                db = DB.getDatabase(requireContext())
                imagen_list_actual = db.DAO().getImagenes() as ArrayList<imagen>
            }
            corrutina.join()
            binding.rvImagenesActuales.layoutManager = GridLayoutManager(context, 3)
            adapterFotosActuales = AdapterFotosActuales(imagen_list_actual, coroutineContext)
            binding.rvImagenesActuales.adapter =
                adapterFotosActuales  // Update the existing adapter
        }
    }

    /**
     *Función que muestra las imágenes de ayer
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun cargarImagenesAyer() {
        runBlocking {
            val corrutina = launch {
                db = DB.getDatabase(requireContext())
                val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")
                val date = LocalDate.parse(LocalDate.now().toString(), formatter).minusDays(1)
//                Toast.makeText(
//                    requireContext(), "${date.dayOfMonth} ${date.monthValue}", Toast.LENGTH_LONG
//                ).show()

                //Aquí cargaré las imágenes de ayer
                imagen_list_antigua =
                    db.DAO().getImagenesListByFecha(date.dayOfMonth, date.monthValue)
                Log.i("Dia-Mes", "$date.dayOfMonth   $date.month")
                try {
                    binding.ivImagenAntigua1.setImageURI(Uri.parse(imagen_list_antigua.imagen1))
                    binding.ivImagenAntigua2.setImageURI(Uri.parse(imagen_list_antigua.imagen2))
                    binding.ivImagenAntigua3.setImageURI(Uri.parse(imagen_list_antigua.imagen3))

                    binding.tvPesoAntiguo.text = imagen_list_antigua.peso.toString()
                } catch (e: Exception) {
                    Log.e("ImagenesAntiguas", e.message.toString())
                }
            }
            corrutina.join()
        }
    }

    fun iniciarActualizacion(context: Context) {
        Log.d("Actualizacion", "Iniciando proceso de actualización...")
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val currentVersionCode = obtenerVersionCode(context)
        val savedVersionCode = sharedPreferences.getInt("VersionCode", -1)
        Log.e("currentVersionCode", currentVersionCode.toString())
        Log.e("savedVersionCode", savedVersionCode.toString())
        if (currentVersionCode > savedVersionCode) {
            Log.d("Actualizacion", "Nueva versión disponible")
            // Generar copia de seguridad antes de actualizar
            crearBackup(context)

            // Actualizar el SharedPreferences con la nueva versión
//            sharedPreferences.edit().putInt("VersionCode", currentVersionCode).apply()
            sharedPreferences.edit().putInt("VersionCode", currentVersionCode).apply()


            // Iniciar el proceso de actualización
            iniciarProcesoActualizacion()
        } else {
            Log.d("Actualizacion", "La versión actual es la más reciente")
        }
    }

    private fun crearBackup(context: Context) {
        Log.d("Backup", "Creando backup...")

        // Verificar permisos
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Pedir permisos si no están concedidos
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )
        } else {

            // Guardar backup de la base de datos
            val backupDirectory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Cuerpo_Peso/backup"
            )

            if (!backupDirectory.exists()) {
                if (backupDirectory.mkdirs()) {
                    Log.d("Backup", "Directorio de backup creado")
                } else {
                    Log.e("Backup", "Error al crear el directorio de backup")
                }
            }

            val backupFile = File(backupDirectory, "backup.db")
            val currentDBPath = "/data/data/com.example.cuerpo_peso/databases/peso_y_cuerpo.db"

            Log.e("UbicacionDB", currentDBPath)

            if (File(currentDBPath).exists()) {
                try {
                    File(currentDBPath).copyTo(backupFile, overwrite = true)
                    Toast.makeText(context, "Backup realizado con éxito", Toast.LENGTH_SHORT).show()
                    Log.d("Backup", "Copia de la base de datos realizada con éxito")
                } catch (e: Exception) {
                    Log.e("Backup", "Error al copiar la base de datos", e)
                    Toast.makeText(context, "Error al realizar el backup", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Log.e("Backup", "La base de datos no existe")
                Toast.makeText(context, "Error al realizar el backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }


    private fun iniciarProcesoActualizacion() {
        val apkPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Cuerpo_Peso.apk"
        )

        Log.d("APKPath", apkPath.absolutePath)  // Verifica la ruta del archivo APK

        if (apkPath.exists()) {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().applicationContext.packageName}.provider",
                apkPath
            )
            Log.d("FileProviderURI", uri.toString())  // Verifica el URI del FileProvider

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

            // Iniciar la actividad con startActivityForResult
            startActivityForResult(intent, REQUEST_CODE_INSTALL_APK)
        } else {
            Log.e("APKPath", "El archivo APK no existe")
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            1 -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // Permiso concedido, crear la copia de seguridad
//                    crearBackup(requireContext())
//                } else {
//                    // Permiso denegado, mostrar un mensaje al usuario
//                    Toast.makeText(
//                        requireContext(), "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }


    //    private fun iniciarProcesoActualizacion(context: Context) {
//        // Ruta de la carpeta de documentos de WhatsApp
//        val whatsappPath = File(
//            Environment.getExternalStorageDirectory().absolutePath + "/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents/Sent","Cuerpo_Peso.apk"
//        )
//        Toast.makeText(requireContext(), Environment.getExternalStorageDirectory().absolutePath, Toast.LENGTH_LONG).show()
//        // Buscar el archivo APK en la carpeta de documentos de WhatsApp
////        val apkFile = whatsappPath.listFiles()?.find {
////            it.name.endsWith("Cuerpo_Peso.apk", true)
////        }
//
//        if (whatsappPath != null && whatsappPath.exists()) {
//            // Mover el archivo APK a tu directorio de aplicación
//            val appDirectory = File(
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//                "Cuerpo_Peso"
//            )
//            if (!appDirectory.exists()) {
//                appDirectory.mkdirs()
//            }
//
//            val newApkFile = File(appDirectory, "Cuerpo_Peso.apk")
//            whatsappPath.copyTo(newApkFile, overwrite = true)
//
//            // Instalar el APK desde tu directorio de aplicación
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.setDataAndType(
//                FileProvider.getUriForFile(
//                    context, "${context.applicationContext.packageName}.provider", newApkFile
//                ), "application/vnd.android.package-archive"
//            )
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
//            context.startActivity(intent)
//        } else {
//            Toast.makeText(
//                context,
//                "Archivo APK no encontrado en la ubicación especificada",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
}