package com.example.appbike

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceTextEncodingTest {
    @Test
    fun kotlinSourcesDoNotContainCommonMojibakeMarkers() {
        val workingDirectory = File(System.getProperty("user.dir").orEmpty())
        val sourceRoot = listOf(
            File(workingDirectory, "app/src/main"),
            File(workingDirectory, "src/main")
        ).firstOrNull(File::isDirectory)

        assertTrue("No se encontró app/src/main para verificar la codificación.", sourceRoot != null)

        val corrupted = sourceRoot!!
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .mapNotNull { file ->
                val text = file.readText(Charsets.UTF_8)
                val marker = MOJIBAKE_MARKERS.firstOrNull(text::contains)
                marker?.let { "${file.relativeTo(sourceRoot).path}: '$it'" }
            }
            .toList()

        assertTrue(
            "Se encontraron cadenas con codificación dañada:\n${corrupted.joinToString("\n")}",
            corrupted.isEmpty()
        )
    }

    private companion object {
        val MOJIBAKE_MARKERS = listOf("Ã", "Â", "â€", "ðŸ", "�")
    }
}
