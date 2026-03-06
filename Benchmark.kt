import kotlin.system.measureNanoTime

fun main() {
    val entriesToWrite = List(1000) { "This is a sample entry $it with some extra text" }
    val maxSizeBytes = 1000000

    // Original
    val timeOriginal = measureNanoTime {
        var currentSize = 0
        for (entry in entriesToWrite) {
            val entryWithNewline = entry + System.lineSeparator()
            val entrySize = entryWithNewline.toByteArray(Charsets.UTF_8).size
            if (maxSizeBytes == 0) break
            if (currentSize + entrySize <= maxSizeBytes) {
                currentSize += entrySize
            } else {
                break
            }
        }
    }

    // Optimized
    val timeOptimized = measureNanoTime {
        var currentSize = 0
        val lineSepSize = System.lineSeparator().toByteArray(Charsets.UTF_8).size
        for (entry in entriesToWrite) {
            if (maxSizeBytes == 0) break
            val entrySize = entry.toByteArray(Charsets.UTF_8).size + lineSepSize
            if (currentSize + entrySize <= maxSizeBytes) {
                currentSize += entrySize
            } else {
                break
            }
        }
    }

    println("Original: $timeOriginal ns")
    println("Optimized: $timeOptimized ns")
}
