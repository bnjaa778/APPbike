package com.example.appbike

private const val MEETUP_DATE_PREFIX = "Fecha y hora:"

internal data class MeetupDescriptionParts(
    val dateTime: String,
    val description: String
)

internal fun encodeMeetupDescription(dateTime: String, description: String): String {
    val cleanDateTime = dateTime.trim()
    val cleanDescription = description.trim()
    return buildString {
        if (cleanDateTime.isNotBlank()) {
            append(MEETUP_DATE_PREFIX)
            append(' ')
            append(cleanDateTime)
        }
        if (cleanDescription.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(cleanDescription)
        }
    }
}

internal fun decodeMeetupDescription(
    rawDescription: String,
    explicitDateTime: String = ""
): MeetupDescriptionParts {
    val cleanDescription = rawDescription.trim()
    val lines = cleanDescription.lines()
    val firstLine = lines.firstOrNull().orEmpty().trim()
    val hasEmbeddedDate = firstLine.startsWith(MEETUP_DATE_PREFIX, ignoreCase = true)
    val embeddedDateTime = if (hasEmbeddedDate) {
        firstLine.substring(MEETUP_DATE_PREFIX.length).trim()
    } else {
        ""
    }
    val visibleDescription = if (hasEmbeddedDate) {
        lines.drop(1).joinToString("\n").trim()
    } else {
        cleanDescription
    }
    return MeetupDescriptionParts(
        dateTime = explicitDateTime.trim().ifBlank { embeddedDateTime },
        description = visibleDescription
    )
}
