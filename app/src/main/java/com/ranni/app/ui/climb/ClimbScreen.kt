package com.ranni.app.ui.climb

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranni.app.R
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.RouteColor
import com.ranni.app.data.model.isOutlineGym
import com.ranni.app.data.model.needsContrastRing

// Sentinel key used to identify the injury card in the expandedCard state
private const val INJURY_CARD_KEY = "__injury__"

@Composable
fun ClimbScreen(viewModel: ClimbViewModel) {
    // Tracks which card is expanded (gym name or INJURY_CARD_KEY); null = all collapsed
    var expandedCard by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<RouteColor?>(null) }
    var selectedGym by remember { mutableStateOf<String?>(null) }   // Gym name for the selected route
    // Holds the RouteColor that triggered the liar dialog (null = hidden)
    var liarRoute by remember { mutableStateOf<RouteColor?>(null) }
    var liarGym by remember { mutableStateOf<String?>(null) }       // Gym name for the liar route
    // Holds the injury severity pending confirmation (null = dialog hidden)
    var pendingInjurySeverity by remember { mutableStateOf<InjurySeverity?>(null) }

    // Observe the starred favourite gym; null means no favourite is set
    val favouriteGym by viewModel.favouriteGym.collectAsState()

    // Auto-expand the favourite gym each time it changes (including on first composition).
    // Only applies when no card is already open so manual expansions are not overridden.
    LaunchedEffect(favouriteGym) {
        if (expandedCard == null) {
            expandedCard = favouriteGym
        }
    }

    // "Liar" dialog for V12 routes — dismissing proceeds to the log confirmation
    if (liarRoute != null) {
        AlertDialog(
            onDismissRequest = {
                // Proceed to log confirmation with the V12 route
                selectedColor = liarRoute
                selectedGym = liarGym
                liarRoute = null; liarGym = null
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Liar!!", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                // Centered button row
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = {
                        // Proceed to log confirmation with the V12 route
                        selectedColor = liarRoute
                        selectedGym = liarGym
                        liarRoute = null; liarGym = null
                    }) {
                        Text("Okay I lied")
                    }
                }
            }
        )
    }

    // Log confirmation dialog with climb type options (New / Flash / Repeat)
    if (selectedColor != null) {
        val color = selectedColor!!
        val gym = selectedGym ?: ""
        AlertDialog(
            onDismissRequest = { selectedColor = null; selectedGym = null },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Log climb?", style = MaterialTheme.typography.titleMedium)
                }
            },
            confirmButton = {
                // Three climb type buttons: New (1x), Flash (1.25x), Repeat (0.75x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        // New: base score (1x multiplier)
                        viewModel.logClimb(color.name, gym, color.score, ClimbType.NEW)
                        selectedColor = null; selectedGym = null
                    }) {
                        Text("New")
                    }
                    TextButton(onClick = {
                        // Flash: 1.25x score multiplier for first-try sends
                        val flashScore = (color.score * 1.25).toInt()
                        viewModel.logClimb(color.name, gym, flashScore, ClimbType.FLASH)
                        selectedColor = null; selectedGym = null
                    }) {
                        Text("Flash")
                    }
                    TextButton(onClick = {
                        // Repeat: 0.75x score multiplier for re-climbed routes
                        val repeatScore = (color.score * 0.75).toInt()
                        viewModel.logClimb(color.name, gym, repeatScore, ClimbType.REPEAT)
                        selectedColor = null; selectedGym = null
                    }) {
                        Text("Repeat")
                    }
                }
            }
        )
    }

    // Injury confirmation dialog — shown after tapping a severity row
    if (pendingInjurySeverity != null) {
        val severity = pendingInjurySeverity!!
        AlertDialog(
            onDismissRequest = { pendingInjurySeverity = null },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Log ${severity.name.lowercase().replaceFirstChar { it.uppercase() }} injury?",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { pendingInjurySeverity = null }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        viewModel.logInjury(severity)
                        pendingInjurySeverity = null
                        expandedCard = null  // Collapse injury card after logging
                    }) {
                        Text("Log")
                    }
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header text prompting the user to pick a climb
            Text(
                "Select climb",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            // Collect the user-ordered active gyms from the ViewModel
            val activeGyms by viewModel.orderedActiveGyms.collectAsState()

            // Drag state — which active gym index is being dragged, its pixel offset, and card height
            var dragIndex by remember { mutableIntStateOf(-1) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var cardHeightPx by remember { mutableFloatStateOf(0f) }

            // Active (draggable) gym cards
            activeGyms.forEachIndexed { index, gym ->
                val isExpanded = expandedCard == gym.name
                val isBeingDragged = dragIndex == index
                val yOffset = if (isBeingDragged) dragOffsetY else 0f

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    // Raise elevation while dragging to give a "lifted" visual cue
                    tonalElevation = if (isBeingDragged) 8.dp else 2.dp,
                    shadowElevation = if (isBeingDragged) 8.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        // graphicsLayer translates the card visually without affecting layout of siblings
                        .graphicsLayer { translationY = yOffset }
                        // Capture card height from the first card (all cards are the same height)
                        .onGloballyPositioned { coords ->
                            if (index == 0) cardHeightPx = coords.size.height.toFloat()
                        }
                        // Long-press initiates drag; keyed on index so closure is never stale after a swap
                        .pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { _, dragAmount ->
                                    dragOffsetY += dragAmount.y
                                    // Compute target slot from accumulated offset — swap when crossing 50% of a card
                                    if (cardHeightPx > 0f) {
                                        val targetIndex = (dragIndex + (dragOffsetY / cardHeightPx).toInt())
                                            .coerceIn(0, activeGyms.lastIndex)
                                        if (targetIndex != dragIndex) {
                                            // Subtract the pixels consumed by the completed swap before updating dragIndex
                                            dragOffsetY -= (targetIndex - dragIndex) * cardHeightPx
                                            viewModel.moveGym(dragIndex, targetIndex)
                                            dragIndex = targetIndex
                                        }
                                    }
                                },
                                onDragEnd = { dragIndex = -1; dragOffsetY = 0f },
                                onDragCancel = { dragIndex = -1; dragOffsetY = 0f }
                            )
                        }
                ) {
                    Column {
                        val isFavourite = favouriteGym == gym.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCard = if (isExpanded) null else gym.name
                                    // Clear route selection when switching cards
                                    selectedColor = null
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                gym.logoRes?.let { logoRes ->
                                    GymLogo(logoRes = logoRes, gymName = gym.name)
                                }
                                Text(gym.name, style = MaterialTheme.typography.titleMedium)
                            }
                            // Right-hand controls: star toggle then expand arrow
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Star icon — tapping toggles this gym as the favourite
                                IconButton(
                                    onClick = { viewModel.setFavouriteGym(gym.name) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = if (isFavourite) "Remove favourite" else "Set as favourite",
                                        // Primary colour when starred, invisible against card when not
                                        tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                                    )
                                }
                                // Expand / collapse arrow
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                gym.routes.forEach { rc ->
                                    ColorRow(
                                        gymName = gym.name,
                                        routeColor = rc,
                                        onClick = {
                                            // Show liar dialog for V12, otherwise show log confirmation
                                            if (rc.grade == "V12") { liarRoute = rc; liarGym = gym.name }
                                            else { selectedColor = rc; selectedGym = gym.name }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Coming-soon gym cards — locked below active gyms, no drag gesture
            viewModel.comingSoonGyms.forEach { gym ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Dimmed to reinforce the "coming soon" (not yet active) state
                            gym.logoRes?.let { logoRes ->
                                GymLogo(logoRes = logoRes, gymName = gym.name, modifier = Modifier.alpha(0.5f))
                            }
                            Text(gym.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Extra spacing to visually separate the "Other" section from the climb cards
            Spacer(modifier = Modifier.height(8.dp))

            // Section title for non-climb actions, styled to match "Select climb"
            Text(
                "Other",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            // Injury card — same expandable card pattern as gym cards
            val isInjuryExpanded = expandedCard == INJURY_CARD_KEY
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCard = if (isInjuryExpanded) null else INJURY_CARD_KEY
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Injury", style = MaterialTheme.typography.titleMedium)
                        Icon(
                            if (isInjuryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isInjuryExpanded) {
                        Column {
                            // One row per severity level
                            InjurySeverity.entries.forEach { severity ->
                                InjuryRow(
                                    severity = severity,
                                    onClick = { pendingInjurySeverity = severity }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A tappable row showing the skull icon tinted for the given severity, mirroring ColorRow layout. */
@Composable
private fun InjuryRow(
    severity: InjurySeverity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Skull icon colored per severity
        Image(
            painter = painterResource(severity.skullRes),
            contentDescription = null,
            modifier = Modifier.size(25.dp)
        )
        // Severity label
        Text(
            severity.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // Right-aligned hint text describing what this severity feels like, forced single line
        Text(
            text = when (severity) {
                InjurySeverity.MILD     -> "Hurts but could keep climbing"
                InjurySeverity.MODERATE -> "Hurts a lot. Impedes climbing"
                InjurySeverity.SEVERE   -> "Welp. No climbing for a while"
                InjurySeverity.DEATH    -> "Welcome to Valhalla"
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1.5f)
        )
    }
}

/**
 * Gym logo shown left of the name on the card header. Fit within a fixed box so every logo
 * gets a consistent visual footprint, regardless of how much whitespace is baked into its own
 * source asset — some logos (e.g. Nomad's wordmark) have almost no internal padding and would
 * otherwise fill the row edge-to-edge and look oversized next to logos that already have
 * breathing room baked in, so those get extra padding applied here instead of in the asset.
 */
@Composable
private fun GymLogo(
    logoRes: Int,
    gymName: String,
    modifier: Modifier = Modifier
) {
    val extraPadding = if (gymName == "Nomad") 6.dp else 0.dp
    Image(
        painter = painterResource(logoRes),
        contentDescription = null,
        modifier = modifier
            .height(28.dp)
            .padding(vertical = extraPadding),
        contentScale = androidx.compose.ui.layout.ContentScale.FillHeight
    )
}

@Composable
private fun ColorRow(
    gymName: String,
    routeColor: RouteColor,
    onClick: () -> Unit
) {
    val color = routeColor.color
    // Custom/Outdoor gyms render hollow (outlined) — same distinction ClimbDot used to draw
    val isOutline = isOutlineGym(gymName)
    val textColor = when {
        isOutline -> color
        // Near-black/white route colors need a dark/light text override so the grade stays legible
        !color.needsContrastRing() -> Color.White
        color.luminance() > 0.5f -> Color.Black
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isOutline) Modifier.border(2.dp, color, RoundedCornerShape(8.dp))
                else Modifier.background(color)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            routeColor.grade,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
