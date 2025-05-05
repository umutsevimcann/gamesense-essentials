package eu.tricht.gamesense

import eu.tricht.gamesense.i18n.LanguageManager
import eu.tricht.gamesense.theme.ThemeManager
import eu.tricht.gamesense.util.StartupManager
import java.awt.*
import java.awt.SystemTray
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.colorchooser.ColorSelectionModel
import java.net.URI
import kotlin.system.exitProcess
import eu.tricht.gamesense.timer
import eu.tricht.gamesense.client
import eu.tricht.gamesense.preferences
import eu.tricht.gamesense.theme.Win11Theme
import javax.swing.border.LineBorder
import javax.swing.border.CompoundBorder

class SystemTray {
    companion object {
        private var tickRateOptionPaneIsOpen = false
        private var frame: JFrame? = null
        private val i18n = LanguageManager
        private var trayIcon: TrayIcon? = null // Tray ikonu referansı

        fun setup() {
            if (!SystemTray.isSupported()) {
                ErrorUtil.showErrorDialogAndExit("System is not supported.");
                return
            }
            
            // Varsayılan dil her zaman İngilizce
            // Default language is always English
            i18n.currentLanguage = "en"
            val savedLang = preferences.get("language", "en")
            if (savedLang != "en") {
                i18n.setLanguage(savedLang)
            }
            
            // Tema yöneticisini başlat
            // Initialize theme manager
            ThemeManager.initialize(preferences)
            
            val tray = SystemTray.getSystemTray()
            // Eski tray ikonu varsa kaldır
            // Remove old tray icon if exists
            if (trayIcon != null) {
                try { tray.remove(trayIcon) } catch (_: Exception) {}
            }
            val trayIconImage = ImageIO.read(Main::class.java.classLoader.getResource("icon.png"))
            val trayIconWidth = TrayIcon(trayIconImage).size.width
            val icon = TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH))
            trayIcon = icon
            
            // Swing JPopupMenu ile özel menü
            // Custom menu with Swing JPopupMenu
            icon.addMouseListener(object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON3) {
                        showSwingTrayMenu(e)
                    } else if (e.button == MouseEvent.BUTTON1) {
                        showControlPanel()
                    }
                }
            })
            tray.add(icon)
        }

        // Swing tabanlı tray menüsü
        // Swing-based tray menu
        private fun showSwingTrayMenu(e: MouseEvent) {
            val menu = JPopupMenu()
            menu.isLightWeightPopupEnabled = false
            
            // Görünmez JFrame oluştur
            // Create invisible JFrame
            val frame = JFrame()
            frame.isUndecorated = true
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame.setSize(1, 1)
            frame.isLocationByPlatform = true
            frame.isAlwaysOnTop = true
            frame.opacity = 0f
            frame.isVisible = true
            
            // Başlık
            // Title
            val title = JMenuItem(i18n.translate("app_name"))
            title.isEnabled = false
            menu.add(title)
            
            // Ayarlar
            // Settings
            val settings = JMenu(i18n.translate("settings"))
            settings.add(createSwingSettingMenuItem(i18n.translate("enable_clock"), "clock"))
            settings.add(createSwingSettingMenuItem(i18n.translate("enable_clock_icon"), "clockIcon"))
            settings.add(createSwingSettingMenuItem(i18n.translate("display_clock_periodically"), "clockPeriodically", false))
            settings.add(createSwingSettingMenuItem(i18n.translate("enable_volume_slider"), "volume"))
            settings.add(createSwingSettingMenuItem(i18n.translate("enable_song_info"), "songInfo"))
            settings.add(createSwingSettingMenuItem(i18n.translate("enable_song_icon"), "songIcon"))
            settings.add(createSwingSettingMenuItem(i18n.translate("flip_song_title"), "songInfoFlip", false))
            settings.add(createSwingSettingMenuItem(i18n.translate("display_song_separator"), "songSeparator", false))
            // Saat seçenekleri
            val clockOptions = JMenu(i18n.translate("clock_options"))
            val showDate = JRadioButtonMenuItem(i18n.translate("show_date"), preferences.get("clockShowDate", "false").toBoolean())
            val showTime = JRadioButtonMenuItem(i18n.translate("show_time"), !preferences.get("clockShowDate", "false").toBoolean())
            val dateTimeGroup = ButtonGroup()
            dateTimeGroup.add(showDate)
            dateTimeGroup.add(showTime)
            showDate.addActionListener {
                preferences.put("clockShowDate", "true")
                showTime.isSelected = false
            }
            showTime.addActionListener {
                preferences.put("clockShowDate", "false")
                showDate.isSelected = false
            }
            clockOptions.add(showTime)
            clockOptions.add(showDate)
            val format12 = JRadioButtonMenuItem(i18n.translate("clock_12h"), preferences.get("clock12h", "false").toBoolean())
            val format24 = JRadioButtonMenuItem(i18n.translate("clock_24h"), !preferences.get("clock12h", "false").toBoolean())
            val group = ButtonGroup()
            group.add(format12)
            group.add(format24)
            format12.addActionListener {
                preferences.put("clock12h", "true")
                format24.isSelected = false
            }
            format24.addActionListener {
                preferences.put("clock12h", "false")
                format12.isSelected = false
            }
            clockOptions.add(format12)
            clockOptions.add(format24)
            val showSeconds = JCheckBoxMenuItem(i18n.translate("show_seconds"), preferences.get("clockShowSeconds", "false").toBoolean())
            showSeconds.addActionListener {
                preferences.put("clockShowSeconds", showSeconds.state.toString())
            }
            clockOptions.add(showSeconds)
            settings.add(clockOptions)
            menu.add(settings)
            
            // Dil
            // Language
            val languageMenu = JMenu(i18n.translate("language"))
            for ((code, name) in i18n.supportedLanguages) {
                val langItem = JRadioButtonMenuItem(name, code == i18n.currentLanguage)
                langItem.addActionListener {
                    if (code != i18n.currentLanguage) {
                        i18n.setLanguage(code)
                        preferences.put("language", code)
                        menu.isVisible = false
                        setup()
                    }
                }
                languageMenu.add(langItem)
            }
            menu.add(languageMenu)
            
            // Tema
            // Theme
            val themeMenu = JMenu(i18n.translate("color_theme"))
            val themeKeys = ThemeManager.predefinedThemes.keys.filter { it != "custom" }.toList()
            for (key in themeKeys) {
                val name = when (key) {
                    "light" -> i18n.translate("light_theme")
                    "dark" -> i18n.translate("dark_theme")
                    "win11" -> "Windows 11"
                    "dark_blue" -> i18n.translate("dark_blue_theme")
                    "dark_green" -> i18n.translate("dark_green_theme")
                    "dark_turquoise" -> i18n.translate("dark_turquoise_theme")
                    "turquoise" -> i18n.translate("turquoise_theme")
                    else -> key.replace("_", " ").capitalize()
                }
                val themeItem = JRadioButtonMenuItem(name, key == ThemeManager.currentThemeKey)
                themeItem.addActionListener {
                    if (key != ThemeManager.currentThemeKey) {
                        ThemeManager.setTheme(key)
                        if (key == "dark" || key.startsWith("dark_")) ThemeManager.updateDarkMode(true)
                        if (key == "light" || (key != "dark" && !key.startsWith("dark_"))) ThemeManager.updateDarkMode(false)
                        menu.isVisible = false
                        setup()
                    }
                }
                themeMenu.add(themeItem)
            }
            menu.add(themeMenu)
            
            // Yeniden Başlat
            // Restart
            val restartItem = JMenuItem(i18n.translate("restart"))
            restartItem.addActionListener {
                try {
                    val separator = System.getProperty("file.separator")
                    val javaBin = System.getProperty("java.home") + separator + "bin" + separator + "java"
                    val jarPath = Main::class.java.protectionDomain.codeSource.location.toURI().path
                    val command = arrayOf(javaBin, "-jar", jarPath)
                    Runtime.getRuntime().exec(command)
                    exitProcess(0)
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(null, "Restart failed. Please restart the application manually.\n" + e.message, "Restart Error", JOptionPane.ERROR_MESSAGE)
                }
            }
            menu.add(restartItem)
            
            // Güncellemeleri Kontrol Et
            // Check for Updates
            val updateItem = JMenuItem(i18n.translate("check_updates"))
            updateItem.addActionListener {
                JOptionPane.showMessageDialog(null, i18n.translate("update_message"), i18n.translate("check_updates"), JOptionPane.INFORMATION_MESSAGE)
            }
            menu.add(updateItem)
            
            // Ayraç
            // Separator
            menu.addSeparator()
            
            // Hakkında
            // About
            val about = JMenuItem(i18n.translate("about"))
            about.addActionListener { showAboutDialog() }
            menu.add(about)
            
            // Çıkış
            // Exit
            val exit = JMenuItem(i18n.translate("exit"))
            exit.addActionListener { exitProcess(0) }
            menu.add(exit)
            
            // Tema uygula
            // Apply theme
            applySwingMenuTheme(menu)
            
            // Menü konumunu belirle ve göster
            // Set menu location and show
            val mouseLocation = MouseInfo.getPointerInfo().location
            menu.show(frame, 0, 0)
            menu.location = mouseLocation
            
            // Menü kapandığında frame'i kapat
            // Dispose frame when menu closes
            menu.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) {}
                override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) { frame.dispose() }
                override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) { frame.dispose() }
            })
        }

        // Swing menüde ayar checkbox'ı
        // Setting checkbox in Swing menu
        private fun createSwingSettingMenuItem(description: String, setting: String, default: Boolean = true): JCheckBoxMenuItem {
            val item = JCheckBoxMenuItem(description)
            item.state = preferences.get(setting, default.toString()).toBoolean()
            item.addActionListener {
                preferences.put(setting, item.state.toString())
                if (setting == "clockIcon") Main.registerClockHandler(client!!)
                if (setting == "songInfoFlip" || setting == "songIcon") Main.registerSongHandler(client!!)
                if (setting == "songSeparator") {
                    timer.cancel()
                    timer.purge()
                    timer = java.util.Timer()
                    Main.startTimer()
                }
            }
            return item
        }

        // Swing menüye tema uygula
        // Apply theme to Swing menu
        private fun applySwingMenuTheme(menu: JPopupMenu) {
            val theme = ThemeManager.getCurrentTheme()
            menu.background = theme["background"]
            menu.foreground = theme["foreground"]
            for (component in menu.components) {
                if (component is JMenuItem) {
                    component.background = theme["background"]
                    component.foreground = theme["foreground"]
                }
                if (component is JMenu) {
                    component.background = theme["background"]
                    component.foreground = theme["foreground"]
                }
                if (component is JCheckBoxMenuItem || component is JRadioButtonMenuItem) {
                    component.background = theme["background"]
                    component.foreground = theme["foreground"]
                }
            }
        }

        private fun showControlPanel() {
            // Kaldırıldı
        }
        
        private fun showColorCustomizationDialog() {
            // Kaldırıldı
        }
        
        private fun showAboutDialog() {
            val version = Main::class.java.`package`.implementationVersion ?: "1.0.0"
            val message = "GameSense Essentials\n" +
                "\nVersion: $version" +
                "\n\nDeveloper: umutsevimcann(Umut S.) (https://github.com/umutsevimcann)" +
                "\nOriginal Creator: mtricht (https://github.com/mtricht)" +
                "\n\nGameSense Essentials is an open-source utility for SteelSeries GG. " +
                "It adds clock, volume slider, and music information to your SteelSeries OLED screen.\n" +
                "\n\nFor updates, source code, and more information, visit:\nhttps://github.com/umutsevimcann/gamesense-essentials" +
                "\n\nSpecial thanks to all contributors and the open-source community."
            JOptionPane.showMessageDialog(
                null,
                message,
                "About GameSense Essentials",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        
        private fun openWebpage(url: String) {
            try {
                Desktop.getDesktop().browse(URI(url))
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    null,
                    "Could not open browser: ${e.message}",
                    i18n.translate("error"),
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        // Modern görünümlü toggle kontrol fonksiyonu
        // Modern style toggle control function
        private fun createModernToggleControl(label: String, setting: String, default: Boolean): JPanel {
            val panel = JPanel(FlowLayout(FlowLayout.LEFT))
            val toggle = JCheckBox(label)
            toggle.font = Font("Dialog", Font.PLAIN, 13)
            Win11Theme.styleCheckComponent(toggle)
            toggle.isSelected = preferences.get(setting, default.toString()).toBoolean()
            toggle.addActionListener {
                preferences.put(setting, toggle.isSelected.toString())
                if (setting == "clockIcon") Main.registerClockHandler(client!!)
                if (setting == "songInfoFlip" || setting == "songIcon") Main.registerSongHandler(client!!)
                if (setting == "songSeparator") {
                timer.cancel()
                timer.purge()
                    timer = java.util.Timer()
                Main.startTimer()
                }
            }
            panel.add(toggle)
            return panel
        }
    }
}
