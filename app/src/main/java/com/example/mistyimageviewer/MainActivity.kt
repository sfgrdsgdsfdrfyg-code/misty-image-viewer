package com.example.mistyimageviewer

import android.os.Bundle
import com.example.mistyimageviewer.ui.theme.MistyImageViewerTheme
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MistyImageViewerTheme { // This theme needs to be defined
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        } else {
            errorMessage?.let {
                Text(text = "Error: $it", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
            }
            imageUrl?.let {
                AsyncImage(
                    model = it,
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

        Button(
            onClick = {
                errorMessage = null // Clear previous errors
                isLoading = true
                coroutineScope.launch {
                    try {
                        val fetchedUrl = fetchMistyImageUrl()
                        if (fetchedUrl.isNullOrEmpty()) {
                            errorMessage = "No image URL received or API returned non-image data."
                        } else {
                            imageUrl = fetchedUrl
                        }
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: "Unknown error occurred during fetch."
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Misty Pic")
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
                    // Check for direct URL
                    if (responseBody.trim().startsWith("http://") || responseBody.trim().startsWith("https://")) {
                        return@withContext responseBody.trim()
                    }
                    // Try to parse img src from HTML
                    val imgSrcMatch = Regex("""img\s+src\s*=\s*["']([^"']+)["']""").find(responseBody)
                    if (imgSrcMatch != null) {
                        return@withContext imgSrcMatch.groupValues[1]
                    }
                    println("API response not a direct image URL or image: $responseBody")
                    return@withContext null
                }
            }
        } else {
            println("HTTP error: $responseCode - ${connection.responseMessage}")
            throw Exception("HTTP error: $responseCode - ${connection.responseMessage}")
        }
    } finally {
        connection?.disconnect()
    }
}