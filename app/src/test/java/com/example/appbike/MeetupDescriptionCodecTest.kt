package com.example.appbike

import org.junit.Assert.assertEquals
import org.junit.Test

class MeetupDescriptionCodecTest {
    @Test
    fun encodedDateIsSeparatedFromVisibleDescription() {
        val encoded = encodeMeetupDescription(
            dateTime = "10 de agosto, 18:00",
            description = "Salida desde la plaza."
        )

        assertEquals(
            "Fecha y hora: 10 de agosto, 18:00\nSalida desde la plaza.",
            encoded
        )
        assertEquals(
            MeetupDescriptionParts(
                dateTime = "10 de agosto, 18:00",
                description = "Salida desde la plaza."
            ),
            decodeMeetupDescription(encoded)
        )
    }

    @Test
    fun explicitBackendDateWinsWithoutRemovingUserText() {
        assertEquals(
            MeetupDescriptionParts(
                dateTime = "2026-08-10T18:00:00-04:00",
                description = "Ruta costera."
            ),
            decodeMeetupDescription(
                rawDescription = "Ruta costera.",
                explicitDateTime = "2026-08-10T18:00:00-04:00"
            )
        )
    }

    @Test
    fun editingKeepsTheDateMetadataExactlyOnce() {
        val loaded = decodeMeetupDescription(
            "Fecha y hora: Sabado 09:30\nDescripcion anterior"
        )
        val updated = encodeMeetupDescription(loaded.dateTime, "Descripcion nueva")

        assertEquals(
            "Fecha y hora: Sabado 09:30\nDescripcion nueva",
            updated
        )
    }
}
