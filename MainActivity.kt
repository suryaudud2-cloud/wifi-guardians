package id.wifiguardian.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private val Bg = Color(0xFF080D18)
private val Panel = Color(0xFF121C2D)
private val Cyan = Color(0xFF35D7F2)
private val Muted = Color(0xFF9BAAC0)

class MainActivity : ComponentActivity() {
    private lateinit var connectivityManager: ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            var monitoring by remember { mutableStateOf(false) }
            var wifiConnected by remember { mutableStateOf(isWifiConnected()) }
            var internetOk by remember { mutableStateOf(isInternetValidated()) }
            var statusText by remember { mutableStateOf("Siap memantau") }

            DisposableEffect(monitoring) {
                if (monitoring) {
                    val cb = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            wifiConnected = isWifiConnected()
                            internetOk = isInternetValidated()
                            statusText = if (internetOk) "Koneksi kembali normal" else "Jaringan terhubung, internet bermasalah"
                            if (!internetOk) showProblemNotification()
                        }
                        override fun onLost(network: Network) {
                            wifiConnected = isWifiConnected()
                            internetOk = isInternetValidated()
                            statusText = "Koneksi jaringan terputus"
                            showProblemNotification()
                        }
                        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                            internetOk = validated
                            wifiConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            if (!validated) {
                                statusText = "Internet tidak tervalidasi"
                                showProblemNotification()
                            } else {
                                statusText = "Koneksi normal"
                            }
                        }
                    }
                    callback = cb
                    try { connectivityManager.registerDefaultNetworkCallback(cb) } catch (_: Exception) { }
                }
                onDispose {
                    callback?.let { try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) { } }
                    callback = null
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Cyan,
                    background = Bg,
                    surface = Panel,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = Cyan, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("WIFI GUARDIAN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text("SMART NETWORK CONTROL", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("STATUS KONEKSI", color = Muted, style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (wifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                        null, tint = if (wifiConnected) Cyan else Color(0xFFFF6B75),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(if (wifiConnected) "Wi-Fi terhubung" else "Wi-Fi tidak terhubung", fontWeight = FontWeight.Bold)
                                        Text(if (internetOk) "Internet tersedia" else "Internet bermasalah / tidak tervalidasi", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF26344A))
                                Text(statusText, color = if (internetOk) Cyan else Color(0xFFFFA4AA))
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("AUTO MONITOR", fontWeight = FontWeight.Bold)
                                Text(
                                    "Pantau putusnya koneksi dan internet yang tidak tervalidasi. Saat gangguan terdeteksi, aplikasi memberi notifikasi dan menyediakan tombol ke panel konektivitas Android.",
                                    color = Muted, style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = {
                                        monitoring = !monitoring
                                        statusText = if (monitoring) "Pemantauan aktif selama aplikasi terbuka" else "Pemantauan dihentikan"
                                        if (monitoring) scheduleBackgroundCheck(this@MainActivity)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (monitoring) Color(0xFF8D3440) else Cyan)
                                ) {
                                    Text(if (monitoring) "HENTIKAN PEMANTAUAN" else "MULAI PEMANTAUAN", color = if (monitoring) Color.White else Bg, fontWeight = FontWeight.Bold)
                                }
                                Text(if (monitoring) "● AKTIF — pemantauan real-time saat layar aplikasi aktif" else "○ NONAKTIF", color = if (monitoring) Cyan else Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = { openInternetPanel() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Panel),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(Modifier.width(8.dp))
                            Text("BUKA KONTROL WI-FI ANDROID")
                        }

                        Spacer(Modifier.weight(1f))
                        Text(
                            "Catatan Android 10+: aplikasi biasa tidak diizinkan mematikan Wi-Fi secara otomatis. Tombol kontrol membuka panel sistem agar pengguna dapat memutus Wi-Fi secara manual.",
                            color = Muted, style = MaterialTheme.typography.labelSmall
                        )
                        Text("WiFi Guardian • v1.0.0", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isInternetValidated(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun openInternetPanel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            } else {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("wifi_alerts", "Peringatan Wi-Fi", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showProblemNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = android.app.PendingIntent.getActivity(this, 1, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "wifi_alerts")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("WiFi Guardian mendeteksi gangguan")
            .setContentText("Periksa jaringan atau buka kontrol Wi-Fi Android.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(1001, notification)
    }

    private fun scheduleBackgroundCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<NetworkCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "wifi_guardian_periodic_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
