package io.github.ntbbloodbath.pocketnumbat.calc

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ExchangeRates(private val cacheFile: File) {
    data class Rates(val xml: String, val fromNetwork: Boolean)

    fun load(): Rates? {
        val cached = readCache()
        if (cached != null && isFresh()) {
            return Rates(cached, fromNetwork = false)
        }

        val fetched = try {
            fetch()
        } catch (_: IOException) {
            null
        }

        if (fetched != null && isValid(fetched)) {
            return Rates(fetched, fromNetwork = true)
        }

        return cached?.let { Rates(it, fromNetwork = false) }
    }

    fun save(xml: String) {
        runCatching { cacheFile.writeText(xml) }
    }

    private fun readCache(): String? = runCatching {
        if (!cacheFile.isFile) {
            null
        } else {
            cacheFile.readText().takeIf(::isValid)
        }
    }.getOrNull()

    private fun isFresh(): Boolean =
        System.currentTimeMillis() - cacheFile.lastModified() <= CACHE_MAX_AGE_MS

    private fun fetch(): String? {
        val connection = URL(ECB_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "PocketNumbat/0.1")

            val code = connection.responseCode
            if (code !in 200..299) {
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isValid(xml: String): Boolean = RATE_ENTRY.containsMatchIn(xml)

    companion object {
        private const val ECB_URL =
            "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
        private const val TIMEOUT_MS = 4_000
        private const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
        private val RATE_ENTRY =
            Regex("""<Cube\s+currency=["'][A-Z]{3}["']\s+rate=["'][0-9.]+["']""")
    }
}
