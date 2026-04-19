# 🚀 Quick Start - Test de Análisis de Imagen

## ⚡ Sin Usuario (Más Rápido)

### 3 Pasos Solamente:

#### Paso 1: Inicia el servidor

```bash
cd backend
npm run dev
```

#### Paso 2: Descarga una imagen de comida

- Toma una foto de tu plato, O
- Descarga una imagen de la web

#### Paso 3: Ejecuta el test

```bash
# En otra terminal
npm run test:quick -- ./tu-imagen.jpg
```

**¡Listo!** No necesitas usuario 🎉

---

## 🎯 Ejemplo Completo Sin Usuario

```bash
# Terminal 1: Servidor
npm run dev

# Terminal 2: Test (sin usuario)
npm run test:quick -- ./comida.jpg
```

**Salida:**
```
📸 Analizando imagen (SIN USUARIO)...

✅ Análisis completado!

🍽️  Platillos detectados:
  1. Arroz con pollo (1 plato mediano)

🔥 Información Nutricional:

  ├─ Calorías: 450 kcal
  ├─ Proteína: 32g
  ├─ Carbohidratos: 45g
  └─ Grasas: 8g

⏰ Tipo de comida: almuerzo

📝 Análisis del plato:
[Análisis detallado...]

✨ Test completado exitosamente
```

---

## 👤 Con Usuario (Para Guardar Datos)

Si quieres **guardar el análisis** en la BD:

### Paso 1: Asegúrate que el servidor está corriendo

```bash
npm run dev
```

### Paso 2: Obtén un User ID

```bash
# Desde MongoDB Compass o terminal:
mongo
use nutrivision
db.users.findOne({})  # Copia el _id
```

### Paso 3: Ejecuta el test con usuario

```bash
npm run test:image -- ./tu-imagen.jpg <USER_ID>
```

---

## 📚 Todos los Scripts Disponibles

| Script | Propósito | Requiere Usuario |
|--------|-----------|-----------------|
| `npm run test:quick` | Test simple sin usuario | ❌ NO |
| `npm run test:validate` | Valida configuración | ❌ NO |
| `npm run test:setup` | Test automático | ✅ SÍ |
| `npm run test:image` | Test con usuario | ✅ SÍ |
| `npm run test:examples` | Ver ejemplos | ✅ SÍ |

---

## 🎯 Tu respuesta incluye:

✅ Platillos detectados  
✅ Calorías totales  
✅ Macronutrientes (proteína, carbos, grasas)  
✅ Tipo de comida detectado  
✅ Análisis detallado del plato  

---

## ❌ Problemas Comunes

| Problema | Solución |
|----------|----------|
| "Error al conectar con el servidor" | Ejecuta `npm run dev` |
| "El archivo no existe" | USA la ruta correcta a la imagen |
| "OpenAI no devolvió JSON valido" | Verifica OPENAI_API_KEY en .env |

---

## 🚬 Próximos Pasos

1. **Ver ejemplo completo (con más opciones de parseo):**
   ```bash
   npm run test:examples -- ./tu-imagen.jpg <USER_ID>
   ```

2. **Leer documentación completa:**
   ```bash
   cat TEST_README.md
   ```

3. **Integrar en tu código:**
   ```javascript
   const { sendImageAnalysisRequest } = require("./ejemplos.js");
   
   const response = await sendImageAnalysisRequest(
     "./comida.jpg",
     "YOUR_USER_ID"
   );
   ```

---

**¡Eso es todo! 🎉**
