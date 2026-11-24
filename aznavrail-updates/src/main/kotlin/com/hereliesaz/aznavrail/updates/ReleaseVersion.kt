package com.hereliesaz.aznavrail.updates

/** Strict numeric release comparison. Unknown formats are not evidence of a newer build. */
internal object ReleaseVersion {
    private val numeric = Regex("""[vV]?[0-9]+(?:\.[0-9]+)*""")
    private val code = Regex("""\bversionCode\s+(\d+)\b""")
    private val build = Regex("""Automated release build\s*[—–-]\s*([0-9]+(?:\.[0-9]+)*)""")

    /** Prefer the central release workflow's build code over a potentially three-part tag. */
    fun newer(tag: String, body: String, installedName: String?, installedCode: Long): Boolean {
        val remoteCode = code.find(body)?.groupValues?.get(1)?.toLongOrNull()
        if (remoteCode != null) return remoteCode > installedCode
        val candidate = build.find(body)?.groupValues?.get(1) ?: tag
        return compare(candidate, installedName ?: "") == 1
    }

    /** Returns null if either version is not a strictly numeric dotted version. */
    fun compare(candidate: String, installed: String): Int? {
        fun parse(value: String): List<Long>? {
            if (!numeric.matches(value)) return null
            return value.removePrefix("v").removePrefix("V").split('.').map { it.toLongOrNull() ?: return null }
        }
        val a = parse(candidate) ?: return null
        val b = parse(installed) ?: return null
        for (i in 0 until maxOf(a.size, b.size)) {
            val comparison = a.getOrElse(i) { 0L }.compareTo(b.getOrElse(i) { 0L })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
