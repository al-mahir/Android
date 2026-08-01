package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.Reciter
import kotlinx.coroutines.flow.Flow
import com.iti.domain.core.Result



 
interface RecitationRepository {
    
    

 
    fun getReciters(): Flow<Result<List<Reciter>>>
    
    


 
    fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>>
    
    /**
     * Starts downloading recitation for a specific surah, or the full Quran if surahNumber is null.
     */
    suspend fun downloadRecitation(reciterId: Int, surahNumber: Int?)
    
    /**
     * Observes the download progress for a specific reciter.
     */
    fun observeDownloadProgress(reciterId: Int): Flow<List<com.example.mushaf.domain.model.DownloadStatus>>

    /**
     * Cancels an ongoing recitation download.
     */
    fun cancelDownloadRecitation(reciterId: Int, surahNumber: Int?)
}
