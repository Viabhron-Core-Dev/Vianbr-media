package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaFolder
import com.example.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)

    private val _mediaFolders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val mediaFolders: StateFlow<List<MediaFolder>> = _mediaFolders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val folders = repository.getMediaFolders()
            _mediaFolders.value = folders
            _isLoading.value = false
        }
    }

    fun scanFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedFolder = repository.getMediaFolder(folderId)
            val currentFolders = _mediaFolders.value.toMutableList()
            val index = currentFolders.indexOfFirst { it.id == folderId }
            if (index != -1) {
                if (updatedFolder != null && updatedFolder.mediaItems.isNotEmpty()) {
                    currentFolders[index] = updatedFolder
                } else {
                    currentFolders.removeAt(index)
                }
                _mediaFolders.value = currentFolders
            }
        }
    }
}
