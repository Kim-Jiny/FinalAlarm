@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.jiny.finalalarm.ui.missions

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * 카메라 미리보기 + [촬영] 버튼. 촬영 시 Bitmap 반환.
 */
@Composable
fun CameraCapture(
    onCancel: () -> Unit,
    onCapture: (Bitmap) -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("카메라 권한이 필요해요", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("권한 요청")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text("취소") }
        }
        return
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
            executor.shutdown()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    val previewView = PreviewView(c)
                    val future = ProcessCameraProvider.getInstance(c)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                            )
                            imageCapture = capture
                        }.onFailure { error = "카메라 시작 실패: ${it.message}" }
                    }, ContextCompat.getMainExecutor(c))
                    previewView
                },
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onCancel) { Text("취소") }
            Button(
                enabled = imageCapture != null && !capturing,
                onClick = {
                    val ic = imageCapture ?: return@Button
                    capturing = true
                    ic.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val rotation = image.imageInfo.rotationDegrees
                                val raw = image.toBitmap()
                                image.close()
                                val final = if (rotation == 0) raw else {
                                    val m = Matrix().apply { postRotate(rotation.toFloat()) }
                                    val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                                    if (rotated !== raw) raw.recycle()
                                    rotated
                                }
                                onCapture(final)
                                capturing = false
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Timber.e(exception, "ImageCapture error")
                                error = "촬영 실패: ${exception.message}"
                                capturing = false
                            }
                        },
                    )
                },
            ) { Text(if (capturing) "촬영 중…" else "촬영") }
        }
    }
}

