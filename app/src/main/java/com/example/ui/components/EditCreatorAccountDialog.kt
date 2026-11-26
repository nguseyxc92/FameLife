package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CreatorProfileEntity
import com.example.data.ProfilePhotoStorage
import com.example.ui.theme.FameMint
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary

@Composable
fun EditCreatorAccountDialog(
    profile: CreatorProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, handle: String, bio: String, avatarUri: String?, isVerified: Boolean) -> Unit
) {
    val context = LocalContext.current
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var handle by remember(profile) { mutableStateOf(profile?.handle ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var avatarUri by remember(profile) { mutableStateOf(profile?.avatarUri) }
    var isVerified by remember(profile) { mutableStateOf(profile?.isVerified == true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = ProfilePhotoStorage.savePickedPhotoToInternalStorage(context, uri)
            if (saved != null) {
                avatarUri = saved
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = { Text("Creator Account") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ViewerAvatar(
                        initials = displayName.ifBlank { "CR" }.take(2).uppercase(),
                        seed = 17,
                        size = 64.dp,
                        hasHighlightRing = true,
                        hasProfilePicture = !avatarUri.isNullOrBlank(),
                        imageUri = avatarUri,
                        modifier = Modifier.clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FameMint.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("account_pick_photo_btn")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = FameMint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (avatarUri.isNullOrBlank()) "Add Profile Photo" else "Change Photo",
                                color = FameMint
                            )
                        }
                        if (!avatarUri.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    ProfilePhotoStorage.clearSavedPhoto(context)
                                    avatarUri = null
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LiveCrimson, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Remove Photo", color = LiveCrimson, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name") },
                    placeholder = { Text("Creator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Handle") },
                    placeholder = { Text("famelife_creator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_handle_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    placeholder = { Text("Late night chats, gaming & music") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("account_bio_input")
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioObsidian.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isVerified = !isVerified }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            VerifiedBadgeIcon(size = 20.dp)
                            Column {
                                Text("Verification Badge", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Show blue verified check while live",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isVerified,
                            onCheckedChange = { isVerified = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = StudioObsidian, checkedTrackColor = FameMint),
                            modifier = Modifier.testTag("account_verified_switch")
                        )
                    }
                }
                Text(
                    text = "Saved privately on your device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(displayName, handle, bio, avatarUri, isVerified)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FameMint, contentColor = StudioObsidian),
                modifier = Modifier.testTag("account_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
