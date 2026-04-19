# NutriVision Backend

Backend en Node.js/Express para:
- Recibir imagenes de comida desde la app.
- Enviar la imagen a OpenAI para estimar informacion nutrimental.
- Guardar el resultado en MongoDB.

## 1. Instalacion

```bash
cd backend
npm install
```

## 2. Configuracion

1. Copia `.env.example` a `.env`
2. Completa:

```env
PORT=4000
MONGODB_URI=...
OPENAI_API_KEY=...
OPENAI_MODEL=gpt-4.1-mini
```

## 3. Ejecutar

```bash
npm run dev
```

## Endpoints

### Health check
`GET /health`

### Calcular meta diaria kcal por IA
`POST /api/analysis/daily-goal/:userId`

Calcula la meta diaria de calorias segun perfil del usuario y la guarda en `dailyCalorieGoalKcal`.

### Analizar imagen nutrimental
`POST /api/analysis/image`

`multipart/form-data`:
- `image` (archivo, requerido)
- `userId` (texto, requerido)
- `photoTakenTime` (texto `HH:mm`, opcional)
- `dishDate` (texto `YYYY-MM-DD`, opcional)

Respuesta incluye:
- platos detectados (`dishes`)
- tipo de comida (`mealType`)
- analisis del plato (`plateAnalysis`)
- nutricion total (`carbs/proteins/fats/calories`)
- guardado en `Analysis` y `ConsumedDish`, y actualizacion de `todayNutritionSummary` del usuario

### Listar analisis
`GET /api/analysis`
