# 📚 Índice de Documentación - NutriVision Backend Tests

## 🎯 ¿Qué quiero hacer?

### 1. **Quiero probar rápidamente sin crear usuario**
   - 📄 Documento: [QUICK_START.md](./QUICK_START.md)
   - ⏱️ Tiempo: 2 minutos
   - 🎬 Ver también: [DEMO.md - Demo 1](./DEMO.md#demo-1-test-más-rápido-sin-usuario)

### 2. **Quiero ver un ejemplo de salida completa**
   - 📄 Documento: [DEMO.md](./DEMO.md)
   - 🎬 Demos visuales con salida esperada
   - ✅ Muestra casos reales

### 3. **Quiero entender la diferencia entre los 2 endpoints**
   - 📄 Documento: [ENDPOINTS_COMPARATIVA.md](./ENDPOINTS_COMPARATIVA.md)
   - 📊 Tabla comparativa
   - 🔄 Flujos de datos

### 4. **Quiero documentación técnica completa**
   - 📄 Documento: [TEST_README.md](./TEST_README.md)
   - 📋 Todos los parámetros
   - 🔧 Solución de problemas

### 5. **Quiero código para integrar en mi aplicación**
   - 📄 Documento: [ejemplos.js](./ejemplos.js)
   - 📝 Código listo para copiar
   - 🚀 Funciones reutilizables

---

## 📖 Documentos Disponibles

### ⚡ Para Empezar (RECOMENDADO)

| Documento | Contenido | Tiempo |
|-----------|----------|--------|
| [QUICK_START.md](./QUICK_START.md) | **Guía de 3 pasos** | 2 min |
| [DEMO.md](./DEMO.md) | **Ejemplos visuales** | 5 min |

### 📚 Documentación Técnica

| Documento | Contenido |
|-----------|----------|
| [TEST_README.md](./TEST_README.md) | Guía completa de todos los tests |
| [ENDPOINTS_COMPARATIVA.md](./ENDPOINTS_COMPARATIVA.md) | Análisis de endpoints sin/con usuario |

### 💻 Código

| Documento | Contenido |
|-----------|----------|
| [test-quick.js](./test-quick.js) | Test sin usuario (RECOMENDADO) |
| [test-image-analysis.js](./test-image-analysis.js) | Test con usuario |
| [setup-test.js](./setup-test.js) | Test automático con descarga de imagen |
| [ejemplos.js](./ejemplos.js) | Ejemplos de código Node.js |
| [validate-setup.js](./validate-setup.js) | Validar configuración |

---

## 🚀 Flujo Rápido: 30 Segundos

### 1. Inicia servidor (Terminal 1)
```bash
npm run dev
```

### 2. Ejecuta test (Terminal 2)
```bash
npm run test:quick -- ./tu-imagen.jpg
```

### 3. Ver resultado
```json
{
  "dishes": [...],
  "nutrition": {
    "calories": 450,
    "proteinGrams": 32
  },
  ...
}
```

📄 Ver paso a paso en: [QUICK_START.md](./QUICK_START.md)

---

## 🎬 Scripts Disponibles

```bash
npm run dev              # Inicia servidor
npm run start            # Inicia servidor (producción)
npm run test:quick       # Test sin usuario ⚡ (RECOMENDADO)
npm run test:validate    # Valida configuración
npm run test:image       # Test con usuario
npm run test:setup       # Test automático con descarga
npm run test:examples    # Ver ejemplos de código
```

---

## 🔄 Decisión: Sin Usuario vs Con Usuario

### Sin Usuario (`/api/analysis/image-quick`)

```bash
npm run test:quick -- ./comida.jpg
```

✅ No requiere cuenta  
✅ Testing rápido  
✅ Demo  
❌ No guarda datos  

📄 Documentación: [ENDPOINTS_COMPARATIVA.md](./ENDPOINTS_COMPARATIVA.md)

### Con Usuario (`/api/analysis/image`)

```bash
npm run test:image -- ./comida.jpg <USER_ID>
```

✅ Guarda en BD  
✅ Historial  
✅ Producción  
❌ Requiere usuario  

📄 Documentación: [ENDPOINTS_COMPARATIVA.md](./ENDPOINTS_COMPARATIVA.md)

---

## ❓ Preguntas Frecuentes

### ❌ "Error: Cannot find module"
**Respuesta:** Ejecuta `npm run dev` en otra terminal  
📄 Ver: [TEST_README.md - Problemas](./TEST_README.md#solución-de-problemas)

### ❌ "El archivo no existe"
**Respuesta:** Usa ruta correcta a la imagen  
📄 Ver: [QUICK_START.md](./QUICK_START.md)

### ❌ "OpenAI no devolvió JSON válido"
**Respuesta:** Verifica OPENAI_API_KEY en .env  
📄 Ver: [TEST_README.md - Problemas](./TEST_README.md#solución-de-problemas)

### ✅ "¿Cómo integro en mi código?"
**Respuesta:** Mira [ejemplos.js](./ejemplos.js)  
📄 Ver: [DEMO.md - Demo 8](./DEMO.md#demo-8-uso-desde-nodejs)

---

## 🎓 Próximos Pasos

### 1. Ejecuta tu primer test
```bash
npm run test:quick -- ./tu-imagen.jpg
```

### 2. Lee la documentación relevante
- Sin usuario → [QUICK_START.md](./QUICK_START.md)
- Con usuario → [ENDPOINTS_COMPARATIVA.md](./ENDPOINTS_COMPARATIVA.md)

### 3. Integra en tu código
- Ver [ejemplos.js](./ejemplos.js)
- Adapta las funciones a tu caso

### 4. Revisa DEMO para más casos
- [DEMO.md](./DEMO.md)

---

## 📊 Estructura de Respuesta

Ambos endpoints devuelven:

```json
{
  "dishes": [
    {
      "name": "Arroz blanco",
      "estimatedPortion": "1 taza"
    }
  ],
  "nutrition": {
    "calories": 450,
    "proteinGrams": 32,
    "carbsGrams": 45,
    "fatGrams": 8
  },
  "mealType": "almuerzo",
  "plateAnalysis": "Descripción detallada..."
}
```

Endpoint con usuario devuelve además:
```json
{
  "analysis": { /* documento guardado */ },
  "consumedDish": { /* comida guardada */ },
  "todayNutritionSummary": { /* resumen del día */ }
}
```

---

## 🗺️ Mapa de Archivos

```
backend/
├── 📘 INDICES.md ..................... (este archivo)
├── 📘 QUICK_START.md ................. Guía de 3 pasos
├── 📘 DEMO.md ........................ Ejemplos visuales
├── 📘 TEST_README.md ................. Documentación completa
├── 📘 ENDPOINTS_COMPARATIVA.md ....... Análisis endpoints
│
├── 🐍 test-quick.js .................. Test sin usuario ⚡
├── 🐍 test-image-analysis.js ......... Test con usuario
├── 🐍 setup-test.js .................. Test automático
├── 🐍 validate-setup.js .............. Validación
├── 🐍 ejemplos.js .................... Código de ejemplo
│
├── src/
│   ├── routes/
│   │   └── analysis.routes.js ........ ← Incluye /image-quick
│   ├── services/
│   │   └── openai.service.js
│   ├── app.js
│   └── server.js
│
├── package.json ...................... Scripts npm
├── .env ............................... Variables de entorno
└── README.md .......................... (si existe)
```

---

## 🔗 Enlaces Rápidos

| Tarea | Documento |
|-------|-----------|
| Empezar ya | [QUICK_START.md](./QUICK_START.md) |
| Ver ejemplos | [DEMO.md](./DEMO.md) |
| Leer todo | [TEST_README.md](./TEST_README.md) |
| Comparar | [ENDPOINTS_COMPARATIVA.md](./ENDPOINTS_COMPARATIVA.md) |
| Código | [ejemplos.js](./ejemplos.js) |

---

## ⚡ TL;DR (Muy Corto)

```bash
# Terminal 1
npm run dev

# Terminal 2
npm run test:quick -- ./comida.jpg
```

**¡Listo!** 🎉

---

**Last Updated:** 2026-04-17  
**Version:** 1.0  
**Status:** ✅ Completo
