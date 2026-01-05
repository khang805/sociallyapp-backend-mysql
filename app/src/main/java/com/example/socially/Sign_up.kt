package com.example.socially

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.socially.api.*
import com.example.socially.auth.SessionManager
import com.example.socially.utils.ApiUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

/**
 * Sign Up Activity
 * Handles user registration via REST API
 */
class Sign_up : AppCompatActivity() {

    private var imageString: String? = null
    private val IMAGE_PICK_CODE = 1000
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.sign_up)

        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)
        
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        val uploadImage = findViewById<ImageView>(R.id.profileImage)
        val usernameField = findViewById<EditText>(R.id.username)
        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val createAccountBtn = findViewById<Button>(R.id.createAccountBtn)

        backArrow.setOnClickListener {
            startActivity(Intent(this@Sign_up, Login_Page::class.java))
            finish()
        }

        uploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        createAccountBtn.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signupUser(username, email, password)
        }
    }

    private fun signupUser(username: String, email: String, password: String) {
        val request = SignupRequest(
            username = username,
            email = email,
            password = password,
            fcm_token = sessionManager.getFcmToken() ?: ""
        )

        RetrofitClient.apiService.signupUser(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()!!

                    // Save user session
                    sessionManager.saveUserSession(
                        userId = data.user_id!!,
                        username = data.username!!,
                        email = data.email!!,
                        authToken = data.token,
                        profileImage = data.profile_picture
                    )

                    Toast.makeText(this@Sign_up, 
                        "Account Created!", Toast.LENGTH_SHORT).show()

                    // If user selected a profile image during signup, upload it now
                    if (!imageString.isNullOrEmpty()) {
                        val updateReq = UpdateProfileRequest(
                            user_id = data.user_id!!,
                            profile_picture_base64 = imageString
                        )

                        RetrofitClient.apiService.updateProfile(updateReq).enqueue(object : Callback<UpdateProfileResponse> {
                            override fun onResponse(
                                call: Call<UpdateProfileResponse>,
                                resp: Response<UpdateProfileResponse>
                            ) {
                                if (resp.isSuccessful && resp.body()?.status == "success") {
                                    // update session with the saved profile picture URL
                                    val picUrl = resp.body()?.profile_picture
                                    picUrl?.let { sessionManager.updateProfileImage(it) }
                                } else {
                                    // Log or inform user but continue to profile setup
                                    val msg = ApiUtils.extractErrorMessage(resp)
                                    Toast.makeText(this@Sign_up, "Profile image upload failed: $msg", Toast.LENGTH_LONG).show()
                                }

                                // Navigate to profile setup regardless
                                // After signup, navigate to Main Feed; Main Feed can prompt for profile setup if needed
                                val intent = Intent(this@Sign_up, Main_feed::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }

                            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                                Toast.makeText(this@Sign_up, "Profile image upload network error: ${t.message}", Toast.LENGTH_LONG).show()
                                val intent = Intent(this@Sign_up, Main_feed::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        })
                    } else {
                        // No image selected, proceed to Main Feed; still marked as first-time in session
                        val intent = Intent(this@Sign_up, Main_feed::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val msg = ApiUtils.extractErrorMessage(response)
                    Toast.makeText(this@Sign_up,
                        "Signup failed: $msg",
                        Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@Sign_up,
                    "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            val uploadImage = findViewById<ImageView>(R.id.profileImage)
            uploadImage.setImageBitmap(bitmap)
            imageString = bitmapToBase64(bitmap)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
