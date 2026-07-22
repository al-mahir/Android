package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.Reciter
import kotlinx.coroutines.flow.Flow
import com.iti.domain.core.Result



 
interface RecitationRepository {
    
    

 
    fun getReciters(): Flow<Result<List<Reciter>>>
    
    


 
    fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>>
}
