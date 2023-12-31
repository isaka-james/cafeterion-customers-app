package com.udomcive.cafeterion

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LoginActivity : ComponentActivity() {

    // Define constants for URLs
    private val logoUrl = "https://cafeterion.000webhostapp.com/images/categories/logo_login.png"

    // Create coroutine scopes
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        initUI()
        loadOnlineLogo()

        // Add text change listeners to validate input
        usernameEditText.doAfterTextChanged { validateInput() }
        passwordEditText.doAfterTextChanged { validateInput() }

        // Set a click listener for the login button
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            ioScope.launch {
                if (!isInternetAvailable()) {
                    mainScope.launch {
                        showOfflineMessage()
                    }
                    return@launch
                }

                val isAuthenticated = authenticateUser(username, password)

                mainScope.launch {
                    if (isAuthenticated) {
                        navigateToHome()
                    } else {
                        showInvalidCredentialsMessage()
                    }
                }
            }
        }
    }

    private fun initUI() {
        imageView = findViewById(R.id.logo_login)
        usernameEditText = findViewById(R.id.username2)
        passwordEditText = findViewById(R.id.password2)
        loginButton = findViewById(R.id.loginButton)
    }

    private fun loadOnlineLogo() {
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.logo_login)
            .error(R.drawable.logo_login)

        Glide.with(this)
            .load(logoUrl)
            .apply(requestOptions)
            .into(imageView)
    }

    private fun validateInput() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        loginButton.isEnabled = username.isNotEmpty() && password.isNotEmpty()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ?: false
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    private fun showOfflineMessage() {
        Toast.makeText(this, "You are offline!", Toast.LENGTH_LONG).show()
    }

    private fun authenticateUser(username: String, password: String): Boolean {
        val clientAuth = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val encodedUsername = URLEncoder.encode(username, "UTF-8")
        val encodedPassword = URLEncoder.encode(password, "UTF-8")

        val url = "https://cafeterion.000webhostapp.com/auth.php?username=${encodedUsername}&password=${encodedPassword}"

        val requestAuth = Request.Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/json"), ""))
            .build()

        try {
            val responseAuth = clientAuth.newCall(requestAuth).execute()

            return when {
                responseAuth.isSuccessful -> {
                    val responseBody = responseAuth.body()?.string()?.toInt() ?: 0
                    responseAuth.close()
                    if (responseBody > 0) {
                        saveUserId(responseBody)
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        } catch (e: IOException) {
            Log.e("Authentication", "Error: ${e.message}")
            return false
        }
    }

    private fun saveUserId(userId: Int) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putInt("userid", userId)
        editor.apply()
    }

    private fun navigateToHome() {
        val intent = Intent(this@LoginActivity, Home::class.java)
        startActivity(intent)
    }

    private fun showInvalidCredentialsMessage() {
        Toast.makeText(this, "Invalid Password or Username", Toast.LENGTH_LONG).show()
    }
}
