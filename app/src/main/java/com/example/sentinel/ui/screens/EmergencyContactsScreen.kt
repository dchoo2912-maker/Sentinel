package com.example.sentinel.ui.screens

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentinel.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(viewModel: ContactsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val blue600 = Color(0xFF2563EB)
    val slate400 = Color(0xFF94A3B8)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Contacts", fontWeight = FontWeight.Bold)
                        Text("${uiState.contacts.size} trusted contacts", color = slate400, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.background(blue600, CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(uiState.contacts) { contact ->
                    ContactCard(
                        name = contact.name,
                        relation = contact.relation,
                        phone = contact.phone,
                        isOnline = contact.isOnline,
                        avatarBrush = Brush.linearGradient(listOf(Color(contact.avatarColorStart), Color(contact.avatarColorEnd))),
                        blue600 = blue600,
                        onDelete = { viewModel.deleteContact(contact.id) }
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { showAddDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, slate400) // Simulating dashed border
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = slate400)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Contact", color = slate400, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, relation, phone, contactUserId ->
                viewModel.addContact(name, relation, phone, contactUserId)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddContactDialog(
    viewModel: ContactsViewModel,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactUserId by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Automatically fill name and email if user is found
    LaunchedEffect(uiState.foundUser) {
        uiState.foundUser?.let {
            name = it.name
            // We keep phone empty as it's not in the User model, 
            // but we could pull email if needed.
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val contactData = getContactDetails(context, it)
            name = contactData.first ?: ""
            phone = contactData.second ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { contactPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.ContactPage, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select from Phonebook")
                }
                
                Text("OR FIND BY SENTINEL ID", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = contactUserId, 
                        onValueChange = { contactUserId = it }, 
                        label = { Text("Sentinel User ID") }, 
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (uiState.isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else if (uiState.foundUser != null) Icon(Icons.Default.Check, null, tint = Color.Green)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.findUserById(contactUserId) },
                        modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.White)
                    }
                }

                if (uiState.foundUser == null && contactUserId.isNotEmpty() && !uiState.isSearching) {
                    // Show warning if search attempted but nothing found
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation (e.g. Spouse)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, relation, phone, contactUserId) }, enabled = name.isNotBlank() && phone.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getContactDetails(context: android.content.Context, uri: Uri): Pair<String?, String?> {
    var name: String? = null
    var phone: String? = null
    
    val contentResolver = context.contentResolver
    val contactCursor = contentResolver.query(uri, null, null, null, null)
    
    if (contactCursor?.moveToFirst() == true) {
        val id = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
        name = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
        
        val hasPhoneNumber = contactCursor.getInt(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
        if (hasPhoneNumber > 0) {
            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(id),
                null
            )
            if (phoneCursor?.moveToFirst() == true) {
                phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
            phoneCursor?.close()
        }
    }
    contactCursor?.close()
    return Pair(name, phone)
}

@Composable
fun ContactCard(
    name: String, 
    relation: String, 
    phone: String, 
    isOnline: Boolean, 
    avatarBrush: Brush, 
    blue600: Color,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.background(avatarBrush, CircleShape).fillMaxSize())
                }
                Surface(
                    modifier = Modifier.size(14.dp).align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = if (isOnline) Color(0xFF16A34A) else Color(0xFF94A3B8),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {}
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(relation, color = Color.Gray, fontSize = 12.sp)
                Text(phone, color = Color.Gray, fontSize = 12.sp)
                Text(
                    if (isOnline) "● Online now" else "● Offline",
                    color = if (isOnline) Color(0xFF16A34A) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column {
                IconButton(onClick = { }, modifier = Modifier.background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)).size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = blue600, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp)).size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
