package com.example.sentinel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.domain.EmergencyContact
import com.example.sentinel.domain.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundUser: User? = null,
    val isSearching: Boolean = false
)

class ContactsViewModel : ViewModel() {
    private val firestoreService = FirestoreService()
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()
    private var contactsJob: Job? = null
    private var currentUserId: String = ""

    fun setUser(userId: String) {
        currentUserId = userId
        startFetchingContacts(userId)
    }

    private fun startFetchingContacts(userId: String) {
        contactsJob?.cancel()
        contactsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            firestoreService.getContacts(userId).collect { contacts ->
                _uiState.value = _uiState.value.copy(contacts = contacts, isLoading = false)
            }
        }
    }

    fun findUserById(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, foundUser = null)
            val user = firestoreService.getUserById(userId)
            _uiState.value = _uiState.value.copy(isSearching = false, foundUser = user)
        }
    }

    fun addContact(name: String, relation: String, phone: String, contactUserId: String = "") {
        viewModelScope.launch {
            try {
                firestoreService.saveContact(
                    EmergencyContact(
                        userId = currentUserId,
                        contactUserId = contactUserId,
                        name = name,
                        relation = relation,
                        phone = phone
                    )
                )
                // Clear search after adding
                _uiState.value = _uiState.value.copy(foundUser = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                firestoreService.deleteContact(contactId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
