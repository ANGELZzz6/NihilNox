import sys
import os
import subprocess

# Parchear subprocess ANTES de cualquier otro import
if sys.platform == "win32":
    import ctypes
    CREATE_NO_WINDOW = 0x08000000
    _orig_popen = subprocess.Popen
    class _SilentPopen(_orig_popen):
        def __init__(self, *args, **kwargs):
            kwargs.setdefault('creationflags', 0)
            kwargs['creationflags'] |= CREATE_NO_WINDOW
            kwargs['startupinfo'] = subprocess.STARTUPINFO()
            kwargs['startupinfo'].dwFlags |= subprocess.STARTF_USESHOWWINDOW
            kwargs['startupinfo'].wShowWindow = 0
            super().__init__(*args, **kwargs)
    subprocess.Popen = _SilentPopen
    hwnd = ctypes.windll.kernel32.GetConsoleWindow()
    if hwnd:
        ctypes.windll.user32.ShowWindow(hwnd, 0)

import shutil
import json as json_mod
import customtkinter as ctk
import threading
import uvicorn
import nest_asyncio
from pyngrok import ngrok
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse
from pydantic import BaseModel
import uuid
import yt_dlp
import pystray
from pystray import MenuItem as item
from PIL import Image, ImageDraw

# ── Config persistente ───────────────────────────────────────
CONFIG_FILE  = os.path.join(os.path.expanduser("~"), ".nihilnox_config.json")
COOKIES_FILE = os.path.join(os.path.expanduser("~"), ".nihilnox_cookies.txt")

def cargar_config():
    try:
        if os.path.exists(CONFIG_FILE):
            with open(CONFIG_FILE, "r") as f:
                return json_mod.load(f)
    except:
        pass
    return {}

def guardar_config(data: dict):
    try:
        with open(CONFIG_FILE, "w") as f:
            json_mod.dump(data, f, indent=2)
    except Exception as e:
        print(f"Error guardando config: {e}")

_config = cargar_config()
NGROK_TOKEN  = _config.get("ngrok_token", "")
NGROK_DOMAIN = _config.get("ngrok_domain", "")
API_KEY      = _config.get("api_key", "")
FFMPEG_PATH  = _config.get("ffmpeg_path", r"C:\Users\elang\.spotdl\ffmpeg.exe")
PORT         = 8000

nest_asyncio.apply()
api = FastAPI(title="NihilNox Server")

_ui_log = None

def ui_log(msg: str):
    if _ui_log:
        _ui_log(msg)

class DescargaRequest(BaseModel):
    url: str

class BuscarCancionRequest(BaseModel):
    query: str

def limpiar(path: str):
    try:
        if path and os.path.exists(path):
            os.remove(path)
            ui_log(f"🗑️ Eliminado: {os.path.basename(path)}")
    except Exception as e:
        ui_log(f"⚠️ No se pudo eliminar: {e}")

@api.get("/")
async def root():
    return {"status": "online", "message": "NihilNox Server is running"}

@api.get("/health")
def health():
    return {"status": "ok"}

@api.post("/download-instagram")
async def download_instagram(payload: dict, background_tasks: BackgroundTasks):
    url = payload.get("url")
    if not url:
        raise HTTPException(status_code=400, detail="URL missing")

    temp_id = str(uuid.uuid4())
    output  = f"video_{temp_id}.mp4"

    ui_log(f"🎬 [INSTAGRAM] Descargando: {url[:60]}...")

    ydl_opts = {
        'format': 'best[ext=mp4]/best[ext=webm]/best',
        'outtmpl': output,
        'quiet': False,
        'no_warnings': False,
        'socket_timeout': 30,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        },
        'extractor_args': {
            'instagram': {
                'android_client_id': 'com.instagram.android',
            }
        }
    }
    if os.path.exists(COOKIES_FILE):
        ydl_opts['cookiefile'] = COOKIES_FILE

    try:
        ui_log(f"📥 Iniciando descarga...")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)

        if not os.path.exists(output):
            raise Exception("Archivo no se creó")

        size = os.path.getsize(output)
        ui_log(f"✅ [INSTAGRAM] Video listo: {os.path.basename(output)} ({size // 1024} KB)")
        background_tasks.add_task(limpiar, output)
        return FileResponse(path=output, media_type='video/mp4', filename="video.mp4")

    except Exception as e:
        error_msg = str(e)
        ui_log(f"❌ [INSTAGRAM] Error: {error_msg}")
        if os.path.exists(output):
            try:
                os.remove(output)
            except:
                pass
        raise HTTPException(status_code=500, detail=error_msg)

@api.post("/descargar")
def descargar(req: DescargaRequest):
    ui_log(f"🎵 Descargar solicitado: {req.url[:60]}...")
    if os.path.exists("musica"):
        shutil.rmtree("musica")
    os.makedirs("musica", exist_ok=True)
    fallidos = []
    if "spotify.com" in req.url:
        try:
            resultado = subprocess.run(
                ["py", "-3.12", "-m", "spotdl", req.url, "--output", "musica/{list-name}/{title}"],
                capture_output=True, text=True, timeout=600
            )
            if resultado.returncode != 0:
                raise HTTPException(status_code=500, detail=resultado.stderr or "Error spotdl")
        except subprocess.TimeoutExpired:
            raise HTTPException(status_code=500, detail="Timeout descargando de Spotify")
        except FileNotFoundError:
            raise HTTPException(status_code=500, detail="spotdl no instalado")
        resultados = []
        nombre_playlist = ""
        for root, dirs, files in os.walk("musica"):
            for f in files:
                if f.endswith((".mp3", ".m4a", ".opus")):
                    ruta = os.path.join(root, f)
                    titulo = os.path.splitext(f)[0]
                    resultados.append({"titulo": titulo, "ruta": ruta})
                    if not nombre_playlist:
                        partes = os.path.relpath(root, "musica").split(os.sep)
                        if partes:
                            nombre_playlist = partes[0]
        ui_log(f"✅ Spotify: {len(resultados)} canciones")
        return {"playlist": nombre_playlist, "canciones": resultados, "total": len(resultados), "fallidos": fallidos}
    else:
        ydl_opts = {
            "format": "bestaudio/best",
            "outtmpl": "musica/%(playlist_title,title)s/%(title)s.%(ext)s",
            "quiet": True, "no_warnings": True, "ignoreerrors": True,
            "sleep_interval": 2, "max_sleep_interval": 4,
        }
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(req.url, download=True)
                if info is None:
                    raise HTTPException(status_code=500, detail="No se pudo obtener info")
                for entry in info.get("entries", [info]):
                    if entry is None:
                        fallidos.append("(video no disponible)")
        except HTTPException:
            raise
        except Exception as e:
            ui_log(f"❌ Error playlist: {str(e)}")
            raise HTTPException(status_code=500, detail=str(e))
        resultados = []
        for root, dirs, files in os.walk("musica"):
            for f in files:
                if f.endswith((".m4a", ".webm", ".mp3")):
                    ruta = os.path.join(root, f)
                    titulo = os.path.splitext(f)[0]
                    resultados.append({"titulo": titulo, "ruta": ruta})
        ui_log(f"✅ YouTube playlist: {len(resultados)} canciones")
        return {"playlist": info.get("title", ""), "canciones": resultados, "total": len(resultados), "fallidos": fallidos}

@api.post("/buscar-cancion")
def buscar_cancion(req: BuscarCancionRequest):
    ui_log(f"🔍 Buscando: {req.query[:60]}")
    carpeta = "musica_spotify"
    os.makedirs(carpeta, exist_ok=True)
    for f in os.listdir(carpeta):
        try:
            os.remove(os.path.join(carpeta, f))
        except:
            pass
    query_mejorada = f"{req.query} official audio"
    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": f"{carpeta}/%(title)s.%(ext)s",
        "quiet": True, "no_warnings": True, "ignoreerrors": False,
        "default_search": "ytsearch1", "noplaylist": True,
        "ffmpeg_location": FFMPEG_PATH,
        "postprocessors": [{"key": "FFmpegExtractAudio", "preferredcodec": "mp3", "preferredquality": "192"}],
    }
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(query_mejorada, download=True)
            if info is None:
                raise HTTPException(status_code=404, detail="No se encontró la canción")
            if "entries" in info:
                entries = [e for e in info["entries"] if e is not None]
                if not entries:
                    raise HTTPException(status_code=404, detail="No se encontraron resultados")
                info = entries[0]
            archivo_encontrado = None
            titulo = ""
            for f in os.listdir(carpeta):
                ruta_f = os.path.join(carpeta, f)
                if os.path.isfile(ruta_f):
                    archivo_encontrado = ruta_f
                    titulo = os.path.splitext(f)[0]
                    break
            if not archivo_encontrado:
                raise HTTPException(status_code=500, detail="Archivo no encontrado tras descarga")
            ui_log(f"✅ Encontrada: {titulo}")
            return {"titulo": titulo, "ruta": archivo_encontrado}
    except HTTPException:
        raise
    except Exception as e:
        ui_log(f"❌ Error buscar-cancion: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@api.post("/info-cancion")
def info_cancion(req: DescargaRequest):
    ui_log(f"ℹ️ Info canción: {req.url[:60]}")
    try:
        resultado = subprocess.run(
            ["py", "-3.12", "-m", "spotdl", "meta", req.url],
            capture_output=True, text=True, timeout=30
        )
        titulo = ""
        artista = ""
        for linea in resultado.stdout.splitlines():
            linea = linea.strip()
            if linea.lower().startswith("title:"):
                titulo = linea.split(":", 1)[1].strip()
            elif linea.lower().startswith("artist:"):
                artista = linea.split(":", 1)[1].strip()
        if titulo and artista:
            return {"query": f"{artista} - {titulo}"}
        resultado2 = subprocess.run(
            ["py", "-3.12", "-m", "spotdl", "save", req.url, "--save-file", "temp_info.spotdl"],
            capture_output=True, text=True, timeout=30
        )
        if os.path.exists("temp_info.spotdl"):
            with open("temp_info.spotdl", "r", encoding="utf-8") as f:
                data = json_mod.load(f)
            os.remove("temp_info.spotdl")
            if isinstance(data, list) and len(data) > 0:
                cancion = data[0]
                titulo = cancion.get("name", "")
                artistas = cancion.get("artists", [])
                artista = artistas[0] if artistas else ""
                if titulo and artista:
                    return {"query": f"{artista} - {titulo}"}
        raise HTTPException(status_code=404, detail="No se pudo obtener info de la canción")
    except HTTPException:
        raise
    except subprocess.TimeoutExpired:
        raise HTTPException(status_code=500, detail="Timeout")
    except Exception as e:
        ui_log(f"❌ Error info-cancion: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@api.post("/download-youtube-video")
async def download_youtube_video(payload: dict, background_tasks: BackgroundTasks):
    url = payload.get("url")
    if not url:
        raise HTTPException(status_code=400, detail="URL missing")

    temp_id = str(uuid.uuid4())
    output  = f"video_{temp_id}.mp4"

    ui_log(f"🎬 [YOUTUBE-VIDEO] Descargando video: {url[:60]}...")

    ydl_opts = {
        'format': 'best[ext=mp4]/best',
        'outtmpl': output,
        'quiet': False,  # ← DEPURACIÓN
        'no_warnings': False,
        'socket_timeout': 30,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
    }
    if os.path.exists(COOKIES_FILE):
        ydl_opts['cookiefile'] = COOKIES_FILE

    try:
        ui_log(f"📥 Iniciando descarga de video...")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            if info is None:
                raise Exception("No se extrajo información del video")

        if not os.path.exists(output):
            raise Exception("Archivo no se creó")

        size = os.path.getsize(output)
        ui_log(f"✅ [YOUTUBE-VIDEO] Video listo: {os.path.basename(output)} ({size // 1024} KB)")
        background_tasks.add_task(limpiar, output)
        return FileResponse(path=output, media_type='video/mp4', filename="video.mp4")

    except Exception as e:
        error_msg = str(e)
        ui_log(f"❌ [YOUTUBE-VIDEO] Error: {error_msg}")
        if os.path.exists(output):
            try:
                os.remove(output)
            except:
                pass
        raise HTTPException(status_code=500, detail=error_msg)

@api.post("/download-youtube-song")
async def download_youtube_song(payload: dict, background_tasks: BackgroundTasks):
    url = payload.get("url")
    if not url:
        raise HTTPException(status_code=400, detail="URL missing")

    # Guardar en carpeta temporal PERO NO eliminar
    carpeta_base = "descargas_youtube"
    os.makedirs(carpeta_base, exist_ok=True)

    ui_log(f"🎵 [YOUTUBE-SONG] Descargando: {url[:60]}...")

    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': f'{carpeta_base}/%(title)s.%(ext)s',
        'quiet': False,  # ← DEPURACIÓN
        'no_warnings': False,
        'socket_timeout': 30,
        'noplaylist': True,
        'ffmpeg_location': FFMPEG_PATH,
        'postprocessors': [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }],
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
    }
    if os.path.exists(COOKIES_FILE):
        ydl_opts['cookiefile'] = COOKIES_FILE

    try:
        ui_log(f"📥 Descargando y convirtiendo a MP3...")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)

        archivo_encontrado = None
        for f in os.listdir(carpeta_base):
            if f.endswith('.mp3'):
                archivo_encontrado = os.path.join(carpeta_base, f)
                break

        if not archivo_encontrado:
            raise Exception("Archivo MP3 no se creó")

        size = os.path.getsize(archivo_encontrado)
        titulo = os.path.splitext(os.path.basename(archivo_encontrado))[0]
        ui_log(f"✅ [YOUTUBE-SONG] Guardada en: {archivo_encontrado} ({size // 1024} KB)")

        # ← NO ELIMINAR, solo enviar
        return FileResponse(path=archivo_encontrado, media_type='audio/mp3',
                          filename=f"{titulo}.mp3")

    except Exception as e:
        error_msg = str(e)
        ui_log(f"❌ [YOUTUBE-SONG] Error: {error_msg}")
        raise HTTPException(status_code=500, detail=error_msg)

@api.get("/archivo")
def obtener_archivo(ruta: str, background_tasks: BackgroundTasks):
    ui_log(f"📤 Archivo pedido: {ruta}")
    if not ruta:
        raise HTTPException(status_code=400, detail="Ruta vacía")
    if not os.path.exists(ruta):
        ui_log(f"❌ Archivo NO existe: {ruta}")
        raise HTTPException(status_code=404, detail="Archivo no encontrado")
    try:
        size = os.path.getsize(ruta)
        ui_log(f"📁 Enviando: {os.path.basename(ruta)} ({size // 1024} KB)")
        background_tasks.add_task(limpiar, ruta)
        return FileResponse(path=ruta, media_type='audio/mp4',
                            filename=os.path.basename(ruta),
                            headers={"Content-Length": str(size)})
    except Exception as e:
        ui_log(f"❌ Error enviando archivo: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

# ── UI ──────────────────────────────────────────────────────

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("blue")

TEXTO_AYUDA = """
╔══════════════════════════════════════════════════╗
║         GUÍA DE CONFIGURACIÓN — NihilNox         ║
╚══════════════════════════════════════════════════╝

Para que el servidor funcione necesitas 4 cosas:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1️⃣  PYTHON 3.12
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Descárgalo desde:
  https://python.org/downloads/release/python-3120/

⚠️ Marca "Add Python to PATH" al instalar.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2️⃣  NGROK — Para el túnel público
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Crea cuenta gratis en: https://ngrok.com
• Copia tu Auth Token desde:
    https://dashboard.ngrok.com/authtokens
• Crea un dominio estático gratis en:
    https://dashboard.ngrok.com/domains
  (Ejemplo: midominio.ngrok-free.dev)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3️⃣  FFMPEG — Para convertir música a MP3
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Instálalo automáticamente con este comando
en la terminal (CMD o PowerShell):

  py -3.12 -m pip install spotdl
  py -3.12 -m spotdl --download-ffmpeg

Esto lo guarda en:
  C:\\\\Users\\\\TuUsuario\\\\.spotdl\\\\ffmpeg.exe

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4️⃣  COOKIES (Opcional — Para Instagram/YouTube)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Si tienes errores "403 Forbidden" o "Login required":
• Instala "Get cookies.txt LOCALLY" en tu navegador.
• Ve a Instagram o YouTube e inicia sesión.
• Exporta las cookies como Netscape format.
• Guarda el archivo como ".nihilnox_cookies.txt"
  en tu carpeta de usuario (C:\\\\Users\\\\TuNombre).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5️⃣  API KEY — Contraseña entre app y servidor
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Es cualquier texto que tú elijas.
Debe ser la misma en:
  • Este servidor (campo API Key)
  • La app Android → Ajustes → API Keys
    → Key del Servidor

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
▶  PASOS PARA USAR
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Presiona ⚙️ Configurar y llena los datos
2. Presiona ▶ INICIAR
3. Copia la URL con 📋 Copiar URL
4. Pégala en la app Android → Ajustes → API Keys
   → URL del Servidor
5. ¡Listo! Ya puedes descargar música y videos
""".strip()


class AyudaDialog(ctk.CTkToplevel):
    def __init__(self, parent):
        super().__init__(parent)
        self.title("❓ Ayuda — Cómo usar NihilNox")
        self.geometry("540x580")
        self.resizable(False, True)
        self.grab_set()

        ctk.CTkLabel(self, text="❓ Cómo configurar NihilNox",
                     font=ctk.CTkFont(size=16, weight="bold")).pack(pady=(16, 8))

        txt = ctk.CTkTextbox(self, width=500, height=460,
                              font=ctk.CTkFont(family="Consolas", size=12))
        txt.pack(padx=16, pady=(0, 8))
        txt.insert("end", TEXTO_AYUDA)
        txt.configure(state="disabled")

        ctk.CTkButton(self, text="Cerrar", width=120,
                      command=self.destroy).pack(pady=(0, 16))


class ConfigDialog(ctk.CTkToplevel):
    def __init__(self, parent, config_actual: dict, on_guardar):
        super().__init__(parent)
        self.title("⚙️ Configuración")
        self.geometry("480x420")
        self.resizable(False, False)
        self.grab_set()
        self.on_guardar = on_guardar

        ctk.CTkLabel(self, text="⚙️ Configuración del Servidor",
                     font=ctk.CTkFont(size=16, weight="bold")).pack(pady=(20, 16))

        def campo(label, valor, show=""):
            ctk.CTkLabel(self, text=label, anchor="w",
                         font=ctk.CTkFont(size=12)).pack(padx=24, fill="x")
            entry = ctk.CTkEntry(self, width=430, show=show)
            entry.insert(0, valor)
            entry.pack(padx=24, pady=(2, 10))
            return entry

        self.e_token  = campo("🔑 ngrok Auth Token:", config_actual.get("ngrok_token", ""), show="*")
        self.e_domain = campo("🌐 ngrok Domain (ej: tudominio.ngrok-free.dev):", config_actual.get("ngrok_domain", ""))
        self.e_apikey = campo("🔐 API Key del servidor:", config_actual.get("api_key", ""))
        self.e_ffmpeg = campo("🎵 Ruta de ffmpeg.exe:", config_actual.get("ffmpeg_path", r"C:\Users\elang\.spotdl\ffmpeg.exe"))

        frame = ctk.CTkFrame(self, fg_color="transparent")
        frame.pack(pady=16)
        ctk.CTkButton(frame, text="💾 Guardar", width=160,
                      fg_color="#2e7d32", hover_color="#1b5e20",
                      command=self._guardar).pack(side="left", padx=8)
        ctk.CTkButton(frame, text="Cancelar", width=120,
                      fg_color="#555", hover_color="#333",
                      command=self.destroy).pack(side="left", padx=8)

    def _guardar(self):
        nueva = {
            "ngrok_token":  self.e_token.get().strip(),
            "ngrok_domain": self.e_domain.get().strip(),
            "api_key":      self.e_apikey.get().strip(),
            "ffmpeg_path":  self.e_ffmpeg.get().strip(),
        }
        guardar_config(nueva)
        self.on_guardar(nueva)
        self.destroy()


class App(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title("NihilNox Server")
        self.geometry("500x580")
        self.resizable(False, False)
        self.running = False
        self.tray_icon = None
        self._build_ui()
        self.protocol("WM_DELETE_WINDOW", self._minimizar_a_tray)
        global _ui_log
        _ui_log = self._log

        if not NGROK_TOKEN or not NGROK_DOMAIN or not API_KEY:
            self.after(300, self._abrir_config)

    def _build_ui(self):
        ctk.CTkLabel(self, text="🎨 NihilNox Server",
                     font=ctk.CTkFont(size=22, weight="bold")).pack(pady=(20, 4))

        self.lbl_status = ctk.CTkLabel(self, text="⚪ INACTIVO",
                                        font=ctk.CTkFont(size=14),
                                        text_color="gray")
        self.lbl_status.pack(pady=2)

        self.lbl_url = ctk.CTkLabel(self,
                                     text=NGROK_DOMAIN or "Sin configurar",
                                     font=ctk.CTkFont(size=12),
                                     text_color="#888888")
        self.lbl_url.pack(pady=2)

        row1 = ctk.CTkFrame(self, fg_color="transparent")
        row1.pack(pady=(4, 2))
        ctk.CTkButton(row1, text="📋 Copiar URL", width=140,
                      command=self._copiar_url).pack(side="left", padx=4)
        ctk.CTkButton(row1, text="⚙️ Configurar", width=140,
                      fg_color="#1565c0", hover_color="#0d47a1",
                      command=self._abrir_config).pack(side="left", padx=4)
        ctk.CTkButton(row1, text="❓ Ayuda", width=100,
                      fg_color="#6a1b9a", hover_color="#4a148c",
                      command=self._abrir_ayuda).pack(side="left", padx=4)

        self.log_box = ctk.CTkTextbox(self, width=440, height=300,
                                       font=ctk.CTkFont(family="Consolas", size=12))
        self.log_box.pack(padx=20, pady=8)
        self.log_box.configure(state="disabled")

        frame = ctk.CTkFrame(self, fg_color="transparent")
        frame.pack(pady=10)
        self.btn_start = ctk.CTkButton(frame, text="▶  INICIAR", width=160,
                                        fg_color="#2e7d32", hover_color="#1b5e20",
                                        command=self._iniciar)
        self.btn_start.pack(side="left", padx=8)
        self.btn_stop = ctk.CTkButton(frame, text="⏹  DETENER", width=160,
                                       fg_color="#c62828", hover_color="#7f0000",
                                       state="disabled",
                                       command=self._detener)
        self.btn_stop.pack(side="left", padx=8)

    def _abrir_ayuda(self):
        AyudaDialog(self)

    def _abrir_config(self):
        ConfigDialog(self, cargar_config(), self._aplicar_config)

    def _aplicar_config(self, nueva: dict):
        global NGROK_TOKEN, NGROK_DOMAIN, API_KEY, FFMPEG_PATH
        NGROK_TOKEN  = nueva.get("ngrok_token", "")
        NGROK_DOMAIN = nueva.get("ngrok_domain", "")
        API_KEY      = nueva.get("api_key", "")
        FFMPEG_PATH  = nueva.get("ffmpeg_path", "")
        self.lbl_url.configure(text=NGROK_DOMAIN or "Sin configurar")
        self._log("✅ Configuración guardada.")

    def _log(self, msg):
        self.log_box.configure(state="normal")
        self.log_box.insert("end", f"{msg}\n")
        self.log_box.see("end")
        self.log_box.configure(state="disabled")

    def _copiar_url(self):
        self.clipboard_clear()
        self.clipboard_append(f"https://{NGROK_DOMAIN}")
        self._log("📋 URL copiada al portapapeles")

    def _iniciar(self):
        if not NGROK_TOKEN or not NGROK_DOMAIN or not API_KEY:
            self._log("⚠️ Configura ngrok Token, Domain y API Key primero.")
            self._abrir_config()
            return
        self.btn_start.configure(state="disabled")
        self.btn_stop.configure(state="normal")
        self.lbl_status.configure(text="🟡 INICIANDO...", text_color="orange")
        threading.Thread(target=self._run_server, daemon=True).start()

    def _run_server(self):
        try:
            self._log("🔧 Configurando ngrok...")
            ngrok.set_auth_token(NGROK_TOKEN)
            tunnel = ngrok.connect(PORT, domain=NGROK_DOMAIN)
            self._log(f"🌍 Túnel activo: {tunnel.public_url}")
            self.after(0, lambda: self.lbl_status.configure(
                text="🟢 ACTIVO", text_color="#4caf50"))
            self.after(0, lambda: self.lbl_url.configure(text_color="#4caf50"))
            self._log("🚀 Iniciando servidor FastAPI...")
            self._log("🎵 /descargar  /buscar-cancion  /download-youtube-song  /archivo")
            self._log("🎬 /download-instagram  /download-youtube-video")

            config = uvicorn.Config(api, host="0.0.0.0", port=PORT,
                                    loop="asyncio", log_config=None)
            server = uvicorn.Server(config)
            self.running = True
            self._log("✅ Servidor listo.")
            server.run()
        except Exception as e:
            self._log(f"❌ Error: {str(e)}")
            self.after(0, lambda: self.lbl_status.configure(text="🔴 ERROR", text_color="red"))
            self.after(0, lambda: self.btn_start.configure(state="normal"))
            self.after(0, lambda: self.btn_stop.configure(state="disabled"))

    def _detener(self):
        self._log("⏹ Deteniendo servidor...")
        ngrok.kill()
        self.running = False
        self.lbl_status.configure(text="⚪ INACTIVO", text_color="gray")
        self.lbl_url.configure(text_color="#888888")
        self.btn_start.configure(state="normal")
        self.btn_stop.configure(state="disabled")
        self._log("🔴 Servidor detenido.")

    def _crear_icono_tray(self):
        img  = Image.new("RGB", (64, 64), color="#1a1a2e")
        draw = ImageDraw.Draw(img)
        draw.ellipse([8, 8, 56, 56], fill="#4caf50")
        draw.text((20, 18), "NN", fill="white")
        return img

    def _minimizar_a_tray(self):
        self.withdraw()
        if self.tray_icon is None:
            image = self._crear_icono_tray()
            menu  = pystray.Menu(
                item("Abrir", self._mostrar_ventana, default=True),
                item("Salir", self._salir_completo)
            )
            self.tray_icon = pystray.Icon("NihilNox", image, "NihilNox Server", menu)
            threading.Thread(target=self.tray_icon.run, daemon=True).start()
        self._log("📌 Minimizado a la bandeja")

    def _mostrar_ventana(self):
        self.after(0, self.deiconify)
        self.after(0, self.lift)

    def _salir_completo(self):
        if self.running:
            ngrok.kill()
        if self.tray_icon:
            self.tray_icon.stop()
        self.after(0, self.destroy)

if __name__ == "__main__":
    app = App()
    app.mainloop()