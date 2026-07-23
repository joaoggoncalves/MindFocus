package com.kis.mindfocus.feature.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kis.mindfocus.R
import com.kis.mindfocus.feature.session.toClockLabel
import com.kis.mindfocus.feature.session.toSpokenLabel
import com.kis.mindfocus.ui.theme.LocalSpacing
import java.time.Duration

@Composable
fun SessionControlCard(
    isActive: Boolean,
    elapsed: Duration,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val spokenElapsed = stringResource(R.string.duration_description, elapsed.toSpokenLabel())

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            SessionStatusLabel(isActive = isActive)

            Text(
                text = elapsed.toClockLabel(),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                // Replaces the digits, which TalkBack would otherwise read as a time of day.
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = spokenElapsed
                },
            )

            Button(
                onClick = if (isActive) onStop else onStart,
                modifier = Modifier.heightIn(min = spacing.minTouchTarget),
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (isActive) R.string.session_stop else R.string.session_start,
                    ),
                    modifier = Modifier.padding(start = spacing.small),
                )
            }
        }
    }
}

@Composable
private fun SessionStatusLabel(isActive: Boolean, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    // Status carries an icon and a word as well as a colour — colour alone would exclude anyone
    // who cannot distinguish it.
    val icon = if (isActive) Icons.Outlined.RadioButtonChecked else Icons.Outlined.Circle
    val label = stringResource(
        if (isActive) R.string.session_status_active else R.string.session_status_idle,
    )
    val tint = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(spacing.medium),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}
