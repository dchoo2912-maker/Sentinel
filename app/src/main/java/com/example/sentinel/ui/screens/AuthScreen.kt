package com.example.sentinel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.viewmodel.AuthState
import com.example.sentinel.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    var isSignIn by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    // Reset error when switching between sign in and sign up
    LaunchedEffect(isSignIn) {
        viewModel.resetState()
    }

    val blue600 = Color(0xFF2563EB)
    val slate50 = Color(0xFFF8FAFC)
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate900 = Color(0xFF0F172A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(32.dp), color = blue600, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Outlined.Shield, null, modifier = Modifier.padding(6.dp), tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sentinel", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (isSignIn) "Welcome back" else "Create account",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tabs
        TabRow(
            selectedTabIndex = if (isSignIn) 0 else 1,
            containerColor = slate50,
            contentColor = blue600,
            indicator = {},
            divider = {},
            modifier = Modifier.height(48.dp).background(slate50, RoundedCornerShape(24.dp)).padding(4.dp)
        ) {
            Tab(selected = isSignIn, onClick = { isSignIn = true }, modifier = Modifier.background(if (isSignIn) Color.White else Color.Transparent, RoundedCornerShape(20.dp))) {
                Text("Sign In", fontWeight = if (isSignIn) FontWeight.SemiBold else FontWeight.Normal, color = if (isSignIn) slate900 else slate400)
            }
            Tab(selected = !isSignIn, onClick = { isSignIn = false }, modifier = Modifier.background(if (!isSignIn) Color.White else Color.Transparent, RoundedCornerShape(20.dp))) {
                Text("Sign Up", fontWeight = if (!isSignIn) FontWeight.SemiBold else FontWeight.Normal, color = if (!isSignIn) slate900 else slate400)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isSignIn) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Full Name") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = slate50, unfocusedContainerColor = slate50)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Email address") },
            leadingIcon = { Icon(Icons.Outlined.Mail, null) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = slate50, unfocusedContainerColor = slate50)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = slate50, unfocusedContainerColor = slate50)
        )

        if (isSignIn) {
            TextButton(
                onClick = { viewModel.resetPassword(email) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password?", color = blue600)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthState.Error) {
            Text((authState as AuthState.Error).message, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (authState is AuthState.Success) {
            Text((authState as AuthState.Success).message, color = Color(0xFF16A34A), modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = {
                if (isSignIn) viewModel.signIn(email, password)
                else viewModel.signUp(email, password, name)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = slate900),
            shape = RoundedCornerShape(12.dp),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(if (isSignIn) "Sign In" else "Create Account", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text("or continue with", color = slate400, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { 
                    viewModel.signInWithGoogle(context, "171617349163-3qfkgru0uvkoqr73ib97htj13sca4te9.apps.googleusercontent.com")
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, slate200)
            ) {
                Text("Google", color = Color.Black)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
