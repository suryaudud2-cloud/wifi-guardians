package id.wifiguardian.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.content.Intent

class NetworkCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val valid = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        if (!wifi || !valid) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                applicationContext, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, "wifi_alerts")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("WiFi Guardian: koneksi bermasalah")
                .setContentText("Buka aplikasi untuk memeriksa koneksi Wi-Fi.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            try { NotificationManagerCompat.from(applicationContext).notify(1002, notification) } catch (_: SecurityException) { }
        }
        return Result.success()
    }
}
