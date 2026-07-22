package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.repository.LiveRecitationRepository
import kotlinx.coroutines.flow.Flow








 
class StartLiveRecitationUseCase(
    private val repository: LiveRecitationRepository,
) {
    operator fun invoke(
        config: LiveRecitationConfig,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent> = repository.session(config, controls)

     
    operator fun invoke(
        from: RecitationCursor,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent> = repository.session(LiveRecitationConfig(start = from), controls)
}
