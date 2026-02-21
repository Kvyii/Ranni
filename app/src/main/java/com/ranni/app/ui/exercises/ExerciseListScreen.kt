package com.ranni.app.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.ranni.app.data.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel,
    onAddExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onStartSession: (Long) -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    // Drag state — which list index is being dragged, its pixel offset, and captured card height
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No exercises yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, exercise ->
                    val isBeingDragged = dragIndex == index
                    val yOffset = if (isBeingDragged) dragOffsetY else 0f

                    // SwipeToDismissBox enables left-swipe to delete, replacing long-press delete
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            // When fully swiped end-to-start, trigger the delete confirmation dialog
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                exerciseToDelete = exercise
                            }
                            // Return false so the item snaps back — the dialog handles actual deletion
                            false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false, // only left-swipe (end-to-start)
                        backgroundContent = {
                            // Red background with delete icon revealed on swipe
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        },
                        modifier = Modifier
                            // graphicsLayer floats the card visually without disrupting LazyColumn layout
                            .graphicsLayer { translationY = yOffset }
                            // Capture card height from the first item for drag hit-testing
                            .onGloballyPositioned { coords ->
                                if (index == 0) cardHeightPx = coords.size.height.toFloat()
                            }
                            // Long-press initiates drag; keyed on stable exercise ID so closure is never stale
                            .pointerInput(exercise.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        dragOffsetY += dragAmount.y
                                        // Swap when drag offset crosses 50% of a card height
                                        if (cardHeightPx > 0f) {
                                            val targetIndex = (dragIndex + (dragOffsetY / cardHeightPx).toInt())
                                                .coerceIn(0, exercises.lastIndex)
                                            if (targetIndex != dragIndex) {
                                                // Subtract consumed pixels before updating dragIndex
                                                dragOffsetY -= (targetIndex - dragIndex) * cardHeightPx
                                                viewModel.moveExercise(dragIndex, targetIndex)
                                                dragIndex = targetIndex
                                            }
                                        }
                                    },
                                    onDragEnd = { dragIndex = -1; dragOffsetY = 0f },
                                    onDragCancel = { dragIndex = -1; dragOffsetY = 0f }
                                )
                            }
                    ) {
                        ExerciseCard(
                            exercise = exercise,
                            onClick = { onStartSession(exercise.id) },
                            onEdit = { onEditExercise(exercise.id) },
                            isBeingDragged = isBeingDragged
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddExercise,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Exercise")
        }
    }

    // Delete confirmation dialog — triggered by swipe, same logic as before
    exerciseToDelete?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Delete \"${ex.name}\"?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(ex)
                    exerciseToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    isBeingDragged: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Raise elevation slightly while dragging for a "lifted" cue
            .then(if (isBeingDragged) Modifier.graphicsLayer { shadowElevation = 8f } else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${exercise.sets} sets" +
                    (exercise.setDurationSeconds?.let { " · ${it}s hold" } ?: "") +
                    " · ${exercise.restDurationSeconds}s rest",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
    }
}
