package eu.tricht.gamesense.theme

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

/**
 * Manages theme and component styles in Windows 11 style
 */
object Win11Theme {
    
    // Windows 11 theme colors
    private val win11Colors = mapOf(
        "light" to mapOf(
            "background" to Color(243, 243, 243),
            "foreground" to Color(33, 33, 33),
            "accent" to Color(0, 120, 212),
            "border" to Color(218, 220, 224),
            "hover" to Color(235, 235, 235),
            "selected" to Color(229, 241, 251),
            "secondary" to Color(128, 128, 128)
        ),
        "dark" to mapOf(
            "background" to Color(32, 32, 32),
            "foreground" to Color(255, 255, 255),
            "accent" to Color(96, 205, 255),
            "border" to Color(62, 62, 62),
            "hover" to Color(45, 45, 45),
            "selected" to Color(44, 53, 66),
            "secondary" to Color(153, 153, 153)
        )
    )
    
    /**
     * Applies theme settings and appearance
     */
    fun applyTheme(isDarkMode: Boolean) {
        try {
            // Set some system defaults
            UIManager.put("Button.arc", 10)
            UIManager.put("Component.arc", 10)
            UIManager.put("ProgressBar.arc", 10)
            UIManager.put("TextComponent.arc", 10)
            
            val theme = if (isDarkMode) win11Colors["dark"]!! else win11Colors["light"]!!
            
            // Basic colors
            UIManager.put("Panel.background", theme["background"])
            UIManager.put("Panel.foreground", theme["foreground"])
            UIManager.put("Label.foreground", theme["foreground"])
            UIManager.put("Button.foreground", theme["foreground"])
            UIManager.put("Button.background", theme["background"])
            UIManager.put("ComboBox.foreground", theme["foreground"])
            UIManager.put("ComboBox.background", theme["background"])
            UIManager.put("CheckBox.foreground", theme["foreground"])
            UIManager.put("CheckBox.background", theme["background"])
            UIManager.put("TextField.foreground", theme["foreground"])
            UIManager.put("TextField.background", if (isDarkMode) Color(50, 50, 50) else Color.WHITE)
            UIManager.put("ScrollPane.background", theme["background"])
            
            // Font
            val defaultFont = Font("Segoe UI Variable", Font.PLAIN, 12)
            UIManager.put("Button.font", defaultFont)
            UIManager.put("Label.font", defaultFont)
            UIManager.put("ComboBox.font", defaultFont)
            UIManager.put("CheckBox.font", defaultFont)
            UIManager.put("RadioButton.font", defaultFont)
            UIManager.put("TextField.font", defaultFont)
            UIManager.put("TextArea.font", defaultFont)
            UIManager.put("Menu.font", defaultFont)
            UIManager.put("MenuItem.font", defaultFont)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Applies Windows 11 style button appearance
     */
    fun styleButton(button: JButton, isPrimary: Boolean = false) {
        button.isBorderPainted = true
        button.isFocusPainted = false
        button.isContentAreaFilled = true
        button.border = CompoundBorder(
            LineBorder(Color(180, 180, 180), 1, true),
            EmptyBorder(6, 12, 6, 12)
        )
        button.font = Font("Segoe UI Variable", Font.PLAIN, 12)
        button.cursor = Cursor(Cursor.HAND_CURSOR)
        
        if (isPrimary) {
            button.background = Color(0, 120, 212)
            button.foreground = Color.WHITE
        }
    }
    
    /**
     * Applies modern panel style - rounded corners and light shadows seen in Windows 11
     */
    fun stylePanel(panel: JPanel, useElevation: Boolean = false) {
        panel.border = if (useElevation) {
            CompoundBorder(
                ShadowBorder(5, 5, 10),
                EmptyBorder(10, 10, 10, 10)
            )
        } else {
            EmptyBorder(10, 10, 10, 10)
        }
    }
    
    /**
     * Applies Windows 11 style for CheckBox and RadioButton
     */
    fun styleCheckComponent(component: JComponent) {
        if (component is AbstractButton) {
            component.isFocusPainted = false
        }
        component.font = Font("Segoe UI Variable", Font.PLAIN, 12)
    }
    
    /**
     * Adds hover effect to components like buttons
     */
    fun addHoverEffect(component: JComponent, isDarkMode: Boolean) {
        val originalBackground = component.background
        val hoverColor = if (isDarkMode) Color(45, 45, 45) else Color(235, 235, 235)
        
        component.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                if (component.isEnabled) {
                    component.background = hoverColor
                }
            }
            
            override fun mouseExited(e: java.awt.event.MouseEvent) {
                if (component.isEnabled) {
                    component.background = originalBackground
                }
            }
        })
    }
}

/**
 * Windows 11 style shadow border effect
 */
class ShadowBorder(
    private val shadowSize: Int = 5,
    private val cornerRadius: Int = 10,
    private val shadowOpacity: Int = 10
) : Border {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val opacity = shadowOpacity / 100f
        g2d.composite = AlphaComposite.SrcOver.derive(opacity)
        g2d.color = Color.GRAY
        
        for (i in 0 until shadowSize) {
            g2d.fillRoundRect(
                x + i, y + i, 
                width - i * 2, 
                height - i * 2, 
                cornerRadius, cornerRadius
            )
        }
        
        g2d.dispose()
    }
    
    override fun getBorderInsets(c: Component): Insets {
        return Insets(shadowSize, shadowSize, shadowSize, shadowSize)
    }
    
    override fun isBorderOpaque(): Boolean {
        return false
    }
} 