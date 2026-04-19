# 🎬 Demo - Uso Práctico

## Demo 1: Test Más Rápido (Sin Usuario)

### Terminal:

```bash
$ npm run test:quick -- ./mi-comida.jpg
```

### Salida:

```
📸 Analizando imagen (SIN USUARIO)...

Archivo: ./mi-comida.jpg
Hora: 12:00
Tipo MIME: image/jpeg
Tamaño: 245.32 KB

📡 Status Code: 200

✅ Análisis completado!

🍽️  Platillos detectados:
  1. Arroz blanco (1 taza)
  2. Pechuga de pollo (150g)
  3. Ensalada (1 plato)

🔥 Información Nutricional:

  ├─ Calorías: 520 kcal
  ├─ Proteína: 42g
  ├─ Carbohidratos: 52g
  └─ Grasas: 12g

⏰ Tipo de comida: almuerzo

📝 Análisis del plato:
Un almuerzo equilibrado con proteína magra, carbohidratos complejos 
y vegetales. La pechuga de pollo es una excelente fuente de proteína. 
El arroz blanco proporciona energía rápida. La ensalada añade fibra 
y vitaminas importantes.

════════════════════════════════════════════════════════════
✨ Test completado exitosamente
```

---

## Demo 2: Con Hora Específica

### Terminal:

```bash
$ npm run test:quick -- ./desayuno.jpg 07:30
```

### Salida:

```
📸 Analizando imagen (SIN USUARIO)...

Archivo: ./desayuno.jpg
Hora: 07:30
Tipo MIME: image/jpeg
Tamaño: 180.15 KB

📡 Status Code: 200

✅ Análisis completado!

🍽️  Platillos detectados:
  1. Huevos revueltos (2 huevos)
  2. Pan tostado (2 rebanadas)
  3. Café con leche

🔥 Información Nutricional:

  ├─ Calorías: 380 kcal
  ├─ Proteína: 16g
  ├─ Carbohidratos: 35g
  └─ Grasas: 18g

⏰ Tipo de comida: desayuno

📝 Análisis del plato:
Un desayuno nutritivo que combina proteínas (huevos), carbohidratos 
(pan) y calcio (leche). Proporciona energía sostenida para la mañana.

════════════════════════════════════════════════════════════
✨ Test completado exitosamente
```

---

## Demo 3: Documentación Disponible

### Ver guía rápida:

```bash
$ cat QUICK_START.md
```

### Ver documentación completa:

```bash
$ cat TEST_README.md
```

### Ver comparativa de endpoints:

```bash
$ cat ENDPOINTS_COMPARATIVA.md
```

### Ver ejemplos de código:

```bash
$ cat ejemplos.js
```

---

## Demo 4: Ver Todos los Scripts Disponibles

### Lista de scripts:

```bash
$ npm run
```

### Salida esperada:

```
Lifecycle scripts included in nutrivision-backend:
  dev        nodemon src/server.js
  start      node src/server.js
  test:validate  node validate-setup.js
  test:quick     node test-quick.js
  test:image     node test-image-analysis.js
  test:setup     node setup-test.js
  test:examples  node ejemplos.js
```

---

## Demo 5: Setup Completo Paso a Paso

### 1️⃣ Inicia el servidor

```bash
$ npm run dev

> nodemon src/server.js

[nodemon] 3.1.9
[nodemon] to restart at any time, type `rs`
[nodemon] watching path(s): *.*
[nodemon] watching extensions: js,json
NutriVision backend corriendo en puerto 3000
```

**Dejar corriendo en esta terminal** ✅

### 2️⃣ En otra terminal, valida la configuración

```bash
$ npm run test:validate

🔍 Validando configuración de NutriVision Backend...

1️⃣  Verificando servidor en http://localhost:3000...
   ✅ Servidor está corriendo

2️⃣  Verificando archivo .env...
   ✅ Archivo .env encontrado

3️⃣  Verificando variables de entorno...
   ✅ Variables de entorno completas
      • OPENAI_API_KEY: sk-proj-6...wJ44
      • DATABASE_URL: mongodb://...
      • OPENAI_MODEL: gpt-4o-mini

────────────────────────────────────────────────────────────

📋 Resumen de Validación:

✅ Todo está configurado correctamente!

📝 Próximos pasos:

1. Ejecuta el test:
   npm run test:quick -- ./tu-imagen.jpg
```

### 3️⃣ Ejecuta el test sin usuario

```bash
$ npm run test:quick -- ./comida.jpg

📸 Analizando imagen (SIN USUARIO)...

[... salida de análisis ...]

✨ Test completado exitosamente
```

✅ **¡Exitoso!**

---

## Demo 6: Si Quieres Guardar en BD (Con Usuario)

### Obtener un User ID:

```bash
$ mongo
> use nutrivision
> db.users.findOne({})
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "name": "Juan Pérez",
  "email": "juan@example.com",
  ...
}
# Copiar el _id
```

### Ejecutar test con usuario:

```bash
$ npm run test:image -- ./comida.jpg 507f1f77bcf86cd799439011

📸 Analizando imagen...

Archivo: ./comida.jpg
User ID: 507f1f77bcf86cd799439011
[...]

📡 Status Code: 201

✅ Análisis completado y guardado en BD!

📊 Información:
  • Guardado en: Analysis (_id: 507f1f77bcf86cd799439050)
  • ComidaConsuida: ConsumedDish (_id: 507f1f77bcf86cd799439051)
  • Usuario actualizado: todayNutritionSummary
```

---

## Demo 7: Verificar que se Guardó

### Ver análisis guardado:

```bash
$ mongo
> use nutrivision
> db.analyses.find({}).pretty()
{
  "_id": ObjectId("507f1f77bcf86cd799439050"),
  "userId": ObjectId("507f1f77bcf86cd799439011"),
  "imageName": "comida.jpg",
  "nutrition": {
    "calories": 520,
    "proteinGrams": 42,
    "carbsGrams": 52,
    "fatGrams": 12
  },
  "createdAt": ISODate("2026-04-17T14:35:22.000Z")
}
```

### Ver comida consumida:

```bash
> db.consumeddishes.find({}).pretty()
{
  "_id": ObjectId("507f1f77bcf86cd799439051"),
  "userId": ObjectId("507f1f77bcf86cd799439011"),
  "dishDate": "2026-04-17",
  "dishTime": "14:35",
  "calories": 520,
  "mealType": "almuerzo",
  "carbsGrams": 52,
  "proteinGrams": 42,
  "fatGrams": 12
}
```

✅ **¡Datos guardados correctamente!**

---

## Demo 8: Uso desde Node.js

### Archivo: `mi-script.js`

```javascript
const { sendImageAnalysisRequest } = require("./ejemplos.js");

async function analizar() {
  try {
    const response = await sendImageAnalysisRequest(
      "./mi-comida.jpg",
      "507f1f77bcf86cd799439011"
    );

    console.log("✅ Análisis completado!");
    console.log("Calorías:", response.nutrition.calories);
    console.log("Platillos:", response.dishes.map(d => d.name).join(", "));
  } catch (error) {
    console.error("❌ Error:", error.message);
  }
}

analizar();
```

### Ejecutar:

```bash
$ node mi-script.js
✅ Análisis completado!
Calorías: 520
Platillos: Arroz blanco, Pechuga de pollo, Ensalada
```

---

## Demo 9: Usando cURL

### Sin usuario (más simple):

```bash
curl -X POST http://localhost:3000/api/analysis/image-quick \
  -F "image=@/path/to/comida.jpg" \
  -F "photoTakenTime=14:30"
```

### Salida:

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
  "plateAnalysis": "..."
}
```

### Con usuario:

```bash
curl -X POST http://localhost:3000/api/analysis/image \
  -F "image=@/path/to/comida.jpg" \
  -F "userId=507f1f77bcf86cd799439011" \
  -F "photoTakenTime=14:30" \
  -F "dishDate=2026-04-17"
```

---

## Demo 10: Prueba Rápida de 30 Segundos

### Terminal 1:

```bash
cd backend
npm run dev
```

### Terminal 2 (espera 5 segundos):

```bash
npm run test:quick -- ./comida.jpg
```

**¡Listo!** Ya tienes análisis completo sin usuario 🎉

---

## Conclusión

| Necesidad | Comando |
|-----------|---------|
| Prueba rápida | `npm run test:quick -- ./imagen.jpg` |
| Demo a alguien | `npm run test:quick -- ./imagen.jpg` |
| Testing | `npm run test:validate && npm run test:quick -- test.jpg` |
| Guardar en BD | `npm run test:image -- ./imagen.jpg <USER_ID>` |
| Ver ejemplos | `npm run test:examples -- ./imagen.jpg <USER_ID>` |

