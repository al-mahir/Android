package com.iti.domain.model.home

/**
 * Everything the Home screen needs, resolved in a single read so the UI renders in one pass
 * instead of stitching four independent loading states together.
 */
data class HomeSummary(
    val user: UserSummary,
    val continueReading: ContinueReading?,
    val sheikhs: List<Sheikh>,
    val circles: List<StudyCircle>,
)

/** The signed-in user, reduced to what the Home header renders. */
data class UserSummary(
    val displayName: String,
    val initials: String,
    val avatarUrl: String?,
)

/**
 * The user's last reading position. Null when they have never opened the Mushaf, in which
 * case Home hides the "Continue Reading" card entirely.
 */
data class ContinueReading(
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
)

/** A teacher the user can book a session with. */
data class Sheikh(
    val id: String,
    val name: String,
    val initials: String,
    val avatarUrl: String?,
    val rating: Double,
    val availability: SheikhAvailability,
)

/**
 * Presence of a [Sheikh]. Kept as an enum rather than a display string so the UI owns
 * localisation and colour — the data layer never ships user-facing text.
 */
enum class SheikhAvailability {
    AVAILABLE,
    IN_SESSION,
    OFFLINE,
}

/** A study circle (halaqa) the user can join. */
data class StudyCircle(
    val id: String,
    val title: String,
    val hostName: String,
    val isJoined: Boolean,
)
