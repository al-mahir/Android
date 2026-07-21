package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.Reciter
import kotlinx.coroutines.flow.Flow
import com.iti.domain.core.Result

/**
 * Repository for accessing Quran reciters and their recitation timing data.
 */
interface RecitationRepository {
    
    /**
     * Retrieves the catalog of available reciters.
     */
    fun getReciters(): Flow<Result<List<Reciter>>>
    
    /**
     * Retrieves the audio segment timings for a specific page and reciter.
     * This data is used to highlight words synchronously with audio playback.
     */
    fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>>
}
