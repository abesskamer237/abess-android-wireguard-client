package com.abessvpn.client

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.StringReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var importedText by mutableStateOf("")
    private var statusText by mutableStateOf("Prêt — aucun tunnel actif")
    private var tunnelUp by mutableStateOf(false)

    private val backend by lazy { GoBackend(applicationContext) }
    private val abessTunnel = object : Tunnel {
        override fun getName(): String = "ABESS"
        override fun onStateChange(newState: Tunnel.State) {
            runOnUiThread {
                tunnelUp = newState == Tunnel.State.UP
                statusText = if (tunnelUp) "Tunnel WireGuard actif" else "Tunnel arrêté"
            }
        }
    }

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) connectTunnel()
        else statusText = "Autorisation VPN refusée par Android"
    }

    private val importProfile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Impossible de lire le fichier")
            }.onSuccess { text ->
                importedText = text
                statusText = runCatching {
                    Config.parse(StringReader(text))
                    "Profil WireGuard reconnu — prêt à se connecter"
                }.getOrElse { e -> "Profil invalide : ${e.message ?: "erreur de format"}" }
            }.onFailure { e ->
                statusText = "Import impossible : ${e.message ?: "erreur de lecture"}"
            }
        }
    }

    private val exportProfile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && importedText.isNotBlank()) {
            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(importedText) }
                    ?: error("Impossible d'écrire le fichier")
            }.onFailure { e ->
                statusText = "Export impossible : ${e.message ?: "erreur d'écriture"}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ABESS VPN", style = MaterialTheme.typography.headlineSmall)
                    Text("WireGuard standard — moteur userspace officiel")
                    Text("État : $statusText")

                    OutlinedButton(
                        onClick = { importProfile.launch(arrayOf("text/*", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Importer un profil .conf") }

                    OutlinedButton(
                        onClick = {
                            if (importedText.isNotBlank()) exportProfile.launch("abess-wireguard.conf")
                            else statusText = "Importe d'abord un profil WireGuard"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Exporter le profil") }

                    OutlinedTextField(
                        value = importedText,
                        onValueChange = { importedText = it },
                        label = { Text("Profil WireGuard secret — ne pas partager") },
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )

                    Button(
                        onClick = {
                            val permissionIntent = VpnService.prepare(this@MainActivity)
                            if (permissionIntent != null) vpnPermission.launch(permissionIntent)
                            else connectTunnel()
                        },
                        enabled = !tunnelUp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Se connecter") }

                    OutlinedButton(
                        onClick = { disconnectTunnel() },
                        enabled = tunnelUp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Se déconnecter") }

                    Text(
                        "La connexion nécessite un profil valide et un serveur WireGuard joignable. " +
                            "L'enrôlement API ABESS n'est pas encore relié dans cette version."
                    )
                }
            }
        }
    }

    private fun connectTunnel() {
        val profile = importedText
        if (profile.isBlank()) {
            statusText = "Importe d'abord un profil WireGuard .conf"
            return
        }

        statusText = "Validation du profil et démarrage du tunnel…"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val config = Config.parse(StringReader(profile))
                backend.setState(abessTunnel, Tunnel.State.UP, config)
                withContext(Dispatchers.Main) {
                    tunnelUp = true
                    statusText = "Tunnel activé. Vérifie le handshake et le trafic réel."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tunnelUp = false
                    statusText = "Échec de connexion : ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    private fun disconnectTunnel() {
        statusText = "Arrêt du tunnel…"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                backend.setState(abessTunnel, Tunnel.State.DOWN, null)
                withContext(Dispatchers.Main) {
                    tunnelUp = false
                    statusText = "Tunnel arrêté"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText = "Échec de l'arrêt : ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }
}
