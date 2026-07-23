package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationControl
import kotlinx.coroutines.flow.Flow







 
interface LiveRecitationRepository {

    








 
    fun session(
        config: LiveRecitationConfig,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent>
}
