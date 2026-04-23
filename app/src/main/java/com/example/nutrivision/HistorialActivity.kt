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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.model.AnalysesResponse
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

        // Cargar inmediatamente
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
        btnBack.setOnClickListener { finish() }
        btnMonth.setOnClickListener { showMonthPicker() }
        btnYear.setOnClickListener { showYearPicker() }
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
        val firstOfMonth = cloneCalendar(visibleMonth).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 0 else firstDayOfWeek - Calendar.SUNDAY
        val gridStart = cloneCalendar(firstOfMonth).apply { add(Calendar.DAY_OF_MONTH, -offset) }

        repeat(6) { rowIndex ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            repeat(7) { colIndex ->
                val cellDate = cloneCalendar(gridStart).apply { add(Calendar.DAY_OF_MONTH, rowIndex * 7 + colIndex) }
                row.addView(createDayCell(cellDate))
            }
            calendarRowsContainer.addView(row)
        }
    }

    private fun createDayCell(date: Calendar): TextView {
        val dateStr = dateKey(date)
        val inVisibleMonth = sameMonth(date, visibleMonth)
        val isSelected = sameDate(date, selectedDate)

        // Verificar si hay datos para esta fecha
        val hasRecords = mealsByDate.containsKey(dateStr) && !mealsByDate[dateStr].isNullOrEmpty()

        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            gravity = Gravity.CENTER
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD

            // Prioridad: seleccionado > tiene datos > mes actual > otro mes
            when {
                isSelected -> {
                    setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.white))
                    background = roundedBg(ContextCompat.getColor(this@HistorialActivity, R.color.black))
                }
                hasRecords && inVisibleMonth -> {
                    setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.black))
                    background = roundedBg(ContextCompat.getColor(this@HistorialActivity, R.color.card_green))
                }
                inVisibleMonth -> {
                    setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.black))
                    background = null
                }
                else -> {
                    setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.gray))
                    background = null
                    alpha = 0.7f
                }
            }

            if (inVisibleMonth) {
                setOnClickListener { selectDate(date) }
            }
        }
    }

    private fun renderMealsForSelectedDate() {
        mealContainer.removeAllViews()
        val dateStr = dateKey(selectedDate)
        val meals = mealsByDate[dateStr] ?: emptyList()

        if (meals.isEmpty()) {
            mealContainer.addView(TextView(this).apply {
                text = "Sin registros para esta fecha"
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, 0)
                setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.gray))
                textSize = 14f
            })
            return
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        // Agrupar por tipo de comida (section)
        val mealsByType = meals.groupBy { it.section }

        mealsByType.forEach { (mealType, typeMeals) ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(12)) }
                radius = dp(16).toFloat()
                cardElevation = 2f
                setCardBackgroundColor(ContextCompat.getColor(this@HistorialActivity, R.color.card_background))
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#EEF2F7")
            }

            val list = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }

            // Título del tipo de comida en NARANJA (usando color orange del colors.xml)
            list.addView(TextView(this).apply {
                text = mealType  // "Desayuno", "Almuerzo", "Cena", "Merienda"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.orange))
                setPadding(0, 0, 0, dp(8))
            })

            typeMeals.forEachIndexed { i, meal ->
                list.addView(createMealRow(meal))
                if (i < typeMeals.size - 1) {
                    list.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(1)
                        ).apply { setMargins(0, dp(12), 0, dp(12)) }
                        setBackgroundColor(Color.parseColor("#EEF2F7"))
                    })
                }
            }
            card.addView(list)
            mainContainer.addView(card)
        }
        mealContainer.addView(mainContainer)
    }

    private fun createMealRow(meal: MealEntry): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val leftColumn = LinearLayout(this@HistorialActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            leftColumn.addView(TextView(this@HistorialActivity).apply {
                text = meal.title  // Nombre del plato
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.black))
                maxLines = 2
            })
            leftColumn.addView(TextView(this@HistorialActivity).apply {
                text = meal.time  // Hora
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.orange))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(4), 0, 0)
            })

            val rightColumn = LinearLayout(this@HistorialActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }
            val caloriesValue = meal.calories.replace(" kcal", "").toIntOrNull() ?: 0
            rightColumn.addView(TextView(this@HistorialActivity).apply {
                text = caloriesValue.toString()
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.black))
            })
            rightColumn.addView(TextView(this@HistorialActivity).apply {
                text = "KCAL"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@HistorialActivity, R.color.gray))
                setPadding(0, dp(2), 0, 0)
            })

            addView(leftColumn)
            addView(rightColumn)
        }
    }

    private fun selectDate(date: Calendar) {
        selectedDate = cloneCalendar(date)
        // Si la fecha seleccionada está fuera del mes visible, cambiar el mes visible
        if (!sameMonth(selectedDate, visibleMonth)) {
            visibleMonth = cloneCalendar(selectedDate).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                normalizeToDate()
            }
        }
        renderScreen()
        cargarAnalisisDelDia(date)
    }

    private fun cargarAnalisisDelDia(date: Calendar) {
        val dateStr = dateKey(date)
        val token = TokenManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = repository.getAnalysesByDate(token, dateStr)
                if (response.isSuccessful && response.body() != null) {
                    val analyses = response.body()!!.data
                    val meals = analyses.mapNotNull { analysis ->
                        try {
                            val dishNames = analysis.foodsDetected
                                ?.joinToString(", ") { it.name }
                                ?: "Plato no especificado"

                            val timeStr = analysis.localTime ?: extractHourFromString(analysis.createdAt)

                            // Capitalizar correctamente el tipo de comida
                            val mealType = analysis.rawModelResponse?.mealType ?: "Comida"
                            val capitalizedMealType = when (mealType.lowercase()) {
                                "desayuno" -> "Desayuno"
                                "almuerzo" -> "Almuerzo"
                                "cena" -> "Cena"
                                "merienda" -> "Merienda"
                                else -> mealType.replaceFirstChar { it.uppercase() }
                            }

                            MealEntry(
                                section = capitalizedMealType,
                                title = dishNames,
                                time = timeStr,
                                calories = "${analysis.nutrition.calories.toInt()} kcal"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error procesando análisis", e)
                            null
                        }
                    }
                    mealsByDate[dateStr] = meals
                    renderMealsForSelectedDate()
                    renderCalendar()
                } else {
                    mealsByDate[dateStr] = emptyList()
                    renderMealsForSelectedDate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en cargarAnalisisDelDia", e)
                mealsByDate[dateStr] = emptyList()
                renderMealsForSelectedDate()
            }
        }
    }

    private fun extractHourFromString(dateTimeStr: String): String {
        return try {
            val pattern = "T(\\d{2}:\\d{2})".toRegex()
            pattern.find(dateTimeStr)?.groupValues?.get(1) ?: "--:--"
        } catch (e: Exception) { "--:--" }
    }

    private fun showMonthPicker() {
        MaterialAlertDialogBuilder(this).setTitle("Mes").setItems(monthLabels) { _, w ->
            visibleMonth.set(Calendar.MONTH, w)
            selectDate(visibleMonth)
        }.show()
    }

    private fun showYearPicker() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val years = (year - 5..year + 1).map { it.toString() }.toTypedArray()
        MaterialAlertDialogBuilder(this).setTitle("Año").setItems(years) { _, w ->
            visibleMonth.set(Calendar.YEAR, years[w].toInt())
            selectDate(visibleMonth)
        }.show()
    }

    private fun sameMonth(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    private fun sameDate(a: Calendar, b: Calendar) = sameMonth(a, b) && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
    private fun dateKey(calendar: Calendar) = dateKeyFormat.format(calendar.time)
    private fun cloneCalendar(source: Calendar) = (source.clone() as Calendar).apply { normalizeToDate() }
    private fun Calendar.normalizeToDate() { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun roundedBg(c: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(10).toFloat()
        setColor(c)
    }
    private fun capitalizeFirst(t: String) = t.replaceFirstChar { it.titlecase(esLocale) }
}