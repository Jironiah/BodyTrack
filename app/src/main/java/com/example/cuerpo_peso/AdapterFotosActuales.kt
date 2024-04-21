package com.example.cuerpo_peso

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cuerpo_peso.db.DB
import com.example.cuerpo_peso.db.imagen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.util.Calendar
import kotlin.coroutines.CoroutineContext


class AdapterFotosActuales(private var list: ArrayList<imagen>, override val coroutineContext: CoroutineContext) : RecyclerView.Adapter<AdapterFotosActuales.ViewHolder>(), CoroutineScope {
    private lateinit var db: DB

    class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val imagenes = vista.findViewById<ImageView>(R.id.iv_imagenes_cuerpo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context)
        return ViewHolder(layout.inflate(R.layout.cardview_imagenes_peso, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (list[position].uri != null) {
            holder.imagenes.setImageURI(Uri.parse(list[position].uri))
        }
        holder.itemView.setOnClickListener {
//            Create and show the popup
            val popupOpciones = PopupMenu(holder.itemView.context, holder.itemView)
            popupOpciones.inflate(R.menu.popup_fotos_peso)
            popupOpciones.show()
            popupOpciones.menu.getItem(0).title = list[position].nombre_imagen

//            Handle the popup menu items
            popupOpciones.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.imgName -> {
                        popupOpciones.dismiss()
                        showPopupMenu(holder.itemView.context, holder.itemView, list[position])
                        true
                    }

                    R.id.btClosePopupFotosPeso -> {
                        popupOpciones.dismiss()
                        true
                    }

                    R.id.btDeleteImageFotosPeso -> {
                        runBlocking {
                            val corrutina = launch {
                                db = DB.getDatabase(holder.itemView.context)
                                try {
                                    val img = db.DAO().getImageByUri(list[position].uri!!)
                                    val subcarpeta = File(
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                        "ImagenesCuerpo/${img.mes}/${img.dia}"
                                    )
                                    val files = subcarpeta.listFiles()
                                    if (files != null) {
                                        files.forEach { file ->
//                                            nameListFiles.add(file.name)
                                            if (file.name == img.nombre_imagen + ".jpg") {
                                                Log.e("Imagen Encontrada", img.nombre_imagen)
                                                file.delete()
                                                Toast.makeText(
                                                    holder.itemView.context,
                                                    "Se ha eliminado la imagen del directorio",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Log.e(
                                                    "Imagen No Encontrada", img.nombre_imagen
                                                )
                                            }
                                        }
                                    }
                                    db.DAO().deleteImage(img)
                                    list = db.DAO().getImagenes() as ArrayList<imagen>
                                    updateList(list)
                                } catch (e: Exception) {
                                    Log.e("EliminarImagen", e.message.toString())
                                }
                            }
                            corrutina.join()
                        }
                        true

                    }

                    else -> false
                }
            }
        }
    }

    fun showPopupMenu(context: Context, view: View, img: imagen) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.popup_cambiar_nombre_imagenes, null)
        val (dia, mes) = obtenerDiaYMesActual()

        // initialize the EditText field
        val nombre = layout.findViewById<EditText>(R.id.et_nuevo_nombre)
        val btAceptar = layout.findViewById<Button>(R.id.bt_confirmar_nombre)
        val btCancelar = layout.findViewById<Button>(R.id.bt_cancelar)
        // create a PopupWindow
        val popup = PopupWindow(
            layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        // set the background color of the PopupWindow
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.color.white))

        // set a touch listener on the popup window so it will be dismissed when touched outside
        popup.isOutsideTouchable = true
        popup.isTouchable = true


        btAceptar.setOnClickListener {
            val subcarpeta = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ImagenesCuerpo/$mes/$dia"
            )
            runBlocking {
                val corrutina = launch {
                    try {
                        db = DB.getDatabase(context)
                        val image = db.DAO().getImageByUri(img.uri)
                        cambiarNombreImagenes(
                            subcarpeta,
                            image.nombre_imagen + ".jpg",
                            nombre.text.toString(),
                            context
                        )
                        val updateImage = image.copy(nombre_imagen = nombre.text.toString())
                        db.DAO().updateImage(updateImage)
                        val newList = db.DAO().getImagenes() as ArrayList<imagen>
                        updateList(newList)
                    } catch (e: Exception) {
                        Log.e("Actualizar Imagen", e.message.toString())
                    }
                }
                corrutina.join()
            }
            popup.dismiss()
        }
        nombre.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val subcarpeta = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "ImagenesCuerpo/$mes/$dia"
                )
                runBlocking {
                    val corrutina = launch {
                        try {
                            db = DB.getDatabase(context)
                            val image = db.DAO().getImageByUri(img.uri)
                            cambiarNombreImagenes(
                                subcarpeta,
                                image.nombre_imagen + ".jpg",
                                nombre.text.toString(),
                                context
                            )
                            val updateImage = image.copy(nombre_imagen = nombre.text.toString())
                            db.DAO().updateImage(updateImage)
                            val newList = db.DAO().getImagenes() as ArrayList<imagen>
                            updateList(newList)
                        } catch (e: Exception) {
                            Log.e("Actualizar Imagen", e.message.toString())
                        }
                    }
                    corrutina.join()
                }
                popup.dismiss()
                Toast.makeText(context, "Has actualizado con Enter", Toast.LENGTH_SHORT).show()
                return@OnEditorActionListener true
            }
            false
        })

        btCancelar.setOnClickListener {
            popup.dismiss()
        }
        // display the popup window at the specified location
        popup.showAsDropDown(view)
    }

    fun updateList(newList: ArrayList<imagen>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun cambiarNombreImagenes(
        subcarpeta: File, nombreArchivo: String, nuevoNombre: String, context: Context
    ) {
        try {
            val archivoImagen = File(subcarpeta, nombreArchivo)

            // Comprobar si el archivo ya existe
            val nuevoArchivo = File(subcarpeta, "$nuevoNombre.jpg")
            archivoImagen.renameTo(nuevoArchivo)

        } catch (e: IOException) {
            Toast.makeText(
                context,
                "Ha habido un problema al cambiar el nombre de las imagenes",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun obtenerDiaYMesActual(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val dia = calendar.get(Calendar.DAY_OF_MONTH)
        val mes = calendar.get(Calendar.MONTH) + 1 // Los meses comienzan desde 0, por eso se suma 1

        return Pair(dia, mes)
    }

    override fun getItemCount() = list.size
}