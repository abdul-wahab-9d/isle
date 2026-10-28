package io.github.abdulwahab9d.isle.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.abdulwahab9d.isle.IsleHost
import io.github.abdulwahab9d.isle.LocalIsleController
import io.github.abdulwahab9d.isle.TaskResult
import io.github.abdulwahab9d.isle.Tone
import io.github.abdulwahab9d.isle.core.IsleMetrics
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    IsleHost {
                        SampleGallery()
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleGallery() {
    val isle = LocalIsleController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Isle", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "The same toasts, born from the notch.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DemoRow("Success") {
            isle.flash(
                title = "Changes saved",
                description = "Your draft is now live.",
                tone = Tone.Success,
            )
        }
        DemoRow("Error") {
            isle.flash(
                title = "Payment failed",
                description = "The card was declined. Try another.",
                tone = Tone.Error,
            )
        }
        DemoRow("Warning") {
            isle.flash(
                title = "Storage almost full",
                description = "Free a little space to keep saving.",
                tone = Tone.Warning,
            )
        }
        DemoRow("Info") {
            isle.flash(
                title = "Update ready",
                description = "A new version is waiting to install.",
                tone = Tone.Info,
            )
        }
        DemoRow("Action") {
            isle.flash(
                title = "File uploaded",
                description = "Share it with your team?",
                tone = Tone.Info,
                actionLabel = "Share",
                onAction = {},
            )
        }
        DemoRow("Promise") {
            isle.task(title = "Saving") {
                delay(1_200)
                TaskResult(
                    title = "Saved",
                    description = "Everything is up to date.",
                    tone = Tone.Success,
                )
            }
        }
        DemoRow("Boarding") {
            isle.flash(
                title = "Boarding",
                tone = Tone.Success,
                expanded = { BoardingCard() },
            )
        }
    }
}

@Composable
private fun DemoRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun BoardingCard() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Airport(code = "DEL", city = "Delhi")
            Text("—", color = Color.White.copy(alpha = 0.35f))
            Airport(code = "SFO", city = "San Francisco", alignEnd = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "On time",
                color = Color(IsleMetrics.SUCCESS_ARGB),
                fontWeight = FontWeight.Medium,
            )
            Text("Seat 32A", color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun Airport(code: String, city: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(code, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        Text(city, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
    }
}
