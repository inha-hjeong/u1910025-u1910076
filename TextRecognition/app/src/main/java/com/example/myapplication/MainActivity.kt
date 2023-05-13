package com.example.myapplication

import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.rememberImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.material.*
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf



class MainActivity : ComponentActivity() {
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

    private lateinit var photoUri: Uri
    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("elina", "Permission granted")
            shouldShowCamera.value = true
        } else {
            Log.i("elina", "Permission denied")
        }
    }
    private var imageUri = mutableStateOf<Uri?>(null)
    private var textChanged = mutableStateOf("Text...")

    override fun onCreate(savedInstanceState: Bundle?) {
        requestCameraPermission()
//        if (OpenCVLoader.initDebug()) {
//            Log.d("yes", "OpenCv configured successfully")
//        } else {
//            Log.d("oh, no", "OpenCv configuration failed")
//        }
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    content = { MainScreen(this) }
                )
            }
        }
    }

    //instance of text recognizer
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val selectImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            imageUri.value = uri
        }

    private fun shareText(sharedText: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, sharedText)
        sendIntent.type = "text/plain"
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    @Composable
    fun MainScreen(context: ComponentActivity) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(3f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        modifier = Modifier.align(Alignment.BottomStart),
                        onClick = {
                            selectImage.launch("image/*")
                        }) {
                        Icon(
                            Icons.Filled.Add,
                            "add",
                            tint = Color.Green
                        )
                    }

                    if (imageUri.value != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = rememberImagePainter(
                                data = imageUri.value
                            ),
                            contentDescription = "image"
                        )
                        IconButton(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onClick = {
                                val image = InputImage.fromFilePath(context, imageUri.value!!)
                                recognizer.process(image)
                                    .addOnSuccessListener {
                                        textChanged.value = it.text
                                    }
                                    .addOnFailureListener {
                                        Log.e("TEXT_REC", it.message.toString())
                                    }
                            }) {
                            Icon(
                                Icons.Filled.Search,
                                "scan",
                                tint = Color.Green
                            )
                        }
                    }

                }
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        text = textChanged.value,
                        color = Color.White
                    )
//                    IconButton(
//                        modifier = Modifier.align(Alignment.BottomEnd),
//                        onClick = {
//                            shareText(textChanged.value)
//                        }) {
//                        Icon(
//                            Icons.Filled.Share,
//                            "share",
//                            tint = Color.Green
//                        )
//                    }
                }
            }
        }
    }
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("elina", "Permission previously granted")
                shouldShowCamera.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("elina", "Show camera permissions dialog")

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    private fun handleImageCapture(uri: Uri) {
        Log.i("elina", "Image captured: $uri")
        shouldShowCamera.value = false

        photoUri = uri
        shouldShowPhoto.value = true
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
