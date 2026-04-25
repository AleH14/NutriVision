# NutriVision

<div align="center">

[![Android](https://img.shields.io/badge/Android-36-green?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Node.js](https://img.shields.io/badge/Node.js-LTS-green?logo=node.js&logoColor=white)](https://nodejs.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-NoSQL-4DB33D?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![OpenAI API](https://img.shields.io/badge/OpenAI-GPT--4-412991?logo=openai&logoColor=white)](https://openai.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Análisis Inteligente de Nutrientes mediante Visión por Computadora**

Una aplicación móvil revolucionaria que utiliza inteligencia artificial para capturar y analizar el contenido nutricional de tus comidas en tiempo real.

[Documentación](#documentación) • [Características](#características) • [Instalación](#instalación) • [Estructura](#estructura-del-proyecto) • [API](#api-endpoints) • [Contribuir](#contribuir)

</div>

---

## 🎯 Descripción

**NutriVision** es una solución integral de nutrición que combina tecnología de vanguardia para proporcionar análisis nutricionales precisos y personalizados. La aplicación permite a los usuarios fotografiar sus comidas y recibir información detallada sobre calorías, macronutrientes y metas nutricionales diarias.

### Flujo de la Aplicación

1. **Captura**: Usuario toma foto de su comida desde la aplicación móvil Android
2. **Envío**: La imagen se transmite al backend seguro
3. **Análisis IA**: OpenAI (GPT-4) analiza la imagen y extrae información nutricional
4. **Almacenamiento**: Los datos se guardan en MongoDB para historial y seguimiento
5. **Retroalimentación**: El usuario recibe desglose nutricional detallado en tiempo real

---

## ✨ Características Principales

### 📱 Aplicación Móvil (Kotlin)
- **Captura de Fotos**: Interfaz intuitiva con cámara integrada (CameraX 1.4.1)
- **Galería**: Seleccionar imágenes del almacenamiento del dispositivo
- **Análisis Nutricional**: Visualización clara de macronutrientes y calorías
- **Seguimiento Diario**: Gráficos y estadísticas de consumo
- **Perfil de Usuario**: Configuración personalizada de metas nutricionales
- **Sincronización**: Conectividad en tiempo real con backend

### 🔧 Backend (Node.js + Express)
- **API RESTful**: Endpoints bien documentados y escalables
- **Integración IA**: Conexión directa con OpenAI API para análisis profundo
- **Gestión de Datos**: Base de datos NoSQL con MongoDB
- **Autenticación**: Sistema seguro de usuarios
- **Validación**: Procesamiento robusto de imágenes

### 🗄️ Base de Datos (MongoDB)
- **Usuarios**: Perfiles y configuraciones personalizadas
- **Análisis**: Historial completo de análisis realizados
- **Platos Consumidos**: Registro detallado de comidas
- **Resumen Nutricional**: Totalizaciones diarias por usuario

---

## 🚀 Instalación y Configuración

### Requisitos Previos

```bash
# Para Android
- Android SDK 36
- Java 11+
- Gradle 8.x
- Android Studio (recomendado)

# Para Backend
- Node.js 18+ (LTS)
- npm o yarn
- MongoDB 5.0+
```

### Setup Rápido

#### 1️⃣ Clonar el Repositorio

```bash
git clone https://github.com/AleH14/NutriVision.git
cd NutriVision
```

#### 2️⃣ Configurar el Backend

```bash
cd backend
cp .env.example .env
npm install
```

**Configuración del archivo `.env`:**

```env
# Server
PORT=4000
NODE_ENV=development

# Database
MONGODB_URI=mongodb://localhost:27017/nutrivision

# OpenAI
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o-mini

# Client
CLIENT_URL=http://localhost:3000
```

**Iniciar el servidor:**

```bash
npm run dev    # Modo desarrollo
npm run build  # Build para producción
npm start      # Producción
```

#### 3️⃣ Compilar la Aplicación Android

```bash
# Desde el directorio raíz del proyecto
./gradlew build

# O con Android Studio
# File → Open → Seleccionar el directorio NutriVision
```

**Configuración de desarrollo en `app/build.gradle.kts`:**

```gradle
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://192.168.1.14:4000/\"")
    }
    release {
        buildConfigField("String", "BASE_URL", "\"https://your-production-server.com/\"")
    }
}
```

---

## 📁 Estructura del Proyecto

```
NutriVision/
├── app/                           # 📱 Aplicación Android (Kotlin)
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/           # Código Kotlin
│   │   │   ├── res/              # Recursos (layouts, drawables, strings)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                 # Tests unitarios
│   ├── build.gradle.kts          # Dependencias Android
│   └── proguard-rules.pro        # Ofuscación de código
│
├── backend/                       # 🔧 Backend (Node.js + Express)
│   ├── src/
│   │   ├── routes/               # Rutas API
│   │   ├── controllers/          # Lógica de negocio
│   │   ├── models/               # Esquemas MongoDB
│   │   ├── middleware/           # Middleware personalizado
│   │   ├── services/             # Servicios (OpenAI, etc.)
│   │   └── config/               # Configuración
│   ├── .env.example              # Variables de entorno
│   ├── package.json              # Dependencias Node.js
│   └── README.md                 # Documentación del backend
│
├── gradle/                        # 🏗️ Configuración Gradle
├── .gitignore                    # Archivos ignorados por Git
├── build.gradle.kts              # Build script raíz
├── settings.gradle.kts           # Configuración de módulos
└── README.md                      # Este archivo
```

---

## 🔌 API Endpoints

### Health Check
```http
GET /health
```

**Respuesta exitosa (200):**
```json
{
  "status": "ok",
  "timestamp": "2026-04-25T10:30:00Z"
}
```

### Calcular Meta Calórica Diaria
```http
POST /api/analysis/daily-goal/:userId
```

Calcula la meta diaria de calorías según el perfil del usuario (edad, peso, altura, nivel de actividad) utilizando IA.

**Parámetros:**
- `userId` (path, requerido): ID del usuario

**Respuesta (200):**
```json
{
  "userId": "507f1f77bcf86cd799439011",
  "dailyCalorieGoalKcal": 2500,
  "updatedAt": "2026-04-25T10:30:00Z"
}
```

### Analizar Imagen de Comida
```http
POST /api/analysis/image
Content-Type: multipart/form-data
```

Analiza una imagen de comida y extrae información nutricional detallada.

**Parámetros:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `image` | archivo | ✅ | Imagen JPEG/PNG de la comida |
| `userId` | texto | ✅ | ID del usuario |
| `photoTakenTime` | texto | ❌ | Hora captura (formato: HH:mm) |
| `dishDate` | texto | ❌ | Fecha del plato (formato: YYYY-MM-DD) |

**Respuesta exitosa (200):**
```json
{
  "id": "507f1f77bcf86cd799439012",
  "dishes": [
    {
      "name": "Pollo al horno",
      "portion": "200g"
    },
    {
      "name": "Brócoli al vapor",
      "portion": "150g"
    }
  ],
  "mealType": "almuerzo",
  "plateAnalysis": {
    "description": "Plato balanceado con proteína de alta calidad...",
    "healthScore": 8.5
  },
  "nutrition": {
    "calories": 450,
    "protein": 45,
    "carbs": 30,
    "fats": 15,
    "fiber": 8
  },
  "analyzedAt": "2026-04-25T12:45:00Z"
}
```

### Listar Análisis de Usuario
```http
GET /api/analysis?userId={userId}&limit=10&page=1
```

Obtiene el historial de análisis nutricionales del usuario.

**Parámetros Query:**
| Parámetro | Tipo | Defecto | Descripción |
|-----------|------|---------|-------------|
| `userId` | texto | — | ID del usuario (requerido) |
| `limit` | número | 20 | Máximo de resultados |
| `page` | número | 1 | Número de página |
| `startDate` | texto | — | Filtro por fecha inicio (YYYY-MM-DD) |
| `endDate` | texto | — | Filtro por fecha fin (YYYY-MM-DD) |

**Respuesta (200):**
```json
{
  "data": [
    {
      "id": "507f1f77bcf86cd799439012",
      "userId": "507f1f77bcf86cd799439011",
      "dishDate": "2026-04-25",
      "nutrition": {
        "calories": 450,
        "protein": 45,
        "carbs": 30,
        "fats": 15
      },
      "analyzedAt": "2026-04-25T12:45:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 5,
    "totalItems": 87
  }
}
```

### Códigos de Error

| Código | Error | Descripción |
|--------|-------|-------------|
| 400 | Bad Request | Parámetros inválidos o faltantes |
| 401 | Unauthorized | Token de autenticación inválido/expirado |
| 403 | Forbidden | Acceso denegado al recurso |
| 404 | Not Found | Recurso no encontrado |
| 413 | Payload Too Large | Imagen muy grande (máx. 5MB) |
| 500 | Internal Server Error | Error en el servidor |
| 503 | Service Unavailable | OpenAI API no disponible |

---

## 🏗️ Arquitectura Técnica

### Stack Tecnológico

```
┌─────────────────────────────────────────────────────┐
│                 NUTRIVISION APP                      │
│                   (Android/Kotlin)                   │
│  - CameraX para captura                             │
│  - Retrofit para networking                         │
│  - MVVM Architecture                                │
└─────────────────┬───────────────────────────────────┘
                  │ HTTP/REST
                  ▼
┌─────────────────────────────────────────────────────┐
│                  BACKEND (Express)                   │
│  - Node.js 18+ LTS                                  │
│  - Middleware authentication                        │
│  - Rate limiting & validación                       │
└─────────────────┬───────────────────────────────────┘
                  │ API Call
                  ├─────────────────────────┐
                  ▼                         ▼
        ┌──────────────────┐      ┌─────────────────┐
        │   OpenAI API     │      │    MongoDB      │
        │  (GPT-4o-mini)   │      │   NoSQL DB      │
        │                  │      │                 │
        │ Image Analysis   │      │ - Users         │
        │ Nutrition Facts  │      │ - Analyses      │
        │ Health Scoring   │      │ - Dishes        │
        └──────────────────┘      └─────────────────┘
```

### Flujo de Datos

```
1. Usuario toma foto
            ↓
2. App comprime y envía
            ↓
3. Backend recibe + valida
            ↓
4. OpenAI analiza contenido
            ↓
5. Backend procesa respuesta IA
            ↓
6. Guarda en MongoDB
            ↓
7. Retorna al cliente + actualiza UI
```

---

## 🔐 Seguridad

- **Variables sensibles**: Usar `.env` para API keys (nunca en código)
- **Autenticación**: JWT tokens con expiración configurable
- **HTTPS**: Obligatorio en producción
- **CORS**: Configurado para dominios permitidos
- **Rate Limiting**: Protección contra abuso API
- **Validación**: Todos los inputs validados en backend

---

## 📊 Dependencias Principales

### Android (Kotlin)
```kotlin
// Networking
- Retrofit 2.x
- OkHttp logging

// UI & Lifecycle
- AndroidX (Core, AppCompat, ConstraintLayout)
- Material Design 3
- CameraX 1.4.1

// Testing
- JUnit 4
- Espresso
```

### Backend (Node.js)
```javascript
// Core
- express.js
- dotenv

// Database
- mongoose (MongoDB ODM)

// AI Integration
- openai (SDK oficial)

// Utilities
- multer (file upload)
- cors
- helmet (security)
- express-validator
```

---

## 🧪 Testing

### Android
```bash
# Tests unitarios
./gradlew testDebugUnitTest

# Tests de instrumentación
./gradlew connectedAndroidTest
```

### Backend
```bash
npm test              # Ejecutar todos los tests
npm run test:watch   # Modo watch
npm run test:coverage # Con cobertura
```

---

## 📚 Documentación

- [Backend README](./backend/README.md) - Documentación completa del servidor
- [API OpenAI](https://platform.openai.com/docs) - Referencia oficial OpenAI
- [MongoDB Docs](https://docs.mongodb.com/) - Documentación MongoDB
- [Android Developers](https://developer.android.com/) - Docs oficiales Android

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Por favor sigue estos pasos:

### 1. Fork el Repositorio
```bash
git clone https://github.com/AleH14/NutriVision.git
cd NutriVision
```

### 2. Crear una Rama
```bash
git checkout -b feature/AmazingFeature
```

### 3. Commits Descriptivos
```bash
git commit -m "Add some AmazingFeature"
```

### 4. Push a la Rama
```bash
git push origin feature/AmazingFeature
```

### 5. Abrir un Pull Request
Describe los cambios, proporciona capturas de pantalla si es necesario.

### Convenciones de Código

- **Kotlin**: Seguir [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- **JavaScript**: Usar Prettier y ESLint configurados
- **Commits**: Usar formato convencional: `feat:`, `fix:`, `docs:`, etc.

---

## 📝 Licencia

Este proyecto está bajo la licencia MIT. Ver archivo [LICENSE](LICENSE) para más detalles.

---

## 👨‍💻 Autor

**AleH14** - [@AleH14](https://github.com/AleH14)

---

## 📞 Contacto & Soporte

- 🐛 [Reportar Bugs](https://github.com/AleH14/NutriVision/issues)
- 💡 [Solicitar Características](https://github.com/AleH14/NutriVision/issues)
- 📧 Contacto: Ver perfil de GitHub

---

## 🗺️ Roadmap

- [ ] Integración con Fitbit/Apple Health
- [ ] Análisis de macronutrientes más precisos
- [ ] Reconocimiento offline mejorado
- [ ] App iOS
- [ ] Dashboard web para análisis detallados
- [ ] Exportación de reportes en PDF
- [ ] Integración con nutricionistas

---

## 🌟 Agradecimientos

- [OpenAI](https://openai.com/) por GPT-4 y Computer Vision
- [MongoDB](https://www.mongodb.com/) por la base de datos flexible
- [Google](https://developer.android.com/) por Android y CameraX
- La comunidad open source

---

<div align="center">

**⭐ Si este proyecto te fue útil, considera dejar una estrella!**

Hecho con ❤️ por AleH14

</div>
