package com.rishaanlabs.ros.ui.screen.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rishaanlabs.ros.data.local.entity.InboxItemType

/**
 * Quick capture.
 *
 * The whole design goal is that the fastest possible path — open, type, save — requires touching
 * nothing else. The user should never have to work out what a thought *is* before they are
 * allowed to write it down; that decision belongs to the Inbox, later, when there is time to
 * make it properly.
 *
 * Everything optional is therefore hidden until asked for, and the text field is the only thing
 * with focus when the sheet opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    onDismiss: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    var showOptions by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(InboxItemType.UNSPECIFIED) }

    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun save() {
        if (text.isBlank()) return
        viewModel.capture(text, type)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // No heading. The placeholder already says what to do, and a title would only push
            // the text field further from the thumb.
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        "What's on your mind?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                singleLine = false,
                minLines = 3,
                maxLines = 8,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (showOptions) {
                Text(
                    text = "If you already know what this is, say so. Otherwise leave it — " +
                        "you can decide in the Inbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaptureTypeChip("Task", InboxItemType.TASK, type) { type = it }
                    CaptureTypeChip("Waiting", InboxItemType.WAITING, type) { type = it }
                    CaptureTypeChip("Note", InboxItemType.NOTE, type) { type = it }
                    CaptureTypeChip("Idea", InboxItemType.IDEA, type) { type = it }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showOptions) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                } else {
                    TextButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Type")
                    }
                }

                // Save sits on the trailing edge, low in the sheet and just above the keyboard,
                // so it stays inside comfortable thumb reach one-handed.
                Button(
                    onClick = { save() },
                    enabled = text.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureTypeChip(
    label: String,
    value: InboxItemType,
    selected: InboxItemType,
    onSelect: (InboxItemType) -> Unit
) {
    FilterChip(
        selected = selected == value,
        // Tapping the chosen type again clears it, so a mis-tap does not force a classification.
        onClick = { onSelect(if (selected == value) InboxItemType.UNSPECIFIED else value) },
        label = { Text(label) }
    )
}
