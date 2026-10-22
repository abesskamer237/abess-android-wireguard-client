package com.abessvpn.client

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var importedText by mutableStateOf("")
    private var statusText by mutableStateOf("Prêt — aucun tunnel actif")
    private val importProfile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) contentResolver.openInputStream(uri)?.bufferedReader()?.use {
            importedText = it.readText()
            statusText = runCatching {
                WireGuardProfile.parse(importedText).validate().let { errors ->
                    if (errors.isEmpty()) "Profil syntaxiquement valide (pas encore connecté)" else errors.joinToString("; ")
                }
            }.getOrElse { e -> "Profil invalide : ${e.message}" }
        }
    }
    private val exportProfile = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && importedText.isNotBlank()) contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(importedText) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var apiUrl by remember { mutableStateOf(BuildConfig.DEFAULT_API_BASE_URL) }
                var token by remember { mutableStateOf("") }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ABESS VPN — référence WireGuard", style = MaterialTheme.typography.headlineSmall)
                    Text("Prototype standard. Le transport propriétaire Geny n’est pas implémenté.")
                    OutlinedTextField(apiUrl, { apiUrl = it }, label = { Text("URL API") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(token, { token = it }, label = { Text("Jeton d’enrôlement") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { statusText = "API non appelée dans cette version. Ajouter un client HTTP TLS et un flux d’enrôlement à durée limitée." }, modifier = Modifier.fillMaxWidth()) { Text("Vérifier / demander une configuration") }
                    OutlinedButton(onClick = { importProfile.launch(arrayOf("text/*", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Importer un profil .conf") }
                    OutlinedButton(onClick = { if (importedText.isNotBlank()) exportProfile.launch("abess-wireguard.conf") }, modifier = Modifier.fillMaxWidth()) { Text("Exporter le profil") }
                    Text("État : $statusText")
                    OutlinedTextField(importedText, { importedText = it }, label = { Text("Profil secret — ne pas partager") }, modifier = Modifier.fillMaxWidth().height(220.dp))
                    Button(onClick = {
                        statusText = runCatching {
                            val errors = WireGuardProfile.parse(importedText).validate()
                            if (errors.isEmpty()) "Profil valide, mais moteur WireGuard non intégré." else errors.joinToString("; ")
                        }.getOrElse { e -> "Erreur : ${e.message}" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Valider le profil") }
                }
            }
        }
    }
}
