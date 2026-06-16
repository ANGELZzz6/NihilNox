package com.example.colorblend.ui.gacha

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.colorblend.ui.gacha.buildGlideUrl
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.JikanRepository
import com.example.colorblend.domain.model.ImagenPersonaje
import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.domain.model.Rareza
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ColeccionPersonajesAdapter(
    private var personajes: List<PersonajeObtenido> = emptyList(),
    private val lifecycleOwner: LifecycleOwner,
    private val activity: AppCompatActivity
) : RecyclerView.Adapter<ColeccionPersonajesAdapter.ViewHolder>() {

    private var onImagenesSeleccionadas: ((List<String>) -> Unit)? = null

    private val imagePickerLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val rutas = mutableListOf<String>()
                val data = result.data
                if (data?.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val uri = data.clipData!!.getItemAt(i).uri
                        val ruta = copiarImagenAlStorage(uri.toString())
                        if (ruta != null) rutas.add(ruta)
                    }
                } else if (data?.data != null) {
                    val ruta = copiarImagenAlStorage(data.data.toString())
                    if (ruta != null) rutas.add(ruta)
                }
                onImagenesSeleccionadas?.invoke(rutas)
            }
        }

    private fun copiarImagenAlStorage(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return null
            val fileName = "personaje_img_${System.currentTimeMillis()}.jpg"
            val file = File(activity.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getImagenesIGDB(
        personajeId: Int,
        nombre: String,
        context: android.content.Context
    ): List<String> {
        return try {
            val dao = AppDatabase.getDatabase(context).imagenPersonajeDao()
            val guardadas = dao.getImagenesPorPersonaje(personajeId)
            if (guardadas.isNotEmpty()) return guardadas.map { it.imageUrl }

            val url = java.net.URL("https://api.igdb.com/v4/characters")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Client-ID", "b2q8jmruxiegx56k954evi1gbkk848")
            connection.setRequestProperty("Authorization", "Bearer hwmdjkhk5zi7c24cmu3d11nq72jous")
            connection.setRequestProperty("Content-Type", "text/plain")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val query = """
                search "$nombre";
                fields name, mug_shot.url;
                limit 5;
            """.trimIndent()

            val writer = java.io.OutputStreamWriter(connection.outputStream)
            writer.write(query)
            writer.flush()
            writer.close()

            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) return emptyList()

            val response = connection.inputStream.bufferedReader().readText()
            val results = org.json.JSONArray(response)

            val urls = (0 until results.length()).mapNotNull { i ->
                val mugShot = results.getJSONObject(i).optJSONObject("mug_shot") ?: return@mapNotNull null
                val imagenRaw = mugShot.optString("url").ifEmpty { return@mapNotNull null }
                val imagenUrl = if (imagenRaw.startsWith("//")) "https:$imagenRaw" else imagenRaw
                imagenUrl.replace("thumb", "cover_big")
            }

            val entidades = urls.map { ImagenPersonaje(personajeId = personajeId, imageUrl = it) }
            dao.insertAll(entidades)
            urls
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun update(nuevos: List<PersonajeObtenido>) {
        personajes = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(personajes[position])
    }

    override fun getItemCount() = personajes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagen = itemView.findViewById<ImageView>(R.id.charImage)
        private val nombre = itemView.findViewById<TextView>(R.id.charName)
        private val score  = itemView.findViewById<TextView>(R.id.charScore)

        fun bind(personaje: PersonajeObtenido) {
            nombre.text = personaje.nombre
            score.text  = "⭐ ${personaje.favoritos}"

            val urlOFichero = personaje.imagenUrl
            if (urlOFichero.startsWith("/")) {
                // Imagen local descargada (superhéroes)
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(java.io.File(urlOFichero))
                    .into(imagen)
            } else {
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(buildGlideUrl(urlOFichero))
                    .into(imagen)
            }

            com.bumptech.glide.Glide.with(itemView.context)
                .load(personaje.imagenUrl)
                .into(imagen)

            val (emoji, colorHex) = when (personaje.rareza) {
                Rareza.LEGENDARIO -> "🟡 Legendario" to "#FFD700"
                Rareza.EPICO      -> "🟣 Épico"      to "#9B59B6"
                Rareza.RARO       -> "🔵 Raro"       to "#3498DB"
                Rareza.COMUN      -> "⚪ Común"      to "#95A5A6"
            }

            itemView.findViewById<TextView>(R.id.charRareza)?.text = emoji

            val borde = itemView.findViewById<View>(R.id.rarezaBorde)
            GradientDrawable().apply {
                setStroke(4, Color.parseColor(colorHex))
                cornerRadius = 10f
                setColor(Color.TRANSPARENT)
                borde.background = this
            }

            val glow = itemView.findViewById<View>(R.id.rarezaGlow)
            GradientDrawable().apply {
                setStroke(3, Color.parseColor(colorHex))
                cornerRadius = 13f
                setColor(Color.TRANSPARENT)
                glow.background = this
            }

            itemView.findViewById<View>(R.id.rarezaStripe)
                ?.setBackgroundColor(Color.parseColor(colorHex))

            itemView.setOnClickListener { mostrarDialog(personaje, colorHex) }
        }

        private fun mostrarDialog(personaje: PersonajeObtenido, colorHex: String) {
            val context = itemView.context
            val dialog = Dialog(context, R.style.DialogCartaPersonaje)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_personaje)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val viewPager    = dialog.findViewById<ViewPager2>(R.id.dialogViewPager)
            val indicador    = dialog.findViewById<TextView>(R.id.dialogIndicador)
            val dialogNombre = dialog.findViewById<TextView>(R.id.dialogNombre)
            val dialogFavs   = dialog.findViewById<TextView>(R.id.dialogFavoritos)
            val btnCargar    = dialog.findViewById<Button>(R.id.btnCargarImagenes)
            val btnMisFotos  = dialog.findViewById<Button>(R.id.btnMisFotos)
            val dialogCerrar = dialog.findViewById<Button>(R.id.dialogCerrar)
            val stripe       = dialog.findViewById<View>(R.id.dialogRarezaStripe)

            stripe?.setBackgroundColor(Color.parseColor(colorHex))

            val pagerAdapter = ImagenPagerAdapter(listOf(personaje.imagenUrl))
            viewPager.adapter = pagerAdapter
            indicador.text = "1 / 1"

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    indicador.text = "${position + 1} / ${pagerAdapter.itemCount}"
                }
            })

            dialogNombre.text = personaje.nombre
            dialogFavs.text   = "⭐ ${personaje.favoritos} favoritos"

            val jikanRepo = JikanRepository(
                AppDatabase.getDatabase(context).imagenPersonajeDao()
            )

            lifecycleOwner.lifecycleScope.launch {
                val imagenesGuardadas = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(context)
                        .imagenPersonajeDao()
                        .getImagenesPorPersonaje(personaje.id)
                }
                if (imagenesGuardadas.isNotEmpty()) {
                    val urls = listOf(personaje.imagenUrl) + imagenesGuardadas.map { it.imageUrl }
                    pagerAdapter.update(urls)
                    indicador.text = "1 / ${urls.size}"
                    btnCargar.visibility = View.GONE
                } else {
                    if (personaje.categoria == "superhero" || personaje.categoria == "videojuego") {
                        btnCargar.visibility = View.GONE
                    }
                }
            }

            btnCargar.setOnClickListener {
                btnCargar.isEnabled = false
                btnCargar.text = "Cargando..."
                lifecycleOwner.lifecycleScope.launch {
                    val urls = withContext(Dispatchers.IO) {
                        when (personaje.categoria) {
                            "superhero"  -> jikanRepo.getImagenes(personaje.id, personaje.nombre)
                                .ifEmpty { listOf(personaje.imagenUrl) }
                            "videojuego" -> getImagenesIGDB(personaje.id, personaje.nombre, context)
                            else         -> jikanRepo.getImagenes(personaje.id, personaje.nombre)
                        }
                    }
                    if (urls.isEmpty() || urls == listOf(personaje.imagenUrl)) {
                        Toast.makeText(context, "No se encontraron imágenes extra", Toast.LENGTH_SHORT).show()
                        btnCargar.isEnabled = true
                        btnCargar.text = "🖼 Cargar más imágenes"
                    } else {
                        val urlsConOriginal = listOf(personaje.imagenUrl) + urls
                        pagerAdapter.update(urlsConOriginal)
                        indicador.text = "1 / ${urlsConOriginal.size}"
                        btnCargar.visibility = View.GONE
                    }
                }
            }

            btnMisFotos.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                onImagenesSeleccionadas = { rutas ->
                    if (rutas.isEmpty()) {
                        Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                    } else {
                        lifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val dao = AppDatabase.getDatabase(context).imagenPersonajeDao()
                                rutas.forEach { ruta ->
                                    dao.insert(ImagenPersonaje(
                                        personajeId = personaje.id,
                                        imageUrl    = ruta,
                                        esLocal     = true
                                    ))
                                }
                            }
                            val todas = withContext(Dispatchers.IO) {
                                AppDatabase.getDatabase(context)
                                    .imagenPersonajeDao()
                                    .getImagenesPorPersonaje(personaje.id)
                            }
                            val urls = listOf(personaje.imagenUrl) + todas.map { it.imageUrl }
                            pagerAdapter.update(urls)
                            indicador.text = "1 / ${urls.size}"
                            Toast.makeText(context, "✅ ${rutas.size} imagen(es) guardada(s)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                imagePickerLauncher.launch(intent)
            }

            dialogCerrar.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
}