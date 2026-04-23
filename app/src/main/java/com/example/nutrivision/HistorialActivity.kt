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
    private lateinit var tvSelectedDate: TextView
    private lateinit var calendarRowsContainer: LinearLayout
    private lateinit var mealContainer: LinearLayout
    private lateinit var btnMonth: TextView
    private lateinit var btnYear: TextView

    private val esLocale = Locale("es", "ES")
    private val monthLabels = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
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
        initViews()
        initCalendars()
        setupNavigation()
        setupActions()
        renderScreen()
        cargarAnalisisDelDia(selectedDate)
    }

    override fun onResume() {
        super.onResume()
        if (::selectedDate.isInitialized) {
            cargarAnalisisDelDia(selectedDate)
        }
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNavigation)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        calendarRowsContainer = findViewById(R.id.calendarRowsContainer)
        mealContainer = findViewById(R.id.mealContainer)
        btnMonth = findViewById(R.id.btnMonth)
        btnYear = findViewById(R.id.btnYear)
        findViewById<View>(R.id.btnBackHistorial).setOnClickListener { finish() }
    }

    private fun initCalendars() {
        visibleMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            normalize()
        }
        selectedDate = Calendar.getInstance().apply { normalize() }
    }

    private fun setupNavigation() {
        bottomNav.selectedItemId = R.id.nav_historial
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, InicioActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }
    }

    private fun setupActions() {
        btnMonth.setOnClickListener { showMonthPicker() }
        btnYear.setOnClickListener { showYearPicker() }
    }

    private fun renderScreen() {
        btnMonth.text = monthLabels[visibleMonth.get(Calendar.MONTH)]
        btnYear.text = visibleMonth.get(Calendar.YEAR).toString()
        tvSelectedDate.text = selectedDateFormat.format(selectedDate.time).replaceFirstChar { it.uppercase() }
        renderCalendar()
        renderMealsForSelectedDate()
    }

    private fun renderCalendar() {
        calendarRowsContainer.removeAllViews()
        val cal = (visibleMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val offset = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 0 else cal.get(Calendar.DAY_OF_WEEK) - 1
        cal.add(Calendar.DAY_OF_MONTH, -offset)

        repeat(6) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            repeat(7) {
                row.addView(createDayCell(cal.clone() as Calendar))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            calendarRowsContainer.addView(row)
        }
    }

    private fun createDayCell(date: Calendar): View {
        val isSelected = sameDate(date, selectedDate)
        val inMonth = sameMonth(date, visibleMonth)
        val dateKey = dateKeyFormat.format(date.time)
        val hasData = mealsByDate.containsKey(dateKey) && !mealsByDate[dateKey].isNullOrEmpty()

        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
            gravity = Gravity.CENTER
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD

            when {
                isSelected -> {
                    setTextColor(Color.WHITE)
                    background = roundedBg(Color.parseColor("#2F2F2F"))
                }
                hasData -> {
                    setTextColor(Color.BLACK)
                    background = roundedBg(Color.parseColor("#E8F5E9"))
                }
                inMonth -> {
                    setTextColor(Color.BLACK)
                    background = null
                }
                else -> {
                    setTextColor(Color.LTGRAY)
                    background = null
                }
            }

            setOnClickListener {
                selectedDate = date.clone() as Calendar
                if (!sameMonth(selectedDate, visibleMonth)) {
                    visibleMonth = (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                }
                renderScreen()
                cargarAnalisisDelDia(selectedDate)
            }
        }
    }

    private fun renderMealsForSelectedDate() {
        mealContainer.removeAllViews()
        val dateKey = dateKeyFormat.format(selectedDate.time)
        val meals = mealsByDate[dateKey].orEmpty()

        if (meals.isEmpty()) {
            mealContainer.addView(TextView(this).apply {
                text = "Sin registros para esta fecha"
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, 0)
                setTextColor(Color.GRAY)
                textSize = 14f
            })
            return
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val mealsByType = meals.groupBy { it.section }
        mealsByType.forEach { (mealType, typeMeals) ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(12)) }
                radius = dp(16).toFloat()
                cardElevation = 2f
                setCardBackgroundColor(Color.WHITE)
                strokeWidth = dp(1)
                strokeColor = Color.parseColor("#EEF2F7")
            }

            val list = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }

            list.addView(TextView(this).apply {
                text = mealType
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#F86E00"))
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
                text = meal.title
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                maxLines = 2
            })
            leftColumn.addView(TextView(this@HistorialActivity).apply {
                text = meal.time
                textSize = 12f
                setTextColor(Color.parseColor("#F86E00"))
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
                setTextColor(Color.BLACK)
            })
            rightColumn.addView(TextView(this@HistorialActivity).apply {
                text = "KCAL"
                textSize = 11f
                setTextColor(Color.GRAY)
                setPadding(0, dp(2), 0, 0)
            })

            addView(leftColumn)
            addView(rightColumn)
        }
    }

    private fun cargarAnalisisDelDia(date: Calendar) {
        val token = TokenManager.getToken(this) ?: return
        val dateStr = dateKeyFormat.format(date.time)
        lifecycleScope.launch {
            try {
                val response = repository.getAnalysesByDate(token, dateStr)
                if (response.isSuccessful && response.body() != null) {
                    val analyses = response.body()!!.data
                    val meals = analyses.mapNotNull { analysis ->
                        try {
                            // Obtener nombre de los platos, manejando null
                            val dishNames = analysis.foodsDetected
                                ?.joinToString(", ") { it.name }
                                ?: "Plato no especificado"

                            // Obtener hora local de createdAt (puede ser null)
                            val timeStr = analysis.createdAt?.let { formatToLocalTime(it) } ?: "N/A"

                            MealEntry(
                                section = analysis.rawModelResponse?.mealType?.replaceFirstChar { it.uppercase() } ?: "Comida",
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

    // Convierte el string ISO con offset (ej: 2026-04-22T23:34:37.000-03:00) a hora local HH:mm
    private fun formatToLocalTime(isoDateStr: String?): String {
        if (isoDateStr.isNullOrEmpty()) return "N/A"
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                val date = sdf.parse(isoDateStr)
                if (date != null) {
                    val localFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    return localFormatter.format(date)
                }
            } catch (e: Exception) { continue }
        }
        return try {
            Regex("T(\\d{2}:\\d{2})").find(isoDateStr)?.groupValues?.get(1) ?: "N/A"
        } catch (e: Exception) { "N/A" }
    }

    private fun showMonthPicker() {
        MaterialAlertDialogBuilder(this).setTitle("Mes").setItems(monthLabels) { _, i ->
            visibleMonth.set(Calendar.MONTH, i)
            renderScreen()
            cargarAnalisisDelDia(selectedDate)
        }.show()
    }

    private fun showYearPicker() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = Array(5) { (currentYear - 2 + it).toString() }
        MaterialAlertDialogBuilder(this).setTitle("Año").setItems(years) { _, i ->
            visibleMonth.set(Calendar.YEAR, years[i].toInt())
            renderScreen()
            cargarAnalisisDelDia(selectedDate)
        }.show()
    }

    private fun sameMonth(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    private fun sameDate(a: Calendar, b: Calendar) = sameMonth(a, b) && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
    private fun Calendar.normalize() { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun roundedBg(c: Int) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(10).toFloat(); setColor(c) }
}