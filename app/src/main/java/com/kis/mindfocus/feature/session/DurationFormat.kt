package com.kis.mindfocus.feature.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kis.mindfocus.R
import java.time.Duration
import java.util.Locale

/** `HH:MM:SS`, dropping the hours group until it is needed. Pure, so it unit-tests directly. */
fun Duration.toClockLabel(): String {
    val total = coerceAtLeast(Duration.ZERO).seconds
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/**
 * TalkBack reads `12:05` as "twelve oh five", i.e. a time of day. The clock label therefore needs a
 * spoken counterpart rather than a content description that repeats it.
 */
@Composable
fun Duration.toSpokenLabel(): String {
    val total = coerceAtLeast(Duration.ZERO).seconds
    val hours = (total / 3600).toInt()
    val minutes = ((total % 3600) / 60).toInt()
    val seconds = (total % 60).toInt()

    val parts = buildList {
        if (hours > 0) add(pluralStringResource(R.plurals.duration_hours, hours, hours))
        if (minutes > 0) add(pluralStringResource(R.plurals.duration_minutes, minutes, minutes))
        if (seconds > 0 || isEmpty()) {
            add(pluralStringResource(R.plurals.duration_seconds, seconds, seconds))
        }
    }
    return parts.reduce { spoken, part -> stringResource(R.string.duration_join, spoken, part) }
}
