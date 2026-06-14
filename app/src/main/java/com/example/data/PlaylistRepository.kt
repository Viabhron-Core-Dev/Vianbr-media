package com.example.data

import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val playlistDao: PlaylistDao) {
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getPlaylistById(id: Int): Flow<Playlist?> = playlistDao.getPlaylistById(id)

    suspend fun insertPlaylist(playlist: Playlist): Long = playlistDao.insertPlaylist(playlist)

    suspend fun deletePlaylistById(id: Int) = playlistDao.deletePlaylistById(id)

    fun getItemsForPlaylist(playlistId: Int): Flow<List<PlaylistItem>> = playlistDao.getItemsForPlaylist(playlistId)

    suspend fun insertPlaylistItem(item: PlaylistItem) = playlistDao.insertPlaylistItem(item)

    suspend fun deletePlaylistItemById(id: Int) = playlistDao.deletePlaylistItemById(id)
    
    suspend fun deletePlaylistItemByUri(playlistId: Int, mediaUri: String) = playlistDao.deletePlaylistItemByUri(playlistId, mediaUri)

    suspend fun updatePlaylistItem(item: PlaylistItem) = playlistDao.updatePlaylistItem(item)
    
    suspend fun updatePlaylistItems(items: List<PlaylistItem>) = playlistDao.updatePlaylistItems(items)
}
