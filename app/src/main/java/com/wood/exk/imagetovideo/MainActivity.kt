package com.wood.exk.imagetovideo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initLocal()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { MainScreen() }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(listOf<ImageInput>()) }
    var globalPrompt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ارفع صور واكتب وصفًا لكل صورة.") }
    var rendering by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        items = items + uris.map { ImageInput(uri = it) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("صورة + وصف → فيديو ضخم", fontSize = 20.sp)

        OutlinedTextField(
            value = globalPrompt,
            onValueChange = { globalPrompt = it },
            label = { Text("الوصف العام (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch("image/*") }) { Text("إضافة صور") }
            if (items.isNotEmpty()) {
                OutlinedButton(onClick = { items = emptyList() }) { Text("مسح") }
            }
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(items) { idx, it ->
                Card {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("صورة ${idx + 1}", fontSize = 12.sp)
                        OutlinedTextField(
                            value = it.description,
                            onValueChange = { v ->
                                items = items.toMutableList().also { l -> l[idx] = it.copy(description = v) }
                            },
                            label = { Text("وصف هذه الصورة") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }
        }

        Button(
            enabled = items.isNotEmpty() && !rendering,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                rendering = true
                status = "جاري الرندر..."
                Thread {
                    try {
                        val workDir = File(ctx.cacheDir, "w_${UUID.randomUUID()}")
                        workDir.mkdirs()
                        val project = ProjectInput(images = items, globalPrompt = globalPrompt)
                        val out = File(ctx.getExternalFilesDir(null), "vid_${System.currentTimeMillis()}.mp4")

                        val ok = RenderingEngine.renderProject(ctx, project, workDir, out)
                        (ctx as ComponentActivity).runOnUiThread {
                            rendering = false
                            status = if (ok) "تم: ${out.absolutePath}" else "فشل الرندر."
                            if (ok) VideoExporter.exportToGallery(ctx, out)
                        }
                    } catch (e: Exception) {
                        (ctx as ComponentActivity).runOnUiThread {
                            rendering = false
                            status = "خطأ: ${e.message}"
                        }
                    }
                }.start()
            }
        ) {
            Text(if (rendering) "جاري الرندر..." else "أنشئ الفيديو")
        }

        Text(status, fontSize = 12.sp)
    }
}
