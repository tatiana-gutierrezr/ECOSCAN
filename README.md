# ♻️ EcoScan - App de Reciclaje Inteligente / Smart Recycling App

**EcoScan** es una aplicación móvil educativa que promueve el reciclaje a través de inteligencia artificial, geolocalización y contenido interactivo.  
Está desarrollada en **Android Studio con Kotlin**, y utiliza **Firebase** para autenticación y base de datos.

**EcoScan** is an educational mobile app that promotes recycling through artificial intelligence, geolocation, and interactive content.  
It is built with **Android Studio (Kotlin)** and uses **Firebase** for authentication and database services.

---

## 📲 Funcionalidades / Features

### 🧠 Información educativa por edades  
**[ES]** Sección con contenido educativo segmentado por edades que explica qué es el reciclaje y cómo hacerlo correctamente.  
**[EN]** Age-appropriate educational section that explains what recycling is and how to do it correctly.

---

### 📍 Puntos de reciclaje en el mapa / Recycling Points Map  
**[ES]** Muestra puntos de recolección en **Bogotá** y **Medellín** usando la **API de Google Maps**.  
**[EN]** Displays recycling drop-off locations in **Bogotá** and **Medellín** using the **Google Maps API**.

---

### 📷 Escaneo con IA / AI-based Object Scanner  
**[ES]** Escáner inteligente con **modelo de clasificación de imágenes** que detecta materiales reciclables como plástico, metal o papel.  
**[EN]** Smart scanner with an **image classification model** that identifies recyclable materials like plastic, metal, or paper.

---

### 👤 Perfil de usuario / User Profile (requiere login / login required)  
**[ES]** Sección de perfil disponible solo para usuarios autenticados:  
- Foto, nombre y nombre de usuario  
- **Historial** de escaneos (foto, fecha y resultado)  
- **Tipo de objeto**: resumen de objetos escaneados y tipo de material (ej. "Has escaneado 3 objetos: 2 de plástico, 1 de metal")  

**[EN]** Profile section (only for logged-in users):  
- Photo, name, and username  
- **Scan history** (photo, date, and classification result)  
- **Object type summary**: total objects scanned and breakdown by material (e.g., "You scanned 3 items: 2 plastic, 1 metal")

---

## 🔓 Acceso sin cuenta / Available without login

✅ Información educativa / Educational content  
✅ Mapa de reciclaje / Recycling map  
✅ Escáner inteligente / Smart scanner

---

## 🔐 Acceso con autenticación / Login required for:

🔒 Perfil de usuario / User profile  
🔒 Historial de escaneos / Scan history  
🔒 Estadísticas por tipo de objeto / Material breakdown

---

## 🛠️ Tecnologías utilizadas / Technologies used

| Herramienta / Tool      | Uso / Purpose                         |
|-------------------------|----------------------------------------|
| Android Studio + Kotlin | Desarrollo de la app / App development |
| Firebase Auth           | Autenticación / Authentication         |
| Firebase Firestore      | Base de datos en la nube / Cloud database |
| Google Maps API         | Mapa de puntos de reciclaje / Recycling map |
| TensorFlow Lite         | Modelo de IA para clasificación / AI image classification |

---

## 🚀 Cómo ejecutar el proyecto / How to run the project

1. Clona el repositorio / Clone the repo  
   ```bash
   git clone [https://github.com/tatiana-gutierrezr/ECOSCAN.git]
   ```

2. Abre el proyecto en Android Studio / Open the project in Android Studio

3. ⚠️ **Nota**: Deberás configurar tus propias claves de API para que ciertas funcionalidades funcionen correctamente (Google Maps, Firebase, etc.). Estas **no están incluidas por motivos de seguridad**.  
   ⚠️ **Note**: You must configure your own API keys (Google Maps, Firebase, etc.) for full functionality. **These are not included for security reasons**.

4. Ejecuta la app en un emulador o dispositivo físico / Run the app on an emulator or device
