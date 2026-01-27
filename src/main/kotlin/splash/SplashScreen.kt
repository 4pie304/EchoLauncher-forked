package splash

import java.awt.*
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.min
import kotlin.random.Random

fun createAndShowSplashScreen(statusLabel: JLabel): JWindow? {
    return runCatching {
        JWindow().apply {
            val props = Properties().apply {
                Thread.currentThread().contextClassLoader.getResourceAsStream("app.properties")?.use(::load)
            }
            val version = props.getProperty("version", "Unknown")
            val buildNumber = props.getProperty("buildNumber", "N/A")
            val versionText = "$version ($buildNumber)"

            val versionLabel = JLabel(versionText, SwingConstants.RIGHT).apply {
                foreground = Color.WHITE
            }
            statusLabel.apply {
                foreground = Color.WHITE
                horizontalAlignment = SwingConstants.RIGHT
            }

            // Выбор случайного баннера
            val bannerName = if (Random.nextBoolean()) "banner1.png" else "banner2.png"

            val possiblePaths = listOf(
                "drawable/$bannerName",
                bannerName,
                "composeResources/drawable/$bannerName"
            )

            var bannerStream = possiblePaths.firstNotNullOfOrNull { path ->
                Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            }

            if (bannerStream == null) {
                val file = java.io.File("src/main/composeResources/drawable/$bannerName")
                if (file.exists()) {
                    bannerStream = file.inputStream()
                }
            }

            if (bannerStream == null) {
                throw IllegalStateException("Banner not found: $bannerName")
            }

            val originalImage = ImageIO.read(bannerStream)
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val targetWidth = screenSize.width / 2.5
            val targetHeight = screenSize.height / 2.5
            val ratio = min(targetWidth / originalImage.width, targetHeight / originalImage.height)
            val newWidth = (originalImage.width * ratio).toInt()
            val newHeight = (originalImage.height * ratio).toInt()
            val finalImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)

            // Загрузка шрифта
            val fontName = "monocraft.ttc"
            val possibleFontPaths = listOf(
                "font/$fontName",
                fontName,
                "composeResources/font/$fontName"
            )

            var fontStream = possibleFontPaths.firstNotNullOfOrNull { path ->
                Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            }
            if (fontStream == null) {
                val file = java.io.File("src/main/composeResources/font/$fontName")
                if (file.exists()) {
                    fontStream = file.inputStream()
                }
            }

            val customFont = try {
                if (fontStream != null) {
                    // Пробуем загрузить шрифт. Если это TTC, createFont может вернуть шрифт, но он может не работать корректно
                    // в зависимости от реализации JDK.
                    Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(24f)
                } else {
                    println("Font not found: $fontName, using default")
                    Font("SansSerif", Font.BOLD, 24)
                }
            } catch (e: Exception) {
                println("Failed to load font: $e")
                Font("SansSerif", Font.BOLD, 24)
            }

            contentPane = JLayeredPane().apply {
                preferredSize = Dimension(newWidth, newHeight)
                layout = null // Отключаем LayoutManager, чтобы работать с setBounds

                // Панель с изображением и градиентом
                val imagePanel = object : JPanel() {
                    override fun paintComponent(g: Graphics) {
                        super.paintComponent(g)
                        val g2d = g as Graphics2D
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                        // Рисуем изображение
                        g2d.drawImage(finalImage, 0, 0, this)

                        // Рисуем градиент слева до центра
                        val gradient = GradientPaint(
                            0f, 0f, Color(0, 0, 0, 200), // Слева темный
                            (width / 2).toFloat(), 0f, Color(0, 0, 0, 0) // К центру прозрачный
                        )
                        g2d.paint = gradient
                        g2d.fillRect(0, 0, width / 2, height)
                    }
                }.apply {
                    setBounds(0, 0, newWidth, newHeight)
                    isOpaque = false
                }

                add(imagePanel)
                setLayer(imagePanel, JLayeredPane.DEFAULT_LAYER)

                // Надпись Materia Launcher
                val titleLabel = JLabel("Materia Launcher").apply {
                    font = customFont
                    foreground = Color.WHITE
                    setBounds(20, 20, newWidth - 40, 40)
                }
                add(titleLabel)
                setLayer(titleLabel, JLayeredPane.PALETTE_LAYER)

                statusLabel.apply {
                    setBounds(0, newHeight - 30 - 10, newWidth - 10, 20)
                }
                add(statusLabel)
                setLayer(statusLabel, JLayeredPane.PALETTE_LAYER)

                versionLabel.apply {
                    setBounds(0, newHeight - 15 - 10, newWidth - 10, 20)
                }
                add(versionLabel)
                setLayer(versionLabel, JLayeredPane.PALETTE_LAYER)
            }
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }.onFailure {
        println("Failed to create splash screen: ${it.stackTraceToString()}")
    }.getOrNull()
}
