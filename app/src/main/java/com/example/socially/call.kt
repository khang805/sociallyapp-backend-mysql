package com.example.socially

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class call : AppCompatActivity() {

    private val appId = "3d164bd129a14bd4a8fc5e9f448d641f" // ✅ Replace with your Agora App ID
    private lateinit var channelName: String
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private var rtcEngine: RtcEngine? = null
    private lateinit var callerNameText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)

        // Get channel name and other user info from intent
        channelName = intent.getStringExtra("channel_name") ?: "testChannel"
        val otherUsername = intent.getStringExtra("other_username") ?: "User"
        
        callerNameText = findViewById(R.id.caller_name)
        callerNameText.text = otherUsername

        if (checkSelfPermission()) {
            initializeAgoraEngine()
        }

        findViewById<TextView>(R.id.end).setOnClickListener {
            endCall()
        }
    }

    private fun checkSelfPermission(): Boolean {
        for (permission in REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAgoraEngine()
            } else {
                Toast.makeText(this, "Permissions denied!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(this, appId, object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    android.util.Log.d("AgoraCall", "User joined: $uid")
                    runOnUiThread { 
                        setupRemoteVideo(uid)
                        Toast.makeText(this@call, "User connected", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    android.util.Log.d("AgoraCall", "User offline: $uid, reason: $reason")
                    runOnUiThread { 
                        removeRemoteVideo()
                        Toast.makeText(this@call, "User disconnected", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    android.util.Log.d("AgoraCall", "Joined channel: $channel with uid: $uid")
                    runOnUiThread {
                        Toast.makeText(this@call, "Connected to call", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(err: Int) {
                    android.util.Log.e("AgoraCall", "Agora error: $err")
                    runOnUiThread {
                        Toast.makeText(this@call, "Call error: $err", Toast.LENGTH_SHORT).show()
                    }
                }
            })

            rtcEngine?.enableVideo()
            rtcEngine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                )
            )

            setupLocalVideo()
            joinChannel()
        } catch (e: Exception) {
            android.util.Log.e("AgoraCall", "Failed to initialize Agora: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)
        val surfaceView = SurfaceView(this)
        container.addView(surfaceView)
        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews() // clear old video if any
        val surfaceView = SurfaceView(this)
        container.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    private fun removeRemoteVideo() {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        container.removeAllViews()
    }

    private fun joinChannel() {
        android.util.Log.d("AgoraCall", "Joining channel: $channelName")
        val options = ChannelMediaOptions()
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        val result = rtcEngine?.joinChannel(null, channelName, 0, options)
        android.util.Log.d("AgoraCall", "Join channel result: $result")
    }

    private fun endCall() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        finish()
    }
}
