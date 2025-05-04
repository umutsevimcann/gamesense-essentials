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

class SystemTray {
    companion object {
        private var tickRateOptionPaneIsOpen = false
        private var frame: JFrame? = null
        private val i18n = LanguageManager
        
        fun setup() {
            if (!SystemTray.isSupported()) {
                ErrorUtil.showErrorDialogAndExit("System is not supported.");
                return
            }
            
            // Dil tercihi
            i18n.currentLanguage = preferences.get("language", "en")
            
            // Tema yöneticisini başlat
            ThemeManager.initialize(preferences)
            
            val tray = SystemTray.getSystemTray()
            val trayIconImage = ImageIO.read(Main::class.java.classLoader.getResource("icon.png"))
            val trayIconWidth = TrayIcon(trayIconImage).size.width
            val icon = TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH))
            
            // Tepsiye tıklama ile panel açma
            icon.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON1) {
                        showControlPanel()
                    }
                }
            })
            
            // Menü oluşturma
            val menu = PopupMenu(i18n.translate("app_name"))
            val title = MenuItem(i18n.translate("app_name"))
            title.isEnabled = false
            
            val controlPanel = MenuItem(i18n.translate("control_panel"))
            controlPanel.addActionListener { showControlPanel() }
            
            val viewSettings = Menu(i18n.translate("settings"))
            
            viewSettings.add(createSettingMenuItem(i18n.translate("enable_clock"), "clock"))
            viewSettings.add(createSettingMenuItem(i18n.translate("enable_clock_icon"), "clockIcon"))
            viewSettings.add(createSettingMenuItem(i18n.translate("display_clock_periodically"), "clockPeriodically", false))
            viewSettings.add(createSettingMenuItem(i18n.translate("enable_volume_slider"), "volume"))
            viewSettings.add(createSettingMenuItem(i18n.translate("enable_song_info"), "songInfo"))
            viewSettings.add(createSettingMenuItem(i18n.translate("enable_song_icon"), "songIcon"))
            viewSettings.add(createSettingMenuItem(i18n.translate("flip_song_title"), "songInfoFlip", false))
            viewSettings.add(createSettingMenuItem(i18n.translate("display_song_separator"), "songSeparator", false))
            
            val themeMenuItem = CheckboxMenuItem(i18n.translate("dark_mode"))
            themeMenuItem.state = ThemeManager.isDarkMode
            themeMenuItem.addItemListener {
                val menuItem = it.source as CheckboxMenuItem
                ThemeManager.updateDarkMode(menuItem.state)
                if (frame != null && frame!!.isVisible) {
                    updateControlPanelTheme()
                }
            }
            
            val tickRate = MenuItem(i18n.translate("change_tick_rate"))
            tickRate.addActionListener { changeTickRate() }
            
            val help = Menu(i18n.translate("help"))
            val about = MenuItem(i18n.translate("about"))
            about.addActionListener { showAboutDialog() }
            val visitGithub = MenuItem(i18n.translate("visit_github"))
            visitGithub.addActionListener { openWebpage("https://github.com/mtricht/gamesense-essentials") }
            help.add(about)
            help.add(visitGithub)
            
            val exit = MenuItem(i18n.translate("exit"))
            exit.addActionListener { exitProcess(0) }
            
            listOf(
                title,
                controlPanel,
                viewSettings,
                MenuItem("-"), // ayırıcı
                tickRate,
                themeMenuItem,
                MenuItem("-"), // ayırıcı
                help,
                exit
            ).forEach(menu::add)
            
            icon.popupMenu = menu
            tray.add(icon)
        }

        private fun createSettingMenuItem(
            description: String,
            setting: String,
            default: Boolean = true
        ): CheckboxMenuItem {
            val settingMenuItem = CheckboxMenuItem(description)
            settingMenuItem.addItemListener {
                val menuItem = it.source as CheckboxMenuItem
                preferences.put(setting, menuItem.state.toString())
                if (setting == "clockIcon") {
                    Main.registerClockHandler(client!!)
                }
                if (setting == "songInfoFlip" || setting == "songIcon") {
                    Main.registerSongHandler(client!!)
                }
                if (setting == "songSeparator") {
                    timer.cancel()
                    timer.purge()
                    timer = java.util.Timer()
                    Main.startTimer()
                }
            }
            settingMenuItem.state = preferences.get(setting, default.toString()).toBoolean()
            return settingMenuItem
        }

        private fun changeTickRate() {
            if (tickRateOptionPaneIsOpen) {
                return
            }
            tickRateOptionPaneIsOpen = true
            val frame = JFrame()
            frame.isAlwaysOnTop = true
            val newTickRate = JOptionPane.showInputDialog(
                frame,
                i18n.translate("tick_rate_ms") + " " + i18n.translate("invalid_number"),
                Tick.tickRateInMs()
            )
            tickRateOptionPaneIsOpen = false
            if (newTickRate == null) {
                return
            }
            try {
                val newTickRateInt = newTickRate.trim().toInt()
                if (newTickRateInt <= 0) {
                    return
                }
                timer.cancel()
                timer.purge()
                preferences.put("tickRate", newTickRate.trim())
                Tick.refreshCache()
                timer = java.util.Timer()
                Main.startTimer()
            } catch (_: Exception) {
            }
        }
        
        private fun showControlPanel() {
            if (frame != null && frame!!.isVisible) {
                frame!!.toFront()
                return
            }
            
            frame = JFrame(i18n.translate("app_name") + " " + i18n.translate("control_panel"))
            frame!!.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            
            // Apply Win11 theme
            Win11Theme.applyTheme(ThemeManager.isDarkMode)
            
            val panel = JPanel()
            panel.layout = BorderLayout()
            
            // Header
            val headerPanel = JPanel()
            headerPanel.layout = BorderLayout()
            headerPanel.border = EmptyBorder(15, 20, 15, 20)
            
            val titleLabel = JLabel(i18n.translate("app_name"))
            titleLabel.font = Font("Segoe UI Variable", Font.BOLD, 20)
            headerPanel.add(titleLabel, BorderLayout.WEST)
            
            // Main control panel
            val controlPanel = JPanel()
            controlPanel.layout = BoxLayout(controlPanel, BoxLayout.Y_AXIS)
            controlPanel.border = EmptyBorder(0, 20, 20, 20)
            
            // Display Settings
            val displayPanel = createModernSectionPanel(i18n.translate("display_settings"))
            
            displayPanel.add(createModernToggleControl(i18n.translate("enable_clock"), "clock", true))
            displayPanel.add(createModernToggleControl(i18n.translate("enable_clock_icon"), "clockIcon", true))
            displayPanel.add(createModernToggleControl(i18n.translate("display_clock_periodically"), "clockPeriodically", false))
            displayPanel.add(createModernToggleControl(i18n.translate("enable_volume_slider"), "volume", true))
            
            // Music Settings
            val musicPanel = createModernSectionPanel(i18n.translate("music_settings"))
            
            musicPanel.add(createModernToggleControl(i18n.translate("enable_song_info"), "songInfo", true))
            musicPanel.add(createModernToggleControl(i18n.translate("enable_song_icon"), "songIcon", true))
            musicPanel.add(createModernToggleControl(i18n.translate("flip_song_title"), "songInfoFlip", false))
            musicPanel.add(createModernToggleControl(i18n.translate("display_song_separator"), "songSeparator", false))
            
            // Performance Settings
            val performancePanel = createModernSectionPanel(i18n.translate("performance_settings"))
            
            val tickRateLabel = JLabel(i18n.translate("tick_rate_ms"))
            tickRateLabel.font = Font("Segoe UI Variable", Font.PLAIN, 12)
            
            val tickRateField = JTextField(Tick.tickRateInMs().toString(), 5)
            
            val tickRateButton = JButton(i18n.translate("apply"))
            Win11Theme.styleButton(tickRateButton, true)
            
            tickRateButton.addActionListener {
                try {
                    val newRate = tickRateField.text.trim().toInt()
                    if (newRate > 0) {
                        timer.cancel()
                        timer.purge()
                        preferences.put("tickRate", newRate.toString())
                        Tick.refreshCache()
                        timer = java.util.Timer()
                        Main.startTimer()
                    }
                } catch (_: Exception) {
                    JOptionPane.showMessageDialog(frame, i18n.translate("invalid_number"), i18n.translate("error"), JOptionPane.ERROR_MESSAGE)
                }
            }
            
            val tickRatePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            tickRatePanel.add(tickRateLabel)
            tickRatePanel.add(tickRateField)
            tickRatePanel.add(tickRateButton)
            performancePanel.add(tickRatePanel)
            
            // Startup setting
            val startupToggle = JCheckBox(i18n.translate("autostart"))
            Win11Theme.styleCheckComponent(startupToggle)
            startupToggle.isSelected = StartupManager.isStartupEnabled()
            startupToggle.addActionListener {
                val isChecked = startupToggle.isSelected
                if (isChecked) {
                    StartupManager.enableStartup()
                } else {
                    StartupManager.disableStartup()
                }
            }
            performancePanel.add(startupToggle)
            
            // Theme & Appearance Settings
            val themePanel = createModernSectionPanel(i18n.translate("appearance"))
            
            // Language selection
            val languagePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val languageLabel = JLabel(i18n.translate("language") + ": ")
            languageLabel.font = Font("Segoe UI Variable", Font.PLAIN, 12)
            
            val languageCombo = JComboBox<String>()
            
            for ((code, name) in i18n.supportedLanguages) {
                languageCombo.addItem(name)
                if (code == i18n.currentLanguage) {
                    languageCombo.selectedItem = name
                }
            }
            
            languageCombo.addActionListener {
                val selectedName = languageCombo.selectedItem as String
                val langCode = i18n.supportedLanguages.entries.find { it.value == selectedName }?.key ?: "en"
                if (langCode != i18n.currentLanguage) {
                    i18n.setLanguage(langCode)
                    preferences.put("language", langCode)
                    
                    // Reload panel when language changes
                    frame?.dispose()
                    showControlPanel()
                }
            }
            
            languagePanel.add(languageLabel)
            languagePanel.add(languageCombo)
            themePanel.add(languagePanel)
            
            // Theme selection
            val themeChoicePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val themeChoiceLabel = JLabel(i18n.translate("color_theme") + ": ")
            themeChoiceLabel.font = Font("Segoe UI Variable", Font.PLAIN, 12)
            
            val themeCombo = JComboBox<String>()
            
            // Predefined themes
            val themeNames = mapOf(
                "light" to "Light",
                "dark" to "Dark",
                "blue" to "Blue",
                "dark_blue" to "Dark Blue",
                "green" to "Green",
                "dark_green" to "Dark Green",
                "custom" to "Custom"
            )
            
            for ((key, name) in themeNames) {
                themeCombo.addItem(name)
                if (key == ThemeManager.currentThemeKey) {
                    themeCombo.selectedItem = name
                }
            }
            
            themeCombo.addActionListener {
                val selectedName = themeCombo.selectedItem as String
                val themeKey = themeNames.entries.find { it.value == selectedName }?.key ?: "light"
                if (themeKey != ThemeManager.currentThemeKey) {
                    ThemeManager.setTheme(themeKey)
                    updateControlPanelTheme()
                }
            }
            
            themeChoicePanel.add(themeChoiceLabel)
            themeChoicePanel.add(themeCombo)
            themePanel.add(themeChoicePanel)
            
            // Dark mode
            val darkModeToggle = JCheckBox(i18n.translate("dark_mode"))
            Win11Theme.styleCheckComponent(darkModeToggle)
            darkModeToggle.isSelected = ThemeManager.isDarkMode
            darkModeToggle.addActionListener {
                ThemeManager.updateDarkMode(darkModeToggle.isSelected)
                updateControlPanelTheme()
            }
            themePanel.add(darkModeToggle)
            
            // Customize colors button
            val customizeButton = JButton(i18n.translate("customize_colors"))
            Win11Theme.styleButton(customizeButton)
            customizeButton.addActionListener {
                showColorCustomizationDialog()
            }
            
            val customizePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            customizePanel.add(customizeButton)
            themePanel.add(customizePanel)
            
            // Add all sections to panel
            controlPanel.add(displayPanel)
            controlPanel.add(Box.createRigidArea(Dimension(0, 15)))
            controlPanel.add(musicPanel)
            controlPanel.add(Box.createRigidArea(Dimension(0, 15)))
            controlPanel.add(performancePanel)
            controlPanel.add(Box.createRigidArea(Dimension(0, 15)))
            controlPanel.add(themePanel)
            
            // Scrollable panel
            val scrollPane = JScrollPane(controlPanel)
            scrollPane.border = null
            scrollPane.verticalScrollBar.unitIncrement = 16
            
            // Put everything together
            panel.add(headerPanel, BorderLayout.NORTH)
            panel.add(scrollPane, BorderLayout.CENTER)
            
            // Additional settings for modern look
            Win11Theme.stylePanel(panel)
            
            // Apply theme
            frame!!.contentPane = panel
            updateControlPanelTheme()
            
            frame!!.pack()
            frame!!.setSize(450, 650)
            frame!!.setLocationRelativeTo(null)
            frame!!.isVisible = true
        }
        
        // Creates a modern Windows 11 style section panel
        private fun createModernSectionPanel(title: String): JPanel {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = EmptyBorder(10, 0, 10, 0)
            
            val titleLabel = JLabel(title)
            titleLabel.font = Font("Segoe UI Variable", Font.BOLD, 14)
            titleLabel.alignmentX = Component.LEFT_ALIGNMENT
            titleLabel.border = EmptyBorder(0, 0, 10, 0)
            
            panel.add(titleLabel)
            
            return panel
        }
        
        // Creates a modern Windows 11 style toggle control
        private fun createModernToggleControl(label: String, setting: String, default: Boolean): JPanel {
            val panel = JPanel(FlowLayout(FlowLayout.LEFT))
            val toggle = JCheckBox(label)
            Win11Theme.styleCheckComponent(toggle)
            toggle.isSelected = preferences.get(setting, default.toString()).toBoolean()
            toggle.addActionListener {
                preferences.put(setting, toggle.isSelected.toString())
                if (setting == "clockIcon") {
                    Main.registerClockHandler(client!!)
                }
                if (setting == "songInfoFlip" || setting == "songIcon") {
                    Main.registerSongHandler(client!!)
                }
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
        
        private fun updateControlPanelTheme() {
            if (frame == null) return
            
            // Apply Windows 11 theme
            Win11Theme.applyTheme(ThemeManager.isDarkMode)
            
            // Also apply color theme
            val theme = ThemeManager.getCurrentTheme()
            updateComponentTheme(frame!!.contentPane, theme)
            
            // Update UI
            SwingUtilities.updateComponentTreeUI(frame)
        }
        
        private fun updateComponentTheme(component: Component, theme: Map<String, Color>) {
            component.background = theme["background"]
            
            if (component is JPanel || component is JScrollPane) {
                component.background = theme["background"]
            }
            
            if (component is JLabel || component is JCheckBox || component is JButton) {
                component.foreground = theme["foreground"]
            }
            
            if (component is JCheckBox) {
                component.background = theme["background"]
            }
            
            if (component is Container) {
                for (child in component.components) {
                    updateComponentTheme(child, theme)
                }
            }
        }
        
        private fun showColorCustomizationDialog() {
            val dialog = JDialog(frame, i18n.translate("customize_colors"), true)
            dialog.layout = BorderLayout()
            
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Theme title
            val themeTitle = JLabel(if (ThemeManager.isDarkMode) "Dark Theme Colors" else "Light Theme Colors")
            themeTitle.font = Font("Segoe UI Variable", Font.BOLD, 14)
            themeTitle.alignmentX = Component.LEFT_ALIGNMENT
            themeTitle.border = EmptyBorder(0, 0, 10, 0)
            panel.add(themeTitle)
            
            // Color selections
            val colorKeys = mapOf(
                "background" to i18n.translate("background_color"),
                "foreground" to i18n.translate("foreground_color"),
                "highlight" to i18n.translate("highlight_color")
            )
            
            val colorButtons = mutableMapOf<String, JButton>()
            
            for ((key, label) in colorKeys) {
                val rowPanel = JPanel(FlowLayout(FlowLayout.LEFT))
                rowPanel.alignmentX = Component.LEFT_ALIGNMENT
                
                val colorLabel = JLabel("$label: ")
                val currentColor = if (ThemeManager.isDarkMode) 
                    ThemeManager.customDarkTheme[key] else ThemeManager.customLightTheme[key]
                
                val colorButton = JButton("    ")
                colorButton.background = currentColor
                colorButton.addActionListener {
                    val chooser = JColorChooser(colorButton.background)
                    val dialog = JDialog(frame, "Choose Color", true)
                    dialog.contentPane.add(chooser)
                    dialog.pack()
                    
                    val okButton = JButton(i18n.translate("save"))
                    okButton.addActionListener {
                        val newColor = chooser.color
                        colorButton.background = newColor
                        ThemeManager.saveCustomColor(ThemeManager.isDarkMode, key, newColor)
                        if (ThemeManager.currentThemeKey == "custom") {
                            updateControlPanelTheme()
                        }
                        dialog.dispose()
                    }
                    
                    val cancelButton = JButton(i18n.translate("cancel"))
                    cancelButton.addActionListener { dialog.dispose() }
                    
                    val buttonPanel = JPanel()
                    buttonPanel.add(okButton)
                    buttonPanel.add(cancelButton)
                    dialog.contentPane.add(buttonPanel, BorderLayout.SOUTH)
                    
                    dialog.setLocationRelativeTo(frame)
                    dialog.isVisible = true
                }
                
                colorButtons[key] = colorButton
                rowPanel.add(colorLabel)
                rowPanel.add(colorButton)
                panel.add(rowPanel)
                panel.add(Box.createRigidArea(Dimension(0, 5)))
            }
            
            // Buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            buttonPanel.alignmentX = Component.LEFT_ALIGNMENT
            
            val resetButton = JButton(i18n.translate("reset_to_default"))
            resetButton.addActionListener {
                ThemeManager.resetCustomTheme(ThemeManager.isDarkMode)
                
                // Update button colors
                val theme = if (ThemeManager.isDarkMode) ThemeManager.customDarkTheme else ThemeManager.customLightTheme
                for ((key, button) in colorButtons) {
                    button.background = theme[key]
                }
                
                if (ThemeManager.currentThemeKey == "custom") {
                    updateControlPanelTheme()
                }
            }
            
            val saveButton = JButton(i18n.translate("save"))
            saveButton.addActionListener {
                // Switch to custom theme
                ThemeManager.setTheme("custom")
                updateControlPanelTheme()
                dialog.dispose()
            }
            
            val cancelButton = JButton(i18n.translate("cancel"))
            cancelButton.addActionListener { dialog.dispose() }
            
            buttonPanel.add(resetButton)
            buttonPanel.add(saveButton)
            buttonPanel.add(cancelButton)
            
            dialog.add(panel, BorderLayout.CENTER)
            dialog.add(buttonPanel, BorderLayout.SOUTH)
            
            dialog.pack()
            dialog.setSize(350, 250)
            dialog.setLocationRelativeTo(frame)
            dialog.isVisible = true
        }
        
        private fun showAboutDialog() {
            val message = i18n.translate("about_message")
            
            JOptionPane.showMessageDialog(
                null,
                message,
                i18n.translate("about") + " " + i18n.translate("app_name"),
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
    }
}
