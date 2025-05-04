package eu.tricht.gamesense.util

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object StartupManager {
    // Windows başlangıç klasörü yolu
    private val startupFolder: String by lazy {
        System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup"
    }
    
    // Uygulama başlatma kısayolu adı
    private const val SHORTCUT_NAME = "GameSense Essentials.lnk"
    
    // Kısayol dosyasının tam yolu
    private val shortcutPath: String by lazy {
        startupFolder + File.separator + SHORTCUT_NAME
    }
    
    /**
     * Uygulamanın Windows başlangıcında otomatik başlatılıp başlatılmadığını kontrol eder
     */
    fun isStartupEnabled(): Boolean {
        val shortcutFile = File(shortcutPath)
        return shortcutFile.exists()
    }
    
    /**
     * Uygulamayı Windows başlangıcında otomatik başlatacak şekilde ayarlar
     */
    fun enableStartup() {
        try {
            // Şu anki jar dosyasının yolunu al
            val jarPath = getJarPath()
            
            // PowerShell ile kısayol oluştur (Windows scriptlet kullanarak)
            val command = """
                powershell.exe -Command "${'$'}WshShell = New-Object -ComObject WScript.Shell; ${'$'}Shortcut = ${'$'}WshShell.CreateShortcut('$shortcutPath'); ${'$'}Shortcut.TargetPath = '$jarPath'; ${'$'}Shortcut.Save()"
            """.trimIndent()
            
            val process = Runtime.getRuntime().exec(command)
            process.waitFor()
            
            if (process.exitValue() != 0) {
                throw IOException("Startup shortcut creation failed with exit code: ${process.exitValue()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Uygulamayı Windows başlangıcında otomatik başlatma özelliğini devre dışı bırakır
     */
    fun disableStartup() {
        try {
            val shortcutFile = File(shortcutPath)
            if (shortcutFile.exists()) {
                shortcutFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Şu anda çalışan jar dosyasının tam yolunu döndürür
     */
    private fun getJarPath(): String {
        val location = StartupManager::class.java.protectionDomain.codeSource.location
        return File(location.toURI()).absolutePath
    }
} 