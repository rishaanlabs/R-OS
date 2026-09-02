package com.rishaanlabs.ros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.rishaanlabs.ros.ui.components.RosHairline
import com.rishaanlabs.ros.ui.theme.Ros
import com.rishaanlabs.ros.ui.theme.RosMeta
import com.rishaanlabs.ros.ui.theme.RosSans

/**
 * The persistent top row: a search field that is really a button, and one control for everything
 * else.
 *
 * The field does not accept typing here. It opens Search, which is the screen that can actually
 * answer — a field that looked editable but forwarded the first keystroke somewhere else would be
 * worse than a button that says what it does.
 */
@Composable
fun RosTopBar(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Ros.colors.bg)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ros.colors.surf)
                .border(1.dp, Ros.colors.line, RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onSearch)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Ros.colors.ink3,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "Search R-OS",
                style = MaterialTheme.typography.bodyMedium,
                color = Ros.colors.ink3,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ros.colors.surf)
                .border(1.dp, Ros.colors.line, RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Everything else",
                tint = Ros.colors.ink3,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * The bottom bar: four words.
 *
 * No icons, because the design distinguishes the selected tab by weight and ink alone. An unread
 * count on Inbox is the one thing allowed to interrupt that, since it is the only tab whose
 * contents demand action rather than offering it.
 */
@Composable
internal fun RosBottomBar(
    tabs: List<RosTab>,
    currentDestination: NavDestination?,
    onSelect: (com.rishaanlabs.ros.navigation.Screen) -> Unit,
    inboxCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(Ros.colors.bg)) {
        RosHairline(color = Ros.colors.line)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 11.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected =
                    currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { onSelect(tab.screen) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.label,
                        fontFamily = RosSans,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Ros.colors.ink else Ros.colors.ink4
                    )
                    if (tab.label == "Inbox" && inboxCount > 0) {
                        Text(
                            text = inboxCount.toString(),
                            style = RosMeta,
                            color = Ros.colors.onAcc,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Ros.colors.acc)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Home's capture affordance: a full-width bar that says what it takes. */
@Composable
fun RosCaptureBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Ros.colors.surf)
            .border(1.dp, Ros.colors.line, RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = Ros.colors.ink2,
            modifier = Modifier.size(15.dp)
        )
        Text(
            "Capture anything…",
            style = MaterialTheme.typography.bodyMedium,
            color = Ros.colors.ink3
        )
    }
}
