package online.ruin_of_future.reporter

import kotlinx.coroutines.awaitAll
import okhttp3.internal.concurrent.TaskRunner.Companion.logger
import java.io.IOException
import org.jsoup.Jsoup
import java.awt.Color
import java.awt.Font
import java.awt.Image.SCALE_SMOOTH
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.text.DateFormatter

class NewsCrawler {
    private val httpGetter = HTTPGetter()

    private val entryUrl: String = "https://www.zhihu.com/people/mt36501"


    private val byteArrayCache = Cached(byteArrayOf(), 1000 * 60 * 60 * 4L)
    private val newsTextCache = Cached(String(), 1000 * 60 * 60 * 4L)

    private val font = Font
        .createFont(Font.TRUETYPE_FONT, this.javaClass.getResourceAsStream("/chinese_font.ttf"))
        .deriveFont(25f)

    @Throws(IOException::class)
    suspend fun newsToday(): Pair<String, ByteArray?> {
        if (byteArrayCache.isNotOutdated()) {
            return Pair(newsTextCache.value,byteArrayCache.value)
        }

        val entryPageDoc = Jsoup.parse(httpGetter.get(entryUrl))
        var todayUrl: String = entryPageDoc.select("div.ArticleItem h2.ContentItem-title a[href]").first()?.attr("href")
            ?: throw IOException("Failed to get url!")
        var todayTitle: String = entryPageDoc.select("div.ArticleItem h2.ContentItem-title a").first()?.text()
            ?: throw IOException("Failed to get title!")
        println(todayTitle)
        val myFormatter = DateTimeFormatter.ofPattern("M月d日")
//        LocalDateTime.now().format(myFormatter)

        if (todayUrl.startsWith("//")) {
            todayUrl = "https:$todayUrl"
        }

        val newsDoc = Jsoup.parse(httpGetter.get(todayUrl))
        val newsNode = newsDoc.select("div.RichText.ztext.Post-RichText")
//        println(newsDoc)
//        var newsImgUrl = newsNode.select("figure img").attr("src")
//            ?: throw IOException("Failed to get ImageUrl!")
//        newsImgUrl = "https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20vZGVubmlzX2xlZS9waWNnb19waWN0dXJlX2JlZC9yYXcvbWFzdGVyL2ltYWdlLTIwMjAwNTE5MTkyOTM1NTE5LnBuZw?x-oss-process=image/format,png"
//        println(newsImgUrl)
//        logger.info(
//            newsImgUrl
//        )
//        val newsImg = ImageIO.read(URL(newsImgUrl))
//            ?: throw IOException("Failed to get Image!")
        val newsTextElement = newsNode.select("p")
        val newsTextStringBuilder = StringBuilder()
        for (p in newsTextElement) {
            val rawStr = StringBuilder()
            var lastNotChinise = false
            for (ch in p.text()) {
                val thisNotChinese = Character.UnicodeScript.of(ch.code) != Character.UnicodeScript.HAN &&
                        (ch.isLetter() || ch.isDigit()) &&
                        !(ch == '.' || ch == '。' || ch == ':' || ch == '：' || ch == ',' || ch == '，')

                if (thisNotChinese) {
                    if (!lastNotChinise) {
                        rawStr.append(' ')
                    }
                    rawStr.append("$ch")
                    lastNotChinise = true
                } else {
                    if (lastNotChinise) {
                        rawStr.append(' ')
                    }
                    rawStr.append("$ch")
                    lastNotChinise = false
                }
            }

            val pStr = rawStr.toString()
            if (pStr.isEmpty()) {
                continue
            }
            if (pStr.contains("在这里，每天 60 秒读懂世界") || pStr.contains("【微语】")){
                continue
            }
            val lineLen = 40
            newsTextStringBuilder.append(pStr)
            newsTextStringBuilder.append('\n')
//            if (pStr.length <= lineLen) {
//                newsTextStringBuilder.append(pStr)
//                newsTextStringBuilder.append('\n')
//            } else {
//                var i = 0;
//                while (i < pStr.length) {
//                    newsTextStringBuilder.append(pStr.subSequence(i, min(i + lineLen, pStr.length)))
//                    i += lineLen
//                    while (i < pStr.length && Character.UnicodeScript.of(pStr[i].code) != Character.UnicodeScript.HAN) {
//                        newsTextStringBuilder.append(pStr[i])
//                        i += 1
//                    }
//                    newsTextStringBuilder.append('\n')
//                }
//            }
//            newsTextStringBuilder.append("\n")
        }
        //Delete last "\n"
        if (newsTextStringBuilder.isNotEmpty())newsTextStringBuilder.setLength(newsTextStringBuilder.length-1)
        var newsText = newsTextStringBuilder.toString()
        val imgWidth = 860


//        val scaledImgHeight = newsImg.height * imgWidth / newsImg.width
//        var imgHeight = scaledImgHeight + font.size * 2
//        for (line in newsText.lines()) {
//            if (line.isEmpty()) {
//                imgHeight += font.size / 2
//            } else {
//                imgHeight += (font.size * 1.5).toInt()
//            }
//        }
//        val bufferedImage =
//            BufferedImage(
//                imgWidth,
//                imgHeight,
//                BufferedImage.TYPE_INT_RGB
//            )
//
//
//        var g = bufferedImage.createGraphics()
//        g.color = Color.WHITE
//        g.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
//        g.dispose()
//
//        g = bufferedImage.createGraphics()
//
//        g.drawImage(
//            newsImg.getScaledInstance(imgWidth, scaledImgHeight, SCALE_SMOOTH),
//            0,
//            0,
//            null
//        )
//        g.dispose()
//
//        g = bufferedImage.createGraphics()
//        g.font = font
//        g.color = Color.BLACK
//        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
//        var curHeight = scaledImgHeight + font.size * 2
//        newsText.lines().forEach { s ->
//            if (s.trim().isNotEmpty()) {
//                g.drawString(
//                    s,
//                    10,
//                    curHeight
//                )
//                curHeight += (font.size * 1.5).toInt()
//            } else {
//                curHeight += font.size / 2
//            }
//        }
//        g.dispose()
        val os = ByteArrayOutputStream()
//        ImageIO.write(newsImg, "png", os)
        newsText = newsText.replace("强奸","强I奸")
        println(todayTitle)
        println(LocalDateTime.now().format(myFormatter))
        if (todayTitle.contains(LocalDateTime.now().format(myFormatter))) {
            newsTextCache.value = newsText
            byteArrayCache.value = os.toByteArray()
            return Pair(newsTextCache.value, byteArrayCache.value)
        }else {
            newsTextCache.value = "Ops!今日暂无推送"
            return Pair(newsTextCache.value , null)
        }
    }
}