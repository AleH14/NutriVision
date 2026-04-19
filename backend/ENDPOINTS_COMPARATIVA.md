# 📊 Comparativa de Endpoints - Sin Usuario vs Con Usuario

## Resumen Rápido

| Característica | Sin Usuario | Con Usuario |
|---|---|---|
| **Endpoint** | `/api/analysis/image-quick` | `/api/analysis/image` |
| **Script** | `test-quick.js` | `test-image-analysis.js` |
| **Requiere UserID** | ❌ NO | ✅ SÍ |
| **Requiere Usuario en BD** | ❌ NO | ✅ SÍ |
| **Guarda en BD** | ❌ NO | ✅ SÍ |
| **Información Nutricional** | ✅ SÍ | ✅ SÍ |
| **Rapidez** | ⚡ Muy rápido | Normal |
| **Caso de uso** | Pruebas rápidas | Producción/Guardar datos |

---

## Endpoint: `/api/analysis/image-quick` (SIN USUARIO)

### ✅ Ventajas:

- No requiere cuenta de usuario
- No requiere configurar BD con usuarios
- Perfecto para **testing**
- Perfecto para **demo**
- Muy rápido
- No contamina la BD

### Solicitud:

```bash
POST http://localhost:3000/api/analysis/image-quick
Content-Type: multipart/form-data

{
  "image": <archivo de imagen>,
  "photoTakenTime": "14:30" (opcional)
}
```

### Respuesta:

```json
{
  "success": true,
  "message": "Análisis completado sin requerir cuenta de usuario",
  "mealType": "almuerzo",
  "dishes": [
    {
      "name": "Arroz con pollo",
      "estimatedPortion": "1 plato mediano"
    }
  ],
  "nutrition": {
    "calories": 450,
    "proteinGrams": 32,
    "carbsGrams": 45,
    "fatGrams": 8
  },
  "plateAnalysis": "Descripción detallada del plato..."
}
```

### Cuándo usar:

- ✅ Testing de la API
- ✅ Demostración de features
- ✅ Desarrollo local
- ✅ Prototipado
- ✅ CI/CD pipelines

### Con cURL:

```bash
curl -X POST http://localhost:3000/api/analysis/image-quick \
  -F "image=@/ruta/a/comida.jpg" \
  -F "photoTakenTime=14:30"
```

### Con Node.js:

```javascript
const fs = require("fs");
const http = require("http");

const imageBuffer = fs.readFileSync("./comida.jpg");
const boundary = "----Boundary" + Math.random().toString(36).substring(7);

const formData = createMultipartData(boundary, imageBuffer, "14:30");

const options = {
  hostname: "localhost",
  port: 3000,
  path: "/api/analysis/image-quick",
  method: "POST",
  headers: {
    "Content-Type": `multipart/form-data; boundary=${boundary}`,
    "Content-Length": formData.length,
  },
};

const req = http.request(options, (res) => {
  let data = "";
  res.on("data", (chunk) => (data += chunk));
  res.on("end", () => {
    console.log(JSON.parse(data));
  });
});

req.write(formData);
req.end();
```

---

## Endpoint: `/api/analysis/image` (CON USUARIO)

### ✅ Ventajas:

- Guarda análisis en BD
- Guarda comida consumida (ConsumedDish)
- Actualiza resumen nutricional del usuario
- Historial de análisis
- Datos para estadísticas
- Perfecto para producción

### Solicitud:

```bash
POST http://localhost:3000/api/analysis/image
Content-Type: multipart/form-data

{
  "image": <archivo de imagen>,
  "userId": "507f1f77bcf86cd799439011",
  "photoTakenTime": "14:30" (opcional),
  "dishDate": "2026-04-17" (opcional)
}
```

### Respuesta:

```json
{
  "analysis": {
    "_id": "507f1f77bcf86cd799439012",
    "userId": "507f1f77bcf86cd799439011",
    "imageName": "IMG_001.jpg",
    "foodsDetected": [...],
    "nutrition": {...},
    "notes": "Análisis del plato...",
    "rawModelResponse": {...}
  },
  "consumedDish": {
    "_id": "507f1f77bcf86cd799439013",
    "userId": "507f1f77bcf86cd799439011",
    "dishDate": "2026-04-17",
    "dishTime": "14:30",
    "calories": 450,
    "mealType": "almuerzo",
    ...
  },
  "mealType": "almuerzo",
  "dishes": [...],
  "nutrition": {...},
  "todayNutritionSummary": {
    "date": "2026-04-17",
    "proteinGramsConsumed": 85,
    "carbsGramsConsumed": 200,
    "fatGramsConsumed": 60
  }
}
```

### Cuándo usar:

- ✅ Aplicación en producción
- ✅ Guardar historial del usuario
- ✅ Tracking de nutrientes
- ✅ Estadísticas de consumo
- ✅ Datos personalizados

### Con cURL:

```bash
curl -X POST http://localhost:3000/api/analysis/image \
  -F "image=@/ruta/a/comida.jpg" \
  -F "userId=507f1f77bcf86cd799439011" \
  -F "photoTakenTime=14:30" \
  -F "dishDate=2026-04-17"
```

---

## Comparativa de Flujos

### Flujo: Testing Rápido

```
┌─────────────────┐
│ Imagen  │
└────┬────────────┘
     │
     ▼
┌──────────────────────────┐
│ POST /image-quick        │
│ (SIN usuario)            │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Análisis de OpenAI       │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Respuesta JSON           │
│ (Platos, Calorías, etc) │
└──────────────────────────┘

✅ Rápido | ✅ Simple | ✅ Sin BD
```

### Flujo: Producción con Historial

```
┌─────────────────┐
│ Imagen  │
└────┬────────────┘
     │
     ▼
┌──────────────────────────┐
│ POST /image              │
│ + userId                 │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Validar Usuario          │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Análisis de OpenAI       │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Guardar en:              │
│ • Analysis (documento)   │
│ • ConsumedDish (comida)  │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Actualizar Usuario:      │
│ • todayNutritionSummary │
└────┬─────────────────────┘
     │
     ▼
┌──────────────────────────┐
│ Respuesta JSON           │
│ (Con IDs guardados)      │
└──────────────────────────┘

✅ Persistencia | ✅ Historial | ✅ Producción
```

---

## Ejemplos de Uso

### Scenario 1: Testing de API (Sin Usuario)

```bash
# Sin usuario - perfecto para testing
npm run test:quick -- ./comida.jpg 14:30
```

### Scenario 2: Demo a Cliente (Sin Usuario)

```bash
# Mostrar feature sin necesidad de BD configurada
node test-quick.js ./platillo.png
```

### Scenario 3: App en Producción (Con Usuario)

```bash
# Usuario registrado
npm run test:image -- ./comida.jpg 507f1f77bcf86cd799439011

# Todo se guarda en BD automáticamente
```

### Scenario 4: Integración en CI/CD (Sin Usuario)

```yaml
# GitHub Actions ejemplo
- name: Test API
  run: |
    npm run dev &
    sleep 5
    npm run test:quick -- ./test-food.jpg
```

---

## Migración a Producción

### Paso 1: Desarrollo (Sin Usuario)

```javascript
// Testing local
const response = await fetch("/api/analysis/image-quick", {
  method: "POST",
  body: formData
});
```

### Paso 2: Con Usuarios (Producción)

```javascript
// Con autenticación de usuario
const response = await fetch("/api/analysis/image", {
  method: "POST",
  body: formData, // ahora incluye userId
  headers: {
    "Authorization": `Bearer ${token}`
  }
});
```

---

## Resumen de Decisión

**¿Cuál usar?**

- **Pruebas/Demo?** → `/api/analysis/image-quick` (sin usuario)
- **Usar en App?** → `/api/analysis/image` (con usuario)

Ambos usan el mismo motor de análisis de OpenAI, la diferencia es **solo en la BD**.
