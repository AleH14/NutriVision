package com.example.nutrivision

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class HistorialActivity : AppCompatActivity() {

    data class MealEntry(
        val section: String,
        val title: String,
        val time: String,
        val calories: String
    )

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnBack: TextView
    private lateinit var btnMonth: TextView
    private lateinit var btnYear: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var calendarRowsContainer: LinearLayout
    private lateinit var mealContainer: LinearLayout

    private val esLocale = Locale("es", "ES")
    private val monthLabels = arrayOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val selectedDateFormat = SimpleDateFormat("d 'de' MMMM", esLocale)

    private lateinit var visibleMonth: Calendar
    private lateinit var selectedDate: Calendar

    private val mealsByDate = mutableMapOf<String, List<MealEntry>>()
    private lateinit var repository: NutriRepository
    private val TAG = "HistorialActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        repository = NutriRepository(RetrofitClient.instance)

        initCalendars()
        bindViews()
        setupNavigation()
        setupActions()
        renderScreen()
        
        // Cargar automáticamente análisis de HOY
        cargarAnalisisDelDia(selectedDate)
    }

    private fun initCalendars() {
        visibleMonth = Calendar.getInstance().apply {
            normalizeToDate()
            set(Calendar.DAY_OF_MONTH, 1)
        }
        selectedDate = Calendar.getInstance().apply {
            normalizeToDate()
        }
    }

    private fun bindViews() {
        bottomNav = findViewById(R.id.bottomNavigation)
        btnBack = findViewById(R.id.btnBackHistorial)
        btnMonth = findViewById(R.id.btnMonth)
        btnYear = findViewById(R.id.btnYear)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        calendarRowsContainer = findViewById(R.id.calendarRowsContainer)
        mealContainer = findViewById(R.id.mealContainer)
    }

    private fun setupNavigation() {
        bottomNav.selectedItemId = R.id.nav_historial

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, InicioActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_historial -> true

                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            finish()
        }

        btnMonth.setOnClickListener {
            showMonthPicker()
        }

        btnYear.setOnClickListener {
            showYearPicker()
        }
    }

    private fun renderScreen() {
        btnMonth.text = monthLabels[visibleMonth.get(Calendar.MONTH)]
        btnYear.text = visibleMonth.get(Calendar.YEAR).toString()
        tvSelectedDate.text = capitalizeFirst(selectedDateFormat.format(selectedDate.time))

        renderCalendar()
        renderMealsForSelectedDate()
    }

    private fun renderCalendar() {
        calendarRowsContainer.removeAllViews()

        val firstOfMonth = cloneCalendar(visibleMonth).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 0 else firstDayOfWeek - Calendar.SUNDAY

        val gridStart = cloneCalendar(firstOfMonth).apply {
            add(Calendar.DAY_OF_MONTH, -offset)
        }

        repeat(6) { rowIndex ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            repeat(7) { colIndex ->
                val index = rowIndex * 7 + colIndex
                val cellDate = cloneCalendar(gridStart).apply {
                    add(Calendar.DAY_OF_MONTH, index)
                }
                row.addView(createDayCell(cellDate))
            }

            calendarRowsContainer.addView(row)
        }
    }

    private fun createDayCell(date: Calendar): TextView {
        val inVisibleMonth = sameMonth(date, visibleMonth)
        val isToday = sameDate(date, Calendar.getInstance())
        val isSelected = sameDate(date, selectedDate)
        val hasRecords = mealsByDate.containsKey(dateKey(date))

        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(44)).apply {
                weight = 1f
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }

            gravity = Gravity.CENTER
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD

            setTextColor(
                when {
                    isSelected -> Color.WHITE
                    !inVisibleMonth -> Color.parseColor("#C4C4C4")
                    else -> resources.getColor(R.color.black, theme)
                }
            )

            background = when {
                isSelected -> roundedBackground(
                    fillColor = Color.parseColor("#2F2F2F"),
                    cornerDp = 10f
                )

                isToday -> roundedStrokeBackground(
                    fillColor = Color.TRANSPARENT,
                    strokeColor = resources.getColor(R.color.orange, theme),
                    strokeWidthDp = 1.5f,
                    cornerDp = 10f
                )

                hasRecords -> roundedBackground(
                    fillColor = resources.getColor(R.color.day_cell_marked_bg, theme),
                    cornerDp = 10f
                )

                else -> null
            }

            if (inVisibleMonth) {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectDate(date)
                }
            } else {
                alpha = 0.7f
            }
        }
    }

    private fun renderMealsForSelectedDate() {
        mealContainer.removeAllViews()

        val meals = mealsByDate[dateKey(selectedDate)].orEmpty()

        if (meals.isEmpty()) {
            val emptyCard = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(12), dp(8), dp(12), dp(12))
                }
                radius = dp(16).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(resources.getColor(R.color.card_background, theme))
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#E5E7EB")
            }

            emptyCard.addView(
                TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = "Sin registros para esta fecha"
                    setTextColor(resources.getColor(R.color.gris_claro, theme))
                    textSize = 15f
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(16), dp(18), dp(16), dp(18))
                }
            )

            mealContainer.addView(emptyCard)
            return
        }

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(12), dp(8), dp(12), dp(12))
            }
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(resources.getColor(R.color.card_background, theme))
            strokeWidth = dp(1)
            strokeColor = Color.parseColor("#EEF2F7")
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        meals.forEachIndexed { index, meal ->
            content.addView(createMealRow(meal))

            if (index != meals.lastIndex) {
                content.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {
                        setMargins(0, dp(14), 0, dp(14))
                    }
                    setBackgroundColor(Color.parseColor("#EEF2F7"))
                })
            }
        }

        card.addView(content)
        mealContainer.addView(card)
    }

    private fun createMealRow(meal: MealEntry): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        left.addView(TextView(this).apply {
            text = "${meal.section}: ${meal.title}"
            textSize = 17f
            setTextColor(resources.getColor(R.color.black, theme))
            typeface = Typeface.DEFAULT_BOLD
        })

        left.addView(TextView(this).apply {
            text = meal.time
            textSize = 13f
            setTextColor(resources.getColor(R.color.orange, theme))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
        })

        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        right.addView(TextView(this).apply {
            text = meal.calories
            textSize = 20f
            setTextColor(resources.getColor(R.color.black, theme))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        })

        right.addView(TextView(this).apply {
            text = "KCAL"
            textSize = 11f
            setTextColor(resources.getColor(R.color.gris_claro, theme))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        })

        row.addView(left)
        row.addView(right)

        return row
    }

    private fun selectDate(date: Calendar) {
        selectedDate = cloneCalendar(date)
        visibleMonth = cloneCalendar(date).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        renderScreen()
        cargarAnalisisDelDia(date)
    }

    private fun cargarAnalisisDelDia(date: Calendar) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.e(TAG, "Token no encontrado")
            return
        }
        
        val dateStr = dateKeyFormat.format(date.time)
        Log.d(TAG, "Cargando análisis para: $dateStr")
        
        lifecycleScope.launch {
            try {
                val response = repository.getAnalysesByDate(token, dateStr)
                
                if (response.isSuccessful && response.body() != null) {
                    val analysesResponse = response.body()!!
                    Log.d(TAG, "Análisis encontrados: ${analysesResponse.count}")
                    
                    // Convertir AnalysisItem a MealEntry
                    val meals = analysesResponse.data.map { analysis ->
                        val mealType = analysis.rawModelResponse?.mealType ?: "Merienda"
                        val dishNames = analysis.foodsDetected.joinToString(", ") { it.name }
                        
                        // Convertir hora de UTC a zona horaria local
                        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                        utcFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val parsedDate = utcFormat.parse(analysis.createdAt) ?: Calendar.getInstance().time
                        
                        // Formatear en zona horaria local
                        val localFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        localFormat.timeZone = java.util.TimeZone.getDefault()
                        val time = localFormat.format(parsedDate)
                        
                        Log.d(TAG, "Hora UTC: ${analysis.createdAt} -> Hora Local: $time")
                        
                        MealEntry(
                            section = capitalizeMealType(mealType),
                            title = dishNames,
                            time = time,
                            calories = "${analysis.nutrition.calories.toInt()} kcal"
                        )
                    }
                    
                    mealsByDate[dateStr] = meals
                    Log.d(TAG, "Comidas cargadas para $dateStr: ${meals.size}")
                    
                    // Actualizar vista
                    renderMealsForSelectedDate()
                    renderCalendar()
                } else {
                    Log.w(TAG, "Respuesta no exitosa: ${response.code()}")
                    mealsByDate[dateStr] = emptyList()
                    renderMealsForSelectedDate()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Error al cargar análisis", error)
                error.printStackTrace()
                Toast.makeText(this@HistorialActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun capitalizeMealType(type: String): String {
        return when (type.lowercase()) {
            "desayuno" -> "Desayuno"
            "almuerzo" -> "Almuerzo"
            "cena" -> "Cena"
            "merienda" -> "Merienda"
            else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun showMonthPicker() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Selecciona un mes")
            .setItems(monthLabels) { _, which ->
                visibleMonth.set(Calendar.MONTH, which)
                visibleMonth.set(Calendar.DAY_OF_MONTH, 1)

                val maxDay = visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentSelectedDay = min(selectedDate.get(Calendar.DAY_OF_MONTH), maxDay)

                selectedDate = cloneCalendar(visibleMonth).apply {
                    set(Calendar.DAY_OF_MONTH, currentSelectedDay)
                }

                renderScreen()
            }
            .show()
    }

    private fun showYearPicker() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 20..currentYear + 20).toList()
        val labels = years.map { it.toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Selecciona un año")
            .setItems(labels) { _, which ->
                visibleMonth.set(Calendar.YEAR, years[which])
                visibleMonth.set(Calendar.DAY_OF_MONTH, 1)

                val maxDay = visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentSelectedDay = min(selectedDate.get(Calendar.DAY_OF_MONTH), maxDay)

                selectedDate = cloneCalendar(visibleMonth).apply {
                    set(Calendar.DAY_OF_MONTH, currentSelectedDay)
                }

                renderScreen()
            }
            .show()
    }

    fun submitMealsForSelectedDate(meals: List<MealEntry>) {
        mealsByDate[dateKey(selectedDate)] = meals
        renderMealsForSelectedDate()
    }

    fun submitMealsForDate(date: Calendar, meals: List<MealEntry>) {
        mealsByDate[dateKey(date)] = meals
        if (sameDate(date, selectedDate)) {
            renderMealsForSelectedDate()
        }
    }

    private fun sameMonth(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    }

    private fun sameDate(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
    }

    private fun dateKey(calendar: Calendar): String {
        return dateKeyFormat.format(calendar.time)
    }

    private fun cloneCalendar(source: Calendar): Calendar {
        return (source.clone() as Calendar).apply {
            normalizeToDate()
        }
    }

    private fun Calendar.normalizeToDate() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun roundedBackground(fillColor: Int, cornerDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(cornerDp.toInt()).toFloat()
            setColor(fillColor)
        }
    }

    private fun roundedStrokeBackground(
        fillColor: Int,
        strokeColor: Int,
        strokeWidthDp: Float,
        cornerDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(cornerDp.toInt()).toFloat()
            setColor(fillColor)
            setStroke(dp(strokeWidthDp.toInt()), strokeColor)
        }
    }

    private fun capitalizeFirst(text: String): String {
        return text.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(esLocale) else it.toString()
        }
    }
}