package com.example.cuerpo_peso.ui

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import com.example.cuerpo_peso.R
import com.example.cuerpo_peso.databinding.FragmentListarBinding
import com.example.cuerpo_peso.db.DB
import com.example.cuerpo_peso.db.imagen
import com.example.cuerpo_peso.db.imagenes_list
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

class listar : Fragment(), CoroutineScope, OnBackStackChangedListener {
    private lateinit var binding: FragmentListarBinding
    private lateinit var diasString: ArrayList<String>
    private lateinit var mesesString: ArrayList<String>
    private lateinit var popupOpciones: PopupWindow
    private var popupEliminarIsShowing = false

    private lateinit var db: DB
    private var job: Job = Job()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        fragmentManager?.addOnBackStackChangedListener(this)
        // Inflate the layout for this fragment
        binding = FragmentListarBinding.inflate(inflater)
        val (dia, mes) = obtenerDiaYMesActual()


        runBlocking {
            val corrutina = launch {
                db = DB.getDatabase(requireContext())
                val dias = db.DAO().getDiasImagenesList() as ArrayList<Int>
                val meses = db.DAO().getMesesImagenesList() as ArrayList<Int>
                diasString = arrayListOf()
                mesesString = arrayListOf()

                dias.forEach { dia ->
                    diasString.add(dia.toString())
                }

                meses.forEach { mes ->
                    mesesString.add(mes.toString())

                }

                recargarSpìnners()

                var dia = dia.toString()
                var mes = mes.toString()
//                val (diaBusqueda, mesBusqueda) = Pair(null, null)
                binding.spinnerFechasDias.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        dia = parent?.getItemAtPosition(position).toString()
                        actualizarLista(dia, mes)
//                        Toast.makeText(requireContext(), "Dia cambiado", Toast.LENGTH_SHORT).show()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        TODO("Not yet implemented")

                    }
                }

                binding.spinnerFechasMeses.onItemSelectedListener =
                    object : OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?, view: View?, position: Int, id: Long
                        ) {
                            mes = parent?.getItemAtPosition(position).toString()
                            actualizarLista(dia, mes)
//                            Toast.makeText(requireContext(), "Mes cambiado", Toast.LENGTH_SHORT)
//                                .show()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("Not yet implemented")
                        }
                    }
            }
            corrutina.join()
        }

        /**
         * Aquí vacía la db de imagenes_list y el directorio cuerpoPeso
         */
        binding.btEliminarImagenesList.setOnClickListener {
            mostrarPopupEliminar(container)
            actualizarLista(dia.toString(), mes.toString())
        }
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
                runBlocking {
                    val corrutina = launch {
                        try {
                            val mes =
                                Integer.parseInt(binding.spinnerFechasMeses.selectedItem.toString())
                            val dia =
                                Integer.parseInt(binding.spinnerFechasDias.selectedItem.toString())
                            val db = DB.getDatabase(requireContext())
                            val listEliminar = db.DAO().getImagenesListByFecha(dia, mes)
                            val subcarpetaCuerpoPeso = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Cuerpo_Peso/CuerpoPeso/$mes/$dia"
                            )
                            val subcarpetaImagenesCuerpo = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Cuerpo_Peso/ImagenesCuerpo/$mes/$dia"
                            )
                            val filesCuerpoPeso = subcarpetaCuerpoPeso.listFiles()
                            val filesImagenesCuerpo = subcarpetaImagenesCuerpo.listFiles()
//                        val nameListFiles = ArrayList<String>()
                            if (filesCuerpoPeso != null) {
                                filesCuerpoPeso.forEach { file ->
//                                nameListFiles.add(file.name)
                                    Log.e("nombre cuerpoPeso", file.name)
                                    file.delete()
                                }
                            }
                            if (filesImagenesCuerpo != null) {
                                filesImagenesCuerpo.forEach { file ->
                                    Log.e("nombre imagenesCuerpo", file.name)
                                    file.delete()
                                }
                            }
                            db.DAO().deleteImagenesList(listEliminar)
                            Toast.makeText(
                                requireContext(), "Has eliminado este registro", Toast.LENGTH_SHORT
                            ).show()
                            recargarSpìnners()
                            binding.imagen1.setImageResource(android.R.color.transparent)
                            binding.imagen2.setImageResource(android.R.color.transparent)
                            binding.imagen3.setImageResource(android.R.color.transparent)
                            binding.tvPeso.text = ""
                            Toast.makeText(
                                requireContext(),
                                "Has eliminado la lista de imágenes",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e(
                                "EliminarImagenesList", e.message.toString()
                            )
                        }
                    }
                    corrutina.join()

                }
                popupOpciones.dismiss()
            }

        }
    }

    /**
     * Función para recargar los spinners dia y mes
     */
    private fun recargarSpìnners() {
        //Adaptador desplegable spinner dias
        if (binding.spinnerFechasDias != null) {
            val adapter = ArrayAdapter(
                requireContext(), R.layout.spinner_item, diasString
            )
            binding.spinnerFechasDias.adapter = adapter
        }

        //Adaptador desplegable spinner meses
        if (binding.spinnerFechasMeses != null) {
            val adapter = ArrayAdapter(
                requireContext(), R.layout.spinner_item, mesesString
            )
            binding.spinnerFechasMeses.adapter = adapter
        }
    }

    /**
     * Función que actualiza la vista de la lista de imagenes
     * @param dia es el dia a buscar
     * @param mes es el mes a buscar
     */
    private fun actualizarLista(dia: String, mes: String) {
        runBlocking {
            val corrutina = launch {
                val todosRegistros = db.DAO().getImagenesList() as ArrayList<imagenes_list>
                try {
                    if (todosRegistros.any { it.dia == Integer.parseInt(dia) } && todosRegistros.any {
                            it.mes == Integer.parseInt(
                                mes
                            )
                        }) {
                        val imagenList = db.DAO()
                            .getImagenesListByFecha(Integer.parseInt(dia), Integer.parseInt(mes))
                        val peso = imagenList.peso.toString()

                        if (imagenList != null) {
                            binding.imagen1.setImageURI(Uri.parse(imagenList.imagen1))
                            binding.imagen2.setImageURI(Uri.parse(imagenList.imagen2))
                            binding.imagen3.setImageURI(Uri.parse(imagenList.imagen3))
                            binding.tvPeso.text = peso
                        }
                    } else {
                        Log.e(
                            "cargarList",
                            "Aún no hay registros que mostrar en esta fechaAún no hay registros que mostrar en esta fecha"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Error Cargar Lista", e.message.toString())
                }
            }
            corrutina.join()
        }
    }

    /**
     * Función que devuelve el dia y el mes actual
     * @return dos variables int que seran la fecha (dia y mes)
     */
    private fun obtenerDiaYMesActual(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val dia = calendar.get(Calendar.DAY_OF_MONTH)
        val mes = calendar.get(Calendar.MONTH) + 1

        return Pair(dia, mes)
    }
}