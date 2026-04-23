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
        val isToday = sameDate(date, Calendar.getInstance())
        val isSelected = sameDate(date, selectedDate)
        
        // Verificar si hay datos en memoria o en caché para marcar el punto en el calendario
        val hasRecords = mealsByDate.containsKey(dateStr) || 
                        DataCacheManager.getCache(this, "history_$dateStr", AnalysesResponse::class.java) != null

        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(44)).apply {
                weight = 1f
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            gravity = Gravity.CENTER
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(when {
                isSelected -> Color.WHITE
                !inVisibleMonth -> Color.parseColor("#C4C4C4")
                else -> Color.BLACK
            })
            background = when {
                isSelected -> roundedBackground(Color.parseColor("#2F2F2F"), 10f)
                isToday -> roundedStrokeBackground(Color.TRANSPARENT, Color.parseColor("#F86E00"), 1.5f, 10f)
                hasRecords -> roundedBackground(Color.parseColor("#E8F5E9"), 10f)
                else -> null
            }
            if (inVisibleMonth) {
                setOnClickListener { selectDate(date) }
            } else {
                alpha = 0.7f
            }
        }
    }

    private fun renderMealsForSelectedDate() {
        mealContainer.removeAllViews()
        val dateStr = dateKey(selectedDate)
        val meals = mealsByDate[dateStr] ?: emptyList()

        if (meals.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Sin registros para esta fecha"
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, dp(20))
            }
            mealContainer.addView(emptyText)
            return
        }

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(12), dp(8), dp(12), dp(12))
            }
            radius = dp(16).toFloat()
            setCardBackgroundColor(Color.WHITE)
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
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(14), 0, dp(14)) }
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
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(TextView(this).apply { text = "${meal.section}: ${meal.title}"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) })
        left.addView(TextView(this).apply { text = meal.time; textSize = 13f; setTextColor(Color.parseColor("#F86E00")) })

        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        right.addView(TextView(this).apply { text = meal.calories; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) })
        right.addView(TextView(this).apply { text = "KCAL"; textSize = 10f; setTextColor(Color.GRAY) })

        row.addView(left)
        row.addView(right)
        return row
    }

    private fun selectDate(date: Calendar) {
        selectedDate = cloneCalendar(date)
        renderScreen()
        cargarAnalisisDelDia(date)
    }

    private fun cargarAnalisisDelDia(date: Calendar) {
        val dateStr = dateKey(date)
        val token = TokenManager.getToken(this) ?: return

        // 1. Intentar cargar desde caché para respuesta inmediata
        val cachedResponse = DataCacheManager.getCache(this, "history_$dateStr", AnalysesResponse::class.java)
        if (cachedResponse != null) {
            Log.d(TAG, "Mostrando historial desde caché para $dateStr")
            procesarYMostrarAnalisis(dateStr, cachedResponse)
        }

        // 2. Consultar al servidor para datos frescos
        lifecycleScope.launch {
            try {
                val response = repository.getAnalysesByDate(token, dateStr)
                if (response.isSuccessful && response.body() != null) {
                    val analysesResponse = response.body()!!
                    DataCacheManager.saveCache(this@HistorialActivity, "history_$dateStr", analysesResponse)
                    procesarYMostrarAnalisis(dateStr, analysesResponse)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red en historial", e)
            }
        }
    }

    private fun procesarYMostrarAnalisis(dateStr: String, response: AnalysesResponse) {
        val meals = response.data.map { analysis ->
            val dishNames = analysis.foodsDetected.joinToString(", ") { it.name }.ifEmpty { "Plato" }
            val time = extractHourFromString(analysis.createdAt)
            MealEntry(
                section = capitalizeMealType(analysis.rawModelResponse?.mealType ?: "Comida"),
                title = dishNames,
                time = time,
                calories = "${analysis.nutrition.calories.toInt()}"
            )
        }
        mealsByDate[dateStr] = meals
        if (dateStr == dateKey(selectedDate)) {
            renderMealsForSelectedDate()
        }
        renderCalendar()
    }

    private fun extractHourFromString(dateTimeStr: String): String {
        return try {
            val pattern = "T(\\d{2}:\\d{2})".toRegex()
            pattern.find(dateTimeStr)?.groupValues?.get(1) ?: "--:--"
        } catch (e: Exception) { "--:--" }
    }

    private fun capitalizeMealType(type: String): String = type.lowercase().replaceFirstChar { it.titlecase() }

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
    private fun roundedBackground(c: Int, r: Float) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(r.toInt()).toFloat(); setColor(c) }
    private fun roundedStrokeBackground(fc: Int, sc: Int, sw: Float, r: Float) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(r.toInt()).toFloat(); setColor(fc); setStroke(dp(sw.toInt()), sc) }
    private fun capitalizeFirst(t: String) = t.replaceFirstChar { it.titlecase(esLocale) }
}