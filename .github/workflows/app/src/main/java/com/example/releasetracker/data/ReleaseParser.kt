package com.example.releasetracker.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object ReleaseParser {
    fun parseAtom(xml: String): List<Release> {
        val releases = mutableListOf<Release>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentTitle = ""
            var currentContent = ""
            var currentLink = ""
            var currentUpdated = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "entry" -> {
                                currentTitle = ""
                                currentContent = ""
                                currentLink = ""
                                currentUpdated = ""
                            }
                            "title" -> currentTitle = parser.nextText()
                            "content" -> currentContent = parser.nextText()
                            "updated" -> currentUpdated = parser.nextText()
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) currentLink = href
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "entry") {
                            val downloadUrl = extractApkUrl(currentContent)
                            releases.add(
                                Release(
                                    version = currentTitle,
                                    title = currentTitle,
                                    changelog = currentContent.take(1000),
                                    publishedDate = currentUpdated,
                                    downloadUrl = downloadUrl,
                                    pageUrl = currentLink
                                )
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return releases
    }

    private fun extractApkUrl(content: String): String? {
        val regex = Regex("""href="([^"]*\.apk[^"]*)"""", RegexOption.IGNORE_CASE)
        return regex.find(content)?.groupValues?.get(1)
    }
}
