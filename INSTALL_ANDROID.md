# Super Mario Sunshine - Android Port (`sms-android`)

Port nativo de **Super Mario Sunshine** para dispositivos Android (ARM64 / `arm64-v8a`) acelerado por hardware mediante **OpenGL ES 3.0**, con controles táctiles personalizables integrados en pantalla y soporte plug-and-play para gamepads físicos (USB y Bluetooth).

---

## 📋 Requisitos Previos

1. **Dispositivo Android**:
   - Arquitectura: ARM64 (`arm64-v8a`).
   - Versión de Android: Android 8.0 (Oreo) o superior.
   - Soporte gráfico: OpenGL ES 3.0 o superior.
2. **Copia original del juego**:
   - Archivo ISO de *Super Mario Sunshine* (versión USA GameCube, código `GMSE01`).
   - Nombre recomendado del archivo: `GMSE01.iso`.

---

## 🚀 Guía de Instalación Rápida

### Paso 1: Instalar el APK

1. Descarga el archivo `app-release.apk` desde la sección de **Releases** del repositorio o desde la compilación local (`android/app/build/outputs/apk/release/app-release.apk`).
2. En tu dispositivo Android, abre el instalador de paquetes y confirma la instalación.
   - *Nota:* Si Android muestra una advertencia de orígenes desconocidos, habilita el permiso para instalar la aplicación.

Si utilizas una PC con ADB:
```bash
adb install -r app-release.apk
```

---

### Paso 2: Colocar la ISO del Juego (`GMSE01.iso`)

El juego busca automáticamente la imagen del disco en varias rutas comunes de almacenamiento. La forma más sencilla es colocar tu archivo ISO en la carpeta **Descargas (Download)** de tu teléfono:

- Ruta recomendada:
  ```text
  /sdcard/Download/GMSE01.iso
  ```
  *(o renombrado como `Super Mario Sunshine.iso` / `Super Mario Sunshine (USA).iso`)*

Otras rutas admitidas automáticamente:
- `/sdcard/sms/GMSE01.iso`
- `Android/data/com.jbcgames.sunshine/files/GMSE01.iso`

Para transferir la ISO desde la computadora vía ADB:
```bash
adb push "GMSE01.iso" /sdcard/Download/GMSE01.iso
```

---

### Paso 3: Permisos de Almacenamiento

Al abrir el juego por primera vez en Android 11 o superior:
1. Concede el permiso de acceso a archivos/almacenamiento si el sistema lo solicita, para que la aplicación pueda leer la ISO desde `/sdcard/Download/`.
2. Si la app no encuentra la ISO de forma automática, puedes conceder el permiso en:
   - **Ajustes > Aplicaciones > Super Mario Sunshine > Permisos > Almacenamiento / Archivos**.

---

## 🎮 Controles

### Controles Táctiles en Pantalla
El port cuenta con un overlay táctil optimizado y estilizado diseñado para jugar cómodamente en pantallas táctiles:

| Control | Función |
| :--- | :--- |
| **Stick Virtual (Izquierda)** | Movimiento de Mario / Navegación en menús. |
| **Botón A (Verde)** | Saltar / Confirmar / Hablar. |
| **Botón B (Rojo)** | Agacharse / Bucear / Cancelar. |
| **Botón X (Amarillo)** | Cambiar boquilla del FLUDD (Hover, Rocket, Turbo). |
| **Botón Y (Azul cielo)** | Vista en primera persona / Apuntar. |
| **Botón R (Azul)** | Disparo de agua continuo en movimiento (Soft spray). |
| **Botón R+ (Azul marino)** | Disparo de agua fijo a máxima presión (Hard spray). |
| **Botón L (Plata)** | Centrar cámara detrás de Mario. |
| **Botón Z (Púrpura)** | Abrir mapa de Isla Delfino / Guía. |
| **Botón START (Naranja)** | Menú de pausa. |
| **Botón HUD (Gris superior)** | Cambiar opacidad de los controles táctiles en pantalla o ciclar visibilidad. |
| **Deslizar en mitad derecha** | Control de cámara libre analógica (C-Stick). |

### Mandos Físicos (Gamepads Bluetooth / USB)
Si conectas un mando de Xbox, PlayStation, Switch Pro Controller o GameCube con adaptador USB, el juego mapea los botones de forma 1:1 automáticamente mediante SDL2.

---

## 🛠️ Compilación desde el Código Fuente

Si deseas compilar el APK tú mismo:

1. **Requisitos de desarrollo**:
   - JDK 17
   - Android SDK (API 34)
   - Android NDK `26.3.11579264`
   - CMake 3.22.1+

2. **Compilar el proyecto**:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```
   El APK generado se encontrará en:
   `android/app/build/outputs/apk/release/app-release.apk`
