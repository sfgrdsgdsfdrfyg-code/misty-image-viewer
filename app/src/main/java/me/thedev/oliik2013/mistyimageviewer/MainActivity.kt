package me.thedev.oliik2013.mistyimageviewer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thedev.oliik2013.mistyimageviewer.ui.theme.MistyImageViewerTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import android.os.Environment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MistyImageViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MistyImageScreen()
                }
            }
        }
    }
}

@Composable
fun MistyImageScreen() {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        } else {
            errorMessage?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Misty Image",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    alignment = Alignment.Center
                )
            } ?: run {
                Text(text = "Click the button to get a Misty image!")
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val fetchedUrl = fetchMistyImageUrl()
                            if (fetchedUrl.isNullOrEmpty()) {
                                errorMessage = "No image URL received"
                            } else {
                                imageUrl = fetchedUrl
                            }
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Misty Pic")
            }

            Button(
                onClick = {
                    imageUrl?.let { url ->
                        coroutineScope.launch {
                            downloadImage(context, url)
                        }
                    } ?: run {
                        Toast.makeText(context, "No image loaded", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = imageUrl != null
            ) {
                Text("Download")
            }
        }
    }
}

private suspend fun fetchMistyImageUrl(): String? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://starnumber.vercel.app/misty?web=1")
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.connect()

        val finalUrl = connection.url.toString()
        val responseCode = connection.responseCode

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val contentType = connection.contentType
            if (contentType != null && contentType.startsWith("image/")) {
                return@withContext finalUrl
            } else {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val responseBody = reader.readText()
                    if (responseBody.trim().startsWith("http://") || responseBody.trim().startsWith("https://")) {
                        return@withContext responseBody.trim()
                    }
                    val imgSrcMatch = Regex("""img\s+src\s*=\s*["']([^"']+)["']""").find(responseBody)
                    if (imgSrcMatch != null) {
                        return@withContext imgSrcMatch.groupValues[1]
                    }
                    return@withContext null
                }
            }
        } else {
            throw Exception("HTTP error: $responseCode")
        }
    } finally {
        connection?.disconnect()
    }
}

private suspend fun downloadImage(context: android.content.Context, imageUrl: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL(imageUrl)
        val connection = url.openConnection() as HttpURLConnection
        val inputStream = connection.inputStream
        val bytes = inputStream.readBytes()
        inputStream.close()
        connection.disconnect()

        val filename = "misty_${System.currentTimeMillis()}.jpg"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, filename)
        file.writeBytes(bytes)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Saved to Downloads/$filename", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
