@file:OptIn(ExperimentalMaterial3Api::class)

package com.dealwatch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddProductScreen(
    onSave: (query: String, targetPrice: Double?) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track a product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("What do you want?") },
                placeholder = { Text("e.g. Sony WH-1000XM5 headphones") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Be specific — brand and model give the best price matches.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )

            OutlinedTextField(
                value = target,
                onValueChange = { input -> target = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Target price (optional)") },
                placeholder = { Text("e.g. 299") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            Text(
                "You'll always be alerted on a new all-time low. A target is just a " +
                    "reminder of the price you're hoping for.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )

            Button(
                onClick = { onSave(query.trim(), target.toDoubleOrNull()) },
                enabled = query.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text("Start tracking")
            }
        }
    }
}
