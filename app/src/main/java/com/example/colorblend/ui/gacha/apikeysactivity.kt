package com.example.colorblend.ui.gacha

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R
import com.example.colorblend.data.local.ApiKeysManager

class ApiKeysActivity : AppCompatActivity() {

    data class CampoConfig(
        val key: String,
        val etId: Int,
        val btnPegarId: Int,
        val btnBorrarId: Int,
        val btnInfoId: Int,
        val urlDocs: String,
        val urlTutorial: String? = null,
        val instrucciones: String
    )

    private val campos: List<CampoConfig> by lazy {
        listOf(
            CampoConfig(
                key           = ApiKeysManager.KEY_GROQ,
                etId          = R.id.etGroq,
                btnPegarId    = R.id.btnPegarGroq,
                btnBorrarId   = R.id.btnBorrarGroq,
                btnInfoId     = R.id.btnInfoGroq,
                urlDocs       = "https://console.groq.com/keys",
                instrucciones = """
                    1. Ve a console.groq.com
                    2. Crea una cuenta gratis
                    3. En el menú izquierdo: "API Keys"
                    4. Clic en "Create API Key"
                    5. Copia la key (solo se muestra una vez)
                    
                    ✅ Es gratis con límites generosos
                    🎯 Usada para: Chat con personajes y validación de metas
                    
                    ¿Dudas? Escribe a:
                    elangelmiguelmr@gmail.com
                """.trimIndent()
            ),
            CampoConfig(
                key           = ApiKeysManager.KEY_IGDB_CLIENT_ID,
                etId          = R.id.etIgdbId,
                btnPegarId    = R.id.btnPegarIgdbId,
                btnBorrarId   = R.id.btnBorrarIgdbId,
                btnInfoId     = R.id.btnInfoIgdbId,
                urlDocs       = "https://dev.twitch.tv/console/apps",
                urlTutorial   = "https://api-docs.igdb.com/#account-creation",
                instrucciones = """
                    IGDB usa credenciales de Twitch Developer:
                    
                    1. Ve a dev.twitch.tv/console/apps
                    2. Inicia sesión con cuenta de Twitch
                       (o crea una en twitch.tv — es gratis)
                    3. Clic en "Register Your Application"
                    4. Completa el formulario:
                       • Nombre: cualquiera (ej: "MiApp")
                       • OAuth Redirect URL: http://localhost
                       • Categoría: Application Integration
                    5. Clic en "Create"
                    6. Copia el "Client ID" que aparece
                    
                    ⚠ También necesitas el Access Token (campo de abajo)
                    🎯 Usada para: Personajes de videojuegos
                    
                    ¿Dudas? Escribe a:
                    elangelmiguelmr@gmail.com
                """.trimIndent()
            ),
            CampoConfig(
                key           = ApiKeysManager.KEY_IGDB_TOKEN,
                etId          = R.id.etIgdbToken,
                btnPegarId    = R.id.btnPegarIgdbToken,
                btnBorrarId   = R.id.btnBorrarIgdbToken,
                btnInfoId     = R.id.btnInfoIgdbToken,
                urlDocs       = "https://dev.twitch.tv/console/apps",
                urlTutorial   = "https://api-docs.igdb.com/#account-creation",
                instrucciones = """
                    Después de obtener el Client ID y Client Secret de Twitch:
                    
                    Abre esta URL en tu navegador (reemplaza los valores):
                    
                    https://id.twitch.tv/oauth2/token
                      ?client_id=TU_CLIENT_ID
                      &client_secret=TU_CLIENT_SECRET
                      &grant_type=client_credentials
                    
                    Del JSON que devuelve, copia el valor de "access_token".
                    
                    ⚠ El token expira cada ~60 días — tendrás que renovarlo
                    🎯 Usada para: Personajes de videojuegos
                    
                    ¿Dudas? Escribe a:
                    elangelmiguelmr@gmail.com
                """.trimIndent()
            ),
            CampoConfig(
                key           = ApiKeysManager.KEY_GIANTBOMB,
                etId          = R.id.etGiantBomb,
                btnPegarId    = R.id.btnPegarGiantBomb,
                btnBorrarId   = R.id.btnBorrarGiantBomb,
                btnInfoId     = R.id.btnInfoGiantBomb,
                urlDocs       = "https://www.giantbomb.com/api/",
                instrucciones = """
                    1. Ve a giantbomb.com/api
                    2. Crea una cuenta gratis en GiantBomb
                    3. Inicia sesión
                    4. En la página de API verás tu key automáticamente
                    5. Cópiala
                    
                    ✅ Es gratis
                    🎯 Usada para: Personajes adicionales de videojuegos
                    
                    ¿Dudas? Escribe a:
                    elangelmiguelmr@gmail.com
                """.trimIndent()
            ),
            CampoConfig(
                key        = ApiKeysManager.KEY_SERVIDOR_URL,
                etId       = R.id.etServidorUrl,
                btnPegarId = R.id.btnPegarServidorUrl,
                btnBorrarId = R.id.btnBorrarServidorUrl,
                btnInfoId  = R.id.btnInfoServidorUrl,
                urlDocs    = "https://ngrok.com/download",
                instrucciones = """
        URL de tu servidor de descarga.
        
        1. Descarga e instala el servidor desde el link
        2. Ejecuta servidor.bat con doble click
        3. Copia la URL que aparece (ej: https://xxxx.ngrok-free.app)
        4. Pegala aqui
        
        El servidor debe estar corriendo para descargar musica.
    """.trimIndent()
            ),
            CampoConfig(
                key        = ApiKeysManager.KEY_SERVIDOR_KEY,
                etId       = R.id.etServidorKey,
                btnPegarId = R.id.btnPegarServidorKey,
                btnBorrarId = R.id.btnBorrarServidorKey,
                btnInfoId  = R.id.btnInfoServidorKey,
                urlDocs    = "https://ngrok.com/download",
                instrucciones = """
        API Key de tu servidor (la defines tu mismo).
        
        Cuando configures el servidor.bat puedes poner
        la key que quieras, por ejemplo:
        
        mi-key-secreta-123
        
        Pon aqui la misma key que pusiste en el servidor.
    """.trimIndent()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_keys)

        campos.forEach { configurarCampo(it) }

        findViewById<Button>(R.id.btnGuardarKeys).setOnClickListener { guardarTodas() }
        findViewById<Button>(R.id.btnVolverKeys).setOnClickListener  { finish() }

        // ── Lógica Servidor Colab ─────────────────────────────────────────────
        val btnDownload = findViewById<Button>(R.id.btnDownloadNotebook)
        val btnOpenDrive = findViewById<Button>(R.id.btnOpenDrive)

        btnDownload.setOnClickListener {
            if (descargarNotebook()) {
                btnOpenDrive.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "✅ Notebook descargado en Descargas/", Toast.LENGTH_LONG).show()
            }
        }

        btnOpenDrive.setOnClickListener {
            abrirEnNavegador("https://drive.google.com")
        }
    }

    private fun descargarNotebook(): Boolean {
        return try {
            val assetName = "servidor_descargas.ipynb"
            val inputStream = assets.open(assetName)
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val outFile = java.io.File(downloadsDir, assetName)
            
            val outputStream = java.io.FileOutputStream(outFile)
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Error al exportar notebook", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun configurarCampo(config: CampoConfig) {
        val et        = findViewById<EditText>(config.etId)
        val btnPegar  = findViewById<ImageButton>(config.btnPegarId)
        val btnBorrar = findViewById<ImageButton>(config.btnBorrarId)
        val btnInfo   = findViewById<ImageButton>(config.btnInfoId)

        val guardada = ApiKeysManager.get(this, config.key)
        if (guardada.isNotBlank()) et.setText(guardada)

        btnInfo.setOnClickListener {
            val builder = AlertDialog.Builder(this)
                .setTitle("¿Cómo obtener esta key?")
                .setMessage(config.instrucciones)
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Abrir web") { _, _ ->
                    abrirEnNavegador(config.urlDocs)
                }

            if (config.urlTutorial != null) {
                builder.setNeutralButton("Ver tutorial") { _, _ ->
                    abrirEnNavegador(config.urlTutorial)
                }
            }

            builder.show()
        }

        btnPegar.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val texto = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!texto.isNullOrBlank()) {
                et.setText(texto.trim())
                Toast.makeText(this, "✅ Pegado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
            }
        }

        btnBorrar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Limpiar key")
                .setMessage("¿Borrar la key guardada?")
                .setPositiveButton("Borrar") { _, _ ->
                    et.setText("")
                    ApiKeysManager.clear(this, config.key)
                    Toast.makeText(this, "Key borrada", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun abrirEnNavegador(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            `package` = obtenerNavegadorPorDefecto()
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun obtenerNavegadorPorDefecto(): String? {
        val intent    = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
        val resolvers = packageManager.queryIntentActivities(intent, 0)
        return resolvers
            .map { it.activityInfo.packageName }
            .firstOrNull { it != packageName && it != "android" }
    }

    private fun guardarTodas() {
        var guardadas = 0
        campos.forEach { config ->
            val valor = findViewById<EditText>(config.etId).text.toString().trim()
            if (valor.isNotBlank()) {
                ApiKeysManager.set(this, config.key, valor)
                guardadas++
            }
        }
        if (guardadas > 0) {
            Toast.makeText(this, "✅ $guardadas key${if (guardadas != 1) "s" else ""} guardada${if (guardadas != 1) "s" else ""}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No hay cambios que guardar", Toast.LENGTH_SHORT).show()
        }
    }
}