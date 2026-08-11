# Plan de Integración de Servidor PC NihilNox (Versión Simplificada)

Este plan detalla la incorporación del script principal del servidor de PC (`NihilNox.py`) al repositorio de Git del proyecto ColorBlend.

## Panorama Actual
- **App Android:** Requiere un servidor FastAPI para procesar descargas de música e Instagram.
- **Herramienta PC:** Existe un script `NihilNox.py` en el escritorio del usuario que gestiona este servidor con interfaz gráfica y túnel ngrok.

## Cambios Propuestos

### Componente PC Server

#### [NEW] [pc-server/](file:///C:/Users/elang/Documents/NihilNox/pc-server/)
Nueva carpeta para organizar las herramientas de escritorio.

#### [NEW] [NihilNox.py](file:///C:/Users/elang/Documents/NihilNox/pc-server/NihilNox.py)
Copia del script principal desde el escritorio. Incluye la lógica de FastAPI, integración con `yt-dlp`, `spotdl` y la interfaz `customtkinter`.

### Documentación

#### [MODIFY] [README.md](file:///C:/Users/elang/Documents/NihilNox/README.md)
Añadir una breve mención sobre la disponibilidad del servidor de PC en la carpeta `pc-server/` y los requisitos básicos (Python 3.12).

## Preguntas Abiertas
- ¿Deseas que incluya también un archivo `requirements.txt` básico para facilitar la instalación de las librerías necesarias (FastAPI, uvicorn, ngrok, etc.)?

## Plan de Verificación

### Pruebas Manuales
- Confirmar la creación de la carpeta `pc-server/`.
- Validar que el contenido de `NihilNox.py` se haya transferido íntegramente.
- Verificar que el `README.md` guíe correctamente hacia esta nueva herramienta.
