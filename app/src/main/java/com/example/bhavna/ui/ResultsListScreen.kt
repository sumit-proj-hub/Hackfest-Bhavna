package com.example.bhavna.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.bhavna.ui.theme.TelegrafFont
import com.example.bhavna.ui.theme.darkGreen
import com.example.bhavna.ui.theme.orangeColor
import com.example.bhavna.viewmodel.MediaType
import com.example.bhavna.viewmodel.ResultsListViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsListAppBar(
    selectionModeActive: Boolean,
    showDeleteDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = "Bhavna",
                fontWeight = FontWeight.Bold,
                fontFamily = TelegrafFont,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        actions = {
            if (selectionModeActive) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = showDeleteDialog)
                )
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
    )
}

@Composable
fun ResultsListScreen(
    navigateToResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsListViewModel = viewModel(),
) {
    val context = LocalContext.current
    var isMediaOptionsVisible by remember { mutableStateOf(false) }
    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var onTitleEntered: (String?) -> Unit by remember { mutableStateOf({}) }
    val onCaptureHandler = { success: Boolean, mediaType: MediaType ->
        isMediaOptionsVisible = false
        if (success) {
            onTitleEntered = { title ->
                if (title != null)
                    viewModel.onMediaCapture(
                        context = context,
                        mediaType = mediaType,
                        title = title
                    )
            }
            viewModel.changeTitleDialogState(true)
        } else {
            Toast.makeText(context, "Media capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) {
        isMediaOptionsVisible = false
        if (it == null)
            return@rememberLauncherForActivityResult
        onTitleEntered = { title ->
            if (title != null)
                viewModel.onMediaPicked(context, it, title)
        }
        viewModel.changeTitleDialogState(true)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        onCaptureHandler(it, MediaType.Image)
    }
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) {
        onCaptureHandler(it, MediaType.Video)
    }

    if (!uiState.isInitialized) {
        viewModel.initializeState(context)
    }

    Scaffold(topBar = {
        ResultsListAppBar(
            selectionModeActive = uiState.selectionModeActive,
            showDeleteDialog = { isDeleteDialogOpen = true }
        )
    }, modifier = modifier) {
        Surface(
            Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        isMediaOptionsVisible = false
                    }
            ) {
                if (uiState.results.isEmpty()) {
                    Text(
                        text = "Click the plus button below to take a photo or video",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.results.toList()) { result ->
                            ResultEntry(
                                viewModel = viewModel,
                                result = result.second,
                                resultKey = result.first,
                                selectionModeActive = uiState.selectionModeActive,
                                navigateToResult = navigateToResult
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { isMediaOptionsVisible = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
                MediaOptionsChooser(
                    onImageClick = { viewModel.requestMediaCapture(context, takePicture) },
                    onVideoClick = { viewModel.requestMediaCapture(context, takeVideo) },
                    onGalleryClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    },
                    if (isMediaOptionsVisible) Modifier
                        .height(IntrinsicSize.Max)
                        .align(Alignment.BottomStart)
                    else Modifier
                        .height(0.dp)
                        .align(Alignment.BottomStart)
                )
                if (uiState.isEnterTitleDialogOpen)
                    AskTitle(
                        changeDialogState = viewModel::changeTitleDialogState,
                        onTitleEntered = onTitleEntered
                    )
                if (isDeleteDialogOpen)
                    ConfirmDelete(
                        onConfirm = {
                            viewModel.deleteSelectedResults()
                            isDeleteDialogOpen = false
                        },
                        onDismiss = { isDeleteDialogOpen = false }
                    )
            }
        }
    }
}

@Composable
private fun IconButton(
    imageVector: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = imageVector,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape
                )
                .padding(24.dp)
                .size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LocalContentColor.current
        )
    }
}

@Composable
private fun MediaOptionsChooser(
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 0.dp,
            bottomStart = 0.dp
        ),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 500))
            .clickable(indication = null, interactionSource = remember {
                MutableInteractionSource()
            }) { }
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            IconButton(
                imageVector = Icons.Default.PhotoCamera,
                title = "Take Image",
                onClick = onImageClick
            )
            IconButton(
                imageVector = Icons.Default.Videocam,
                title = "Take Video",
                onClick = onVideoClick
            )
            IconButton(
                imageVector = Icons.Default.Photo,
                title = "Gallery",
                onClick = onGalleryClick
            )
        }
    }
}

@Composable
private fun AskTitle(
    changeDialogState: (Boolean) -> Unit,
    onTitleEntered: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        title = {
            Text(
                "Enter Title",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Enter Title") }
            )
        },
        onDismissRequest = { changeDialogState(false) },
        confirmButton = {
            TextButton(onClick = {
                val trimTitle = title.trim()
                if (trimTitle != "") {
                    changeDialogState(false)
                    onTitleEntered(trimTitle)
                }
            }) {
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = { changeDialogState(false) }) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ResultEntry(
    viewModel: ResultsListViewModel,
    result: Result,
    resultKey: Int,
    selectionModeActive: Boolean,
    navigateToResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val showRipple = { offset: Offset ->
        MainScope().launch {
            val press = PressInteraction.Press(offset)
            interactionSource.emit(press)
            delay(100)
            interactionSource.emit(PressInteraction.Release(press))
        }
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = if (result.isSelected) 1f else .4f
        ),
        modifier = modifier
            .padding(8.dp)
            .indication(interactionSource, rememberRipple())
            .pointerInput(listOf(result.isSelected, selectionModeActive, result.uploadStatus)) {
                detectTapGestures(
                    onTap = {
                        if (selectionModeActive) {
                            viewModel.changeResultSelection(resultKey, !result.isSelected)
                            return@detectTapGestures
                        }
                        showRipple(it)
                        if (result.uploadStatus > 100)
                            navigateToResult(result.resultDir.name)
                    },
                    onLongPress = {
                        viewModel.changeResultSelection(resultKey, !result.isSelected)
                        showRipple(it)
                    }
                )
            }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = Result.getMediaFile(result.resultDir),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .weight(1f)
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatMilliseconds(result.timestamp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                when {
                    result.uploadStatus > 100 -> Text(
                        text = "Status: Ready",
                        color = darkGreen,
                        style = MaterialTheme.typography.titleMedium
                    )

                    result.uploadStatus == 100 -> Text(
                        text = "Status: Processing",
                        color = orangeColor,
                        style = MaterialTheme.typography.titleMedium
                    )

                    result.uploadStatus < 0 -> Text(
                        text = "Status: Failed",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium
                    )

                    else -> Text(
                        text = "Status: Uploading (${result.uploadStatus}%)",
                        color = orangeColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            when {
                result.uploadStatus > 100 -> Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Done",
                    tint = darkGreen,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(12.dp)
                )

                result.uploadStatus < 0 -> Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Retry",
                    tint = Color.Yellow,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(12.dp)
                        .scale(scaleX = -1f, scaleY = 1f)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.uploadFile(
                                resultKey,
                                result.resultDir,
                                result.mediaType
                            )
                        }
                )

                else -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmDelete(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text("Confirm Delete") },
        text = {
            Text(
                text = "Are you sure you want to delete these entries?",
                style = MaterialTheme.typography.titleMedium
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No", style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

private fun formatMilliseconds(milliseconds: Long, pattern: String = "dd MMM yyyy, HH:mm"): String {
    val date = Date(milliseconds)
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}