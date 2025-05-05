package eu.tricht.gamesense.theme

import java.awt.Color
import java.util.prefs.Preferences

object ThemeManager {
    private var preferences: Preferences? = null
    
    private val defaultLightTheme = mapOf(
        "background" to Color(245, 245, 245),
        "foreground" to Color(33, 33, 33),
        "highlight" to Color(0, 120, 215),
        "secondary" to Color(180, 180, 180)
    )
    
    private val defaultDarkTheme = mapOf(
        "background" to Color(43, 43, 43),
        "foreground" to Color(240, 240, 240),
        "highlight" to Color(0, 175, 255),
        "secondary" to Color(80, 80, 80)
    )
    
    // Önceden tanımlanmış tema seçenekleri
    val predefinedThemes = mapOf(
        "light" to defaultLightTheme,
        "dark" to defaultDarkTheme,
        "win11" to mapOf(
            "background" to Color(243, 243, 247),
            "foreground" to Color(31, 31, 31),
            "highlight" to Color(0, 120, 212),
            "secondary" to Color(222, 228, 239)
        ),
        "purple" to mapOf(
            "background" to Color(240, 235, 250),
            "foreground" to Color(60, 30, 90),
            "highlight" to Color(120, 60, 200),
            "secondary" to Color(200, 180, 230)
        ),
        "orange" to mapOf(
            "background" to Color(255, 245, 230),
            "foreground" to Color(120, 60, 0),
            "highlight" to Color(255, 140, 0),
            "secondary" to Color(255, 210, 150)
        ),
        "red" to mapOf(
            "background" to Color(250, 230, 230),
            "foreground" to Color(120, 0, 0),
            "highlight" to Color(220, 40, 40),
            "secondary" to Color(255, 180, 180)
        ),
        "gray" to mapOf(
            "background" to Color(230, 230, 230),
            "foreground" to Color(60, 60, 60),
            "highlight" to Color(120, 120, 120),
            "secondary" to Color(180, 180, 180)
        ),
        "blue" to mapOf(
            "background" to Color(225, 235, 245),
            "foreground" to Color(30, 30, 90),
            "highlight" to Color(0, 102, 204),
            "secondary" to Color(150, 180, 210)
        ),
        "dark_blue" to mapOf(
            "background" to Color(25, 35, 65),
            "foreground" to Color(220, 230, 255),
            "highlight" to Color(70, 150, 255),
            "secondary" to Color(80, 100, 140)
        ),
        "green" to mapOf(
            "background" to Color(235, 245, 235),
            "foreground" to Color(20, 70, 20),
            "highlight" to Color(40, 150, 40),
            "secondary" to Color(150, 200, 150)
        ),
        "dark_green" to mapOf(
            "background" to Color(25, 45, 25),
            "foreground" to Color(220, 250, 220),
            "highlight" to Color(80, 200, 80),
            "secondary" to Color(60, 120, 60)
        ),
        "turquoise" to mapOf(
            "background" to Color(230, 250, 250),
            "foreground" to Color(20, 80, 90),
            "highlight" to Color(0, 180, 170),
            "secondary" to Color(150, 230, 230)
        ),
        "dark_turquoise" to mapOf(
            "background" to Color(20, 40, 45),
            "foreground" to Color(220, 250, 250),
            "highlight" to Color(0, 210, 200),
            "secondary" to Color(60, 130, 140)
        )
    )
    
    var currentThemeKey = "light"
    var isDarkMode = false
    
    // Özelleştirilmiş tema renkleri
    var customLightTheme = defaultLightTheme.toMutableMap()
    var customDarkTheme = defaultDarkTheme.toMutableMap()
    
    fun initialize(prefs: Preferences) {
        preferences = prefs
        detectSystemTheme() // Sistem temasını algıla ve uygula
        isDarkMode = preferences!!.getBoolean("darkMode", isDarkMode)
        currentThemeKey = preferences!!.get("themeKey", if (isDarkMode) "dark" else "light") ?: "light"
        
        // Özelleştirilmiş renkleri yükle
        loadCustomColors()
    }
    
    // Windows sistem temasını algıla ve uygula
    fun detectSystemTheme() {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            reader.close()
            process.waitFor()
            val isLight = output.contains("REG_DWORD") && output.trim().endsWith("0x1")
            isDarkMode = !isLight
        } catch (e: Exception) {
            // Hata olursa varsayılan açık modda kal
            isDarkMode = false
        }
    }
    
    fun getCurrentTheme(): Map<String, Color> {
        return when {
            isDarkMode && currentThemeKey == "custom" -> customDarkTheme
            !isDarkMode && currentThemeKey == "custom" -> customLightTheme
            isDarkMode && predefinedThemes.containsKey(currentThemeKey) -> predefinedThemes[currentThemeKey]!!
            !isDarkMode && predefinedThemes.containsKey(currentThemeKey) -> predefinedThemes[currentThemeKey]!!
            isDarkMode -> defaultDarkTheme
            else -> defaultLightTheme
        }
    }
    
    fun setTheme(themeKey: String) {
        if (predefinedThemes.containsKey(themeKey) || themeKey == "custom") {
            currentThemeKey = themeKey
            preferences?.put("themeKey", themeKey)
        }
    }
    
    fun updateDarkMode(darkMode: Boolean) {
        isDarkMode = darkMode
        preferences?.putBoolean("darkMode", darkMode)
    }
    
    fun saveCustomColor(isDark: Boolean, colorKey: String, color: Color) {
        if (isDark) {
            customDarkTheme[colorKey] = color
        } else {
            customLightTheme[colorKey] = color
        }
        
        // Tercihlere kaydet
        saveCustomColors()
    }
    
    fun resetCustomTheme(isDark: Boolean) {
        if (isDark) {
            customDarkTheme = defaultDarkTheme.toMutableMap()
        } else {
            customLightTheme = defaultLightTheme.toMutableMap()
        }
        saveCustomColors()
    }
    
    private fun saveCustomColors() {
        if (preferences == null) return
        
        // Özelleştirilmiş açık tema
        for ((key, color) in customLightTheme) {
            preferences!!.putInt("customLight_${key}_r", color.red)
            preferences!!.putInt("customLight_${key}_g", color.green)
            preferences!!.putInt("customLight_${key}_b", color.blue)
        }
        
        // Özelleştirilmiş koyu tema
        for ((key, color) in customDarkTheme) {
            preferences!!.putInt("customDark_${key}_r", color.red)
            preferences!!.putInt("customDark_${key}_g", color.green)
            preferences!!.putInt("customDark_${key}_b", color.blue)
        }
    }
    
    private fun loadCustomColors() {
        if (preferences == null) return
        
        // Özelleştirilmiş açık tema
        for (key in defaultLightTheme.keys) {
            val r = preferences!!.getInt("customLight_${key}_r", defaultLightTheme[key]!!.red)
            val g = preferences!!.getInt("customLight_${key}_g", defaultLightTheme[key]!!.green)
            val b = preferences!!.getInt("customLight_${key}_b", defaultLightTheme[key]!!.blue)
            customLightTheme[key] = Color(r, g, b)
        }
        
        // Özelleştirilmiş koyu tema
        for (key in defaultDarkTheme.keys) {
            val r = preferences!!.getInt("customDark_${key}_r", defaultDarkTheme[key]!!.red)
            val g = preferences!!.getInt("customDark_${key}_g", defaultDarkTheme[key]!!.green)
            val b = preferences!!.getInt("customDark_${key}_b", defaultDarkTheme[key]!!.blue)
            customDarkTheme[key] = Color(r, g, b)
        }
    }
} 