package com.kis.mindfocus.feature.session.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.kis.mindfocus.R
import com.kis.mindfocus.feature.session.model.SessionSummaryUi
import com.kis.mindfocus.feature.session.toClockLabel
import com.kis.mindfocus.ui.theme.LocalSpacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SessionHistoryItem(
    summary: SessionSummaryUi,
    onClick: (String) -> Unit,
    onRetrySync: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    }
    val startedAtLabel = formatter.format(summary.startedAt)
    // The row reads as one control; the trailing retry button keeps its own click and label.
    val openLabel = stringResource(R.string.history_open_description, startedAtLabel)

    ListItem(
        modifier = modifier.clickable(
            onClickLabel = openLabel,
            role = Role.Button,
            onClick = { onClick(summary.id) },
        ),
        headlineContent = { Text(text = startedAtLabel) },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.distraction_total,
                    summary.distractionCount,
                ) + " · " + summary.duration.toClockLabel(),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                SyncBadge(isSynced = summary.isSynced)
                if (!summary.isSynced) {
                    IconButton(
                        onClick = { onRetrySync(summary.id) },
                        modifier = Modifier.sizeIn(
                            minWidth = spacing.minTouchTarget,
                            minHeight = spacing.minTouchTarget,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(
                                R.string.history_retry_sync_description,
                                startedAtLabel,
                            ),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SyncBadge(isSynced: Boolean, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    // The icon shape differs as well as the tint, so sync state does not depend on colour alone.
    val icon = if (isSynced) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff
    val description = stringResource(
        if (isSynced) R.string.history_synced else R.string.history_not_synced,
    )

    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = if (isSynced) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
        modifier = modifier.size(spacing.large),
    )
}

@Composable
fun SessionHistoryEmptyState(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Text(
        text = stringResource(R.string.history_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.large),
    )
}
