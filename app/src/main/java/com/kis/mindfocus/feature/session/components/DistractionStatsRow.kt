package com.kis.mindfocus.feature.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.kis.mindfocus.R
import com.kis.mindfocus.ui.theme.LocalSpacing

@Composable
fun DistractionStatsRow(
    noiseCount: Int,
    movementCount: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        DistractionStatTile(
            icon = Icons.Outlined.GraphicEq,
            label = stringResource(R.string.distraction_noise),
            count = noiseCount,
            modifier = Modifier.weight(1f),
        )
        DistractionStatTile(
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            label = stringResource(R.string.distraction_movement),
            count = movementCount,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DistractionStatTile(
    icon: ImageVector,
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    OutlinedCard(
        // Merged so TalkBack announces "Noise, 3" as one item instead of three separate nodes.
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium)
        }
    }
}
