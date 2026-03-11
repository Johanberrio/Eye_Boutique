# Eye_Boutique 👓

**Versión 1.0**

Una aplicación Android nativa desarrollada en Kotlin que proporciona una solución integral para la gestión de una boutique de lentes oftálmicos.

## 📋 Descripción del Proyecto

Eye_Boutique es la **primera versión** de una aplicación móvil diseñada para facilitar la administración y consulta de productos ópticos, integrando funcionalidades modernas de autenticación, almacenamiento de datos y experiencia de usuario mejorada.

## ✨ Funcionalidades Implementadas

### 🔐 Autenticación y Seguridad
- **Autenticación Firebase**: Sistema de login seguro mediante Firebase Authentication
- **Autenticación Biométrica**: Soporte para desbloqueo mediante huella dactilar o reconocimiento facial (API 24+)
- **Almacenamiento Seguro**: DataStore para el almacenamiento persistente y seguro de preferencias del usuario

### 💾 Gestión de Datos
- **Firebase Firestore**: Base de datos en tiempo real para sincronización de información
- **Firebase Functions**: Funciones en la nube para lógica de negocio serverless
- **Persistencia Local**: DataStore para preferencias y datos locales

### 🎨 Interfaz de Usuario
- **Jetpack Compose**: Framework moderno para construir interfaces de usuario declarativas
- **Material Design 3**: Implementación de las últimas directrices de diseño de Material
- **Navegación Nativa**: Sistema de navegación entre pantallas optimizado
- **Iconografía Extendida**: Material Icons Extended para iconos personalizados

### 📱 Características Técnicas
- **Kotlin**: Desarrollo 100% en Kotlin con corrutinas
- **MVVM Architecture**: Arquitectura limpia basada en ViewModel y LiveData
- **Jetpack Navigation**: Navegación segura entre fragmentos y actividades
- **Java 11**: Compilación con Java 11 para mejor rendimiento
- **Desugaring**: Soporte para java.time en dispositivos antiguos

## 🛠️ Stack Tecnológico

### Core Android
- `androidx.core:core-ktx` - Extensiones de Kotlin para Android
- `androidx.lifecycle:*` - ViewModel, LiveData y manejo del ciclo de vida
- `androidx.fragment:fragment-ktx` - Gestión de fragmentos

### Compose & UI
- `androidx.compose:*` - Framework Compose para UI declarativa
- `androidx.compose.material3` - Material Design 3 Components
- `androidx.compose.material:material-icons-extended` - Iconografía extendida
- `androidx.navigation:navigation-compose` - Navegación en Compose

### Firebase
- `com.google.firebase:firebase-auth-ktx` - Autenticación
- `com.google.firebase:firebase-firestore-ktx` - Base de datos Firestore
- `com.google.firebase:firebase-functions-ktx` - Cloud Functions
- Firebase BOM 33.1.2

### Seguridad & Almacenamiento
- `androidx.biometric:biometric` - API de autenticación biométrica
- `androidx.datastore:datastore-preferences` - Almacenamiento de preferencias

### Testing
- `junit` - Framework de pruebas unitarias
- `androidx.test.espresso` - Framework de pruebas UI
- `androidx.compose.ui:ui-test-junit4` - Pruebas de Compose

## 📦 Configuración del Proyecto

### Especificaciones
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Lenguaje**: Kotlin
- **Build System**: Gradle 8.x

### Namespace


### Versión Actual
- **Version Code**: 1
- **Version Name**: 1.0

## 🚀 Requisitos Previos

- Android Studio Arctic Fox o superior
- JDK 11 o superior
- Conexión a Internet (para Firebase)
- Dispositivo Android con API 24+

## 📲 Instalación

1. **Clonar el repositorio**
```bash
git clone https://github.com/Johanberrio/Eye_Boutique.git
cd Eye_Boutique

🏗️ Estructura del Proyecto
Configurar Firebase

Descargar google-services.json de Firebase Console
Colocar el archivo en la carpeta app/
Compilar y ejecutar

./gradlew build
./gradlew installDebug

Eye_Boutique/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/lentespro/    # Código fuente Kotlin
│   │   │   ├── res/                            # Recursos (layouts, strings, etc.)
│   │   │   └── AndroidManifest.xml            # Configuración de la aplicación
│   │   ├── androidTest/                        # Pruebas instrumentadas
│   │   └── test/                               # Pruebas unitarias
│   ├── build.gradle.kts                        # Configuración del módulo app
│   └── google-services.json                    # Configuración de Firebase
├── gradle/                                      # Scripts de Gradle
├── build.gradle.kts                            # Build script raíz
├── settings.gradle.kts                         # Configuración de proyectos
└── gradle.properties                           # Propiedades globales de Gradle
