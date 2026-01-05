package com.example.socially

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Screenshot Alerts Activity - Shows screenshot notifications
 */
class ScreenshotAlertsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private val alerts = mutableListOf<ScreenshotAlert>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screenshot_alerts)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        recyclerView = findViewById(R.id.alertsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadScreenshotAlerts()
    }

    private fun loadScreenshotAlerts() {
        RetrofitClient.apiService.getScreenshotAlerts(
            userId = sessionManager.getUserId(),
            lastNotificationId = 0
        ).enqueue(object : Callback<ScreenshotAlertsResponse> {
            override fun onResponse(call: Call<ScreenshotAlertsResponse>, response: Response<ScreenshotAlertsResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    alerts.clear()
                    alerts.addAll(response.body()!!.alerts)
                    
                    if (alerts.isEmpty()) {
                        Toast.makeText(this@ScreenshotAlertsActivity, "No screenshot alerts", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ScreenshotAlertsActivity, "${alerts.size} alerts loaded", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ScreenshotAlertsResponse>, t: Throwable) {
                Toast.makeText(this@ScreenshotAlertsActivity, "Failed to load alerts", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
