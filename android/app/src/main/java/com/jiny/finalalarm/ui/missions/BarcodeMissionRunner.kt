@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.jiny.finalalarm.ui.missions

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * CameraX 미리보기 + MLKit BarcodeScanner.
 * - mode: "QR" → QR_CODE 한정. "BARCODE" → 일반 1D 바코드들 (CODE_128, EAN_13 등)
 * - expectedCode가 null이면 어떤 코드든 스캔하면 통과
 * - expectedCode가 있으면 해당 raw value와 일치해야 통과
 */
@Composable
fun BarcodeMissionRunner(
    mode: String,
    expectedCode: String?,
    onComplete: (scannedCode: String) -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

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
        }
        return
    }

    var status by remember { mutableStateOf("스캔 중…") }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val scanner: BarcodeScanner = remember(mode) {
        val opts = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                if (mode == "QR") Barcode.FORMAT_QR_CODE
                else Barcode.FORMAT_CODE_128
                    or Barcode.FORMAT_CODE_39
                    or Barcode.FORMAT_EAN_8
                    or Barcode.FORMAT_EAN_13
                    or Barcode.FORMAT_UPC_A
                    or Barcode.FORMAT_UPC_E,
            )
            .build()
        BarcodeScanning.getClient(opts)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
            executor.shutdown()
            scanner.close()
        }
    }

    var matched by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("$mode 스캔", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        expectedCode?.let {
            Text("기대 값: $it", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    val previewView = PreviewView(c)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(c)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { imageProxy ->
                            processImage(scanner, imageProxy) { value ->
                                if (matched) return@processImage
                                if (expectedCode == null || value == expectedCode) {
                                    matched = true
                                    onComplete(value)
                                } else {
                                    status = "다른 코드: $value"
                                }
                            }
                        }
                        runCatching {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }.onFailure { status = "카메라 시작 실패: ${it.message}" }
                    }, ContextCompat.getMainExecutor(c))
                    previewView
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun processImage(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onValue: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onValue)
        }
        .addOnCompleteListener { imageProxy.close() }
}
