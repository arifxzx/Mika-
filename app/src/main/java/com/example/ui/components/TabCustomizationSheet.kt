package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.AppSettings
import com.example.model.BottomNavDest
import com.example.model.MainTab
import com.example.model.TabLongPressAction
import com.example.model.TabTapAction
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTabsCustomizationSheet(
    settings: AppSettings,
    onToggleTabEnabled: (MainTab) -> Unit,
    onMoveTab: (MainTab, Int) -> Unit,
    onSetDefaultTab: (MainTab) -> Unit,
    onDismiss: () -> Unit,
    onReorderEnabledTabs: ((List<MainTab>) -> Unit)? = null,
    onSetTabActions: ((TabTapAction?, TabLongPressAction?) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    val mikaColors = LocalMikaColors.current
    val haptic = LocalHapticFeedback.current

    val orderedTabs = remember(settings.enabledTabs) {
        (settings.enabledTabs + MainTab.entries).distinct()
    }
    val latestEnabledTabs by rememberUpdatedState(settings.enabledTabs)
    var draggingTab by remember { mutableStateOf<MainTab?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(150f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier.testTag("library_tab_customizer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customize Library Tabs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Drag handles to reorder or pick tap & hold actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("library_tab_customizer_done")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            if (onSetTabActions != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tab Tap Action",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabTapAction.entries.forEach { action ->
                        val selected = settings.tabTapAction == action
                        Surface(
                            onClick = { onSetTabActions(action, null) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) accent else mikaColors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accent else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tab Long-Press Action",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabLongPressAction.entries.forEach { action ->
                        val selected = settings.tabLongPressAction == action
                        Surface(
                            onClick = { onSetTabActions(null, action) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) accent else mikaColors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accent else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(orderedTabs, key = { _, tab -> tab.name }) { _, tab ->
                    val enabled = tab in settings.enabledTabs
                    val isDefault = settings.defaultTab == tab
                    val isDragging = draggingTab == tab

                    MikaGlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        tintColor = if (isDragging || enabled) accent else MaterialTheme.colorScheme.outline,
                        tintStrength = if (isDragging) 0.22f else if (isDefault) 0.14f else 0.05f,
                        frostedAlpha = if (enabled) 0.96f else 0.72f,
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragOffsetY
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                    shadowElevation = 12f
                                }
                            }
                            .onSizeChanged { s ->
                                if (s.height > 0) itemHeightPx = s.height.toFloat()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = enabled,
                                onCheckedChange = { onToggleTabEnabled(tab) },
                                enabled = !enabled || settings.enabledTabs.size > 1,
                                colors = CheckboxDefaults.colors(checkedColor = accent),
                                modifier = Modifier.testTag("customizer_tab_toggle_${tab.name.lowercase()}")
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (enabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isDefault -> "Default tab"
                                        enabled -> "Visible · drag handle to reorder"
                                        else -> "Hidden (in drawer)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDefault) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (enabled) {
                                Surface(
                                    onClick = { onSetDefaultTab(tab) },
                                    shape = RoundedCornerShape(50),
                                    color = if (isDefault) accent else mikaColors.surfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDefault) accent else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.testTag("customizer_tab_default_${tab.name.lowercase()}")
                                ) {
                                    Text(
                                        text = if (isDefault) "Default" else "Set",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDefault) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .pointerInput(tab) {
                                            detectVerticalDragGestures(
                                                onDragStart = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    draggingTab = tab
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggingTab = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingTab = null
                                                    dragOffsetY = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount
                                                    val step = (itemHeightPx * 0.72f).coerceAtLeast(44f)
                                                    val list = latestEnabledTabs.toMutableList()
                                                    val cur = list.indexOf(tab)
                                                    if (cur >= 0) {
                                                        if (dragOffsetY > step && cur < list.lastIndex) {
                                                            if (onReorderEnabledTabs != null) {
                                                                list.add(cur + 1, list.removeAt(cur))
                                                                onReorderEnabledTabs(list)
                                                            } else {
                                                                onMoveTab(tab, 1)
                                                            }
                                                            dragOffsetY -= itemHeightPx
                                                        } else if (dragOffsetY < -step && cur > 0) {
                                                            if (onReorderEnabledTabs != null) {
                                                                list.add(cur - 1, list.removeAt(cur))
                                                                onReorderEnabledTabs(list)
                                                            } else {
                                                                onMoveTab(tab, -1)
                                                            }
                                                            dragOffsetY += itemHeightPx
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("customizer_tab_drag_${tab.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = MikaIcons.DragHandle,
                                        contentDescription = "Drag to reorder ${tab.label}",
                                        tint = if (isDragging) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(mikaScaledIcon(20.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavCustomizationSheet(
    settings: AppSettings,
    onToggleShowBar: (Boolean) -> Unit,
    onMoveDest: (BottomNavDest, Int) -> Unit,
    onToggleDestHidden: (BottomNavDest) -> Unit,
    onDismiss: () -> Unit,
    onReorderDests: ((List<BottomNavDest>) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    val orderedDests = remember(settings.bottomNavOrder) {
        (settings.bottomNavOrder + BottomNavDest.entries).distinct()
    }
    val latestOrderedDests by rememberUpdatedState(orderedDests)
    val visibleCount = remember(settings.hiddenBottomNavItems) {
        BottomNavDest.entries.count { it !in settings.hiddenBottomNavItems }
    }
    var draggingDest by remember { mutableStateOf<BottomNavDest?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(148f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier.testTag("bottom_nav_customizer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customize Navigation Bar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Drag handles to reorder destinations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bottom_nav_customizer_done")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            MikaGlassSurface(
                shape = RoundedCornerShape(16.dp),
                tintStrength = 0.08f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Bottom Navigation Bar",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Solid dock across Home, Folders, Library & Videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = settings.showBottomNavBar,
                        onCheckedChange = onToggleShowBar,
                        modifier = Modifier.testTag("bottom_nav_show_bar_switch")
                    )
                }
            }

            orderedDests.forEach { dest ->
                val isVisible = dest !in settings.hiddenBottomNavItems
                val canHide = !isVisible || visibleCount > 1
                val isDragging = draggingDest == dest

                val destIcon = when (dest) {
                    BottomNavDest.HOME -> MikaIcons.HomeLinear
                    BottomNavDest.FOLDERS -> MikaIcons.Folder
                    BottomNavDest.LIBRARY -> MikaIcons.MusicNote
                    BottomNavDest.VIDEOS -> MikaIcons.VideoLinear
                }

                MikaGlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    tintColor = if (isDragging || isVisible) accent else MaterialTheme.colorScheme.outline,
                    tintStrength = if (isDragging) 0.22f else if (isVisible) 0.08f else 0.03f,
                    frostedAlpha = if (isVisible) 0.96f else 0.72f,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffsetY
                                scaleX = 1.02f
                                scaleY = 1.02f
                                shadowElevation = 12f
                            }
                        }
                        .onSizeChanged { s ->
                            if (s.height > 0) itemHeightPx = s.height.toFloat()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isVisible,
                            onCheckedChange = { onToggleDestHidden(dest) },
                            enabled = canHide,
                            colors = CheckboxDefaults.colors(checkedColor = accent),
                            modifier = Modifier.testTag("customizer_bnav_toggle_${dest.name.lowercase()}")
                        )
                        Box(
                            modifier = Modifier.padding(end = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = destIcon,
                                contentDescription = null,
                                tint = if (isVisible) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(mikaScaledIcon(20.dp))
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (dest) {
                                    BottomNavDest.HOME -> "Global Audio & Video hub"
                                    BottomNavDest.FOLDERS -> "Device folders & hierarchy"
                                    BottomNavDest.LIBRARY -> "Music library tabs"
                                    BottomNavDest.VIDEOS -> "Offline video player hub"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .pointerInput(dest) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingDest = dest
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggingDest = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingDest = null
                                            dragOffsetY = 0f
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount
                                            val step = (itemHeightPx * 0.72f).coerceAtLeast(44f)
                                            val list = latestOrderedDests.toMutableList()
                                            val cur = list.indexOf(dest)
                                            if (cur >= 0) {
                                                if (dragOffsetY > step && cur < list.lastIndex) {
                                                    if (onReorderDests != null) {
                                                        list.add(cur + 1, list.removeAt(cur))
                                                        onReorderDests(list)
                                                    } else {
                                                        onMoveDest(dest, 1)
                                                    }
                                                    dragOffsetY -= itemHeightPx
                                                } else if (dragOffsetY < -step && cur > 0) {
                                                    if (onReorderDests != null) {
                                                        list.add(cur - 1, list.removeAt(cur))
                                                        onReorderDests(list)
                                                    } else {
                                                        onMoveDest(dest, -1)
                                                    }
                                                    dragOffsetY += itemHeightPx
                                                }
                                            }
                                        }
                                    )
                                }
                                .testTag("customizer_bnav_drag_${dest.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = MikaIcons.DragHandle,
                                contentDescription = "Drag to reorder ${dest.label}",
                                tint = if (isDragging) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(mikaScaledIcon(20.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
