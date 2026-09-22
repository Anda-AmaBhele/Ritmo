package com.example.ritmo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritmo.R
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // --- Google Sign-In setup ---
    val googleSignInClient = remember {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                isLoading = false
                if (authResult.isSuccessful) {
                    onLoginSuccess()
                } else {
                    errorMessage = authResult.exception?.localizedMessage
                        ?: "Google sign-in failed. Please try again."
                }
            }
        } catch (e: ApiException) {
            errorMessage = "Google sign-in was cancelled or failed."
        }
    }

    fun submit() {
        errorMessage = null
        infoMessage = null

        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in both fields."
            return
        }

        isLoading = true
        val task = if (isRegisterMode) {
            auth.createUserWithEmailAndPassword(email.trim(), password)
        } else {
            auth.signInWithEmailAndPassword(email.trim(), password)
        }

        task.addOnCompleteListener { result ->
            isLoading = false
            if (result.isSuccessful) {
                if (isRegisterMode) {
                    // Send a free email verification link instead of paid SMS 2FA
                    auth.currentUser?.sendEmailVerification()
                    infoMessage = "Account created! Check your email to verify it."
                } else {
                    onLoginSuccess()
                }
            } else {
                errorMessage = result.exception?.localizedMessage
                    ?: "Something went wrong. Please try again."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RitmoCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            RitmoLogo(modifier = Modifier.height(48.dp))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ritmo",
                fontSize = 44.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = RitmoSage
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRegisterMode) "Create your account" else "Welcome back",
                fontSize = 16.sp,
                color = RitmoGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RitmoSage,
                    unfocusedBorderColor = RitmoGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RitmoSage,
                    unfocusedBorderColor = RitmoGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 13.sp
                )
            }

            if (infoMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = infoMessage ?: "",
                    color = RitmoSage,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submit() },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RitmoSage),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = RitmoCream,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isRegisterMode) "Register" else "Log in")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = {
                isRegisterMode = !isRegisterMode
                errorMessage = null
                infoMessage = null
            }) {
                Text(
                    text = if (isRegisterMode) {
                        "Already have an account? Log in"
                    } else {
                        "New here? Register"
                    },
                    color = RitmoBlack
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = RitmoGray)
                Text(
                    text = "  or  ",
                    color = RitmoGray,
                    fontSize = 13.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = RitmoGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    errorMessage = null
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Continue with Google", color = RitmoBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}
