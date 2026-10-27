package io.github.abdulwahab9d.isle.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.abdulwahab9d.isle.IsleHost
import io.github.abdulwahab9d.isle.LocalIsleController
import io.github.abdulwahab9d.isle.SessionHandle
import io.github.abdulwahab9d.isle.TaskResult
import io.github.abdulwahab9d.isle.Tone
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    IsleHost {
                        SampleControls()
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleControls() {
    val isle = LocalIsleController.current
    var recording by remember { mutableStateOf<SessionHandle?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                isle.flash(
                    title = "Copied",
                    description = "On the clipboard",
                    tone = Tone.Success,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Flash")
        }
        Button(
            onClick = {
                isle.task(title = "Translating") {
                    delay(1_000)
                    TaskResult(title = "Done", tone = Tone.Success)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Task")
        }
        Button(
            onClick = {
                recording = isle.session(
                    id = "recording",
                    title = "Recording",
                    description = "Tap the capsule",
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Session")
        }
        Button(
            onClick = { recording?.dismiss() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Dismiss")
        }
    }
}
