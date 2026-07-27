package com.example.mushaf.data.download

import com.example.mushaf.domain.model.DownloadState
import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.LocalizedText
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.repository.DownloadableResourceRepository
import com.iti.domain.core.Result
import com.iti.domain.core.asResult
import com.iti.domain.core.resultOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus


class FakeDownloadableResourceRepository(
    scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DownloadableResourceRepository {

    private val downloadScope = scope + SupervisorJob()

    private val resources = MutableStateFlow(SEED_RESOURCES)

    private val jobs = mutableMapOf<String, Job>()

    override fun observeResources(kind: ResourceKind): Flow<Result<List<DownloadableResource>>> =
        resources.map { all -> all.filter { it.kind == kind } }.asResult()

    override suspend fun startDownload(id: String): Result<Unit> = resultOf {
        if (jobs[id]?.isActive == true) return@resultOf

        jobs[id] = downloadScope.launch {
            try {
                for (step in 1..PROGRESS_STEPS) {
                    delay(STEP_MILLIS)
                    updateState(id, DownloadState.Downloading(step.toFloat() / PROGRESS_STEPS))
                }
                updateState(id, DownloadState.Downloaded)
            } finally {
                jobs.remove(id)
            }
        }
    }

    override suspend fun cancelDownload(id: String): Result<Unit> = resultOf {
        jobs.remove(id)?.cancel()
        updateState(id, DownloadState.NotDownloaded)
    }

    override suspend fun deleteDownload(id: String): Result<Unit> = resultOf {
        jobs.remove(id)?.cancel()
        updateState(id, DownloadState.NotDownloaded)
    }

    private fun updateState(id: String, state: DownloadState) {
        resources.update { all ->
            all.map { if (it.id == id) it.copy(state = state) else it }
        }
    }

    private companion object {
        const val PROGRESS_STEPS = 20
        const val STEP_MILLIS = 180L

        const val MB = 1024L * 1024L

        val SEED_RESOURCES: List<DownloadableResource> = listOf(
            
            DownloadableResource(
                id = "reciter_husary",
                kind = ResourceKind.RECITER,
                name = LocalizedText("محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
                subtitle = LocalizedText("حفص عن عاصم", "Hafs 'an 'Asim"),
                sizeBytes = 820 * MB,
                state = DownloadState.Downloaded,
            ),
            DownloadableResource(
                id = "reciter_minshawi",
                kind = ResourceKind.RECITER,
                name = LocalizedText("محمد صديق المنشاوي", "Mohamed Siddiq Al-Minshawi"),
                subtitle = LocalizedText("حفص عن عاصم", "Hafs 'an 'Asim"),
                sizeBytes = 760 * MB,
            ),
            DownloadableResource(
                id = "reciter_abdulbasit",
                kind = ResourceKind.RECITER,
                name = LocalizedText("عبد الباسط عبد الصمد", "Abdul Basit Abdus Samad"),
                subtitle = LocalizedText("ورش عن نافع", "Warsh 'an Nafi'"),
                sizeBytes = 910 * MB,
            ),
            DownloadableResource(
                id = "reciter_sudais",
                kind = ResourceKind.RECITER,
                name = LocalizedText("عبد الرحمن السديس", "Abdurrahman As-Sudais"),
                subtitle = LocalizedText("حفص عن عاصم", "Hafs 'an 'Asim"),
                sizeBytes = 680 * MB,
            ),
            DownloadableResource(
                id = "reciter_shatri",
                kind = ResourceKind.RECITER,
                name = LocalizedText("أبو بكر الشاطري", "Abu Bakr Ash-Shatri"),
                subtitle = LocalizedText("قالون عن نافع", "Qalun 'an Nafi'"),
                sizeBytes = 705 * MB,
            ),

            
            DownloadableResource(
                id = "tafseer_muyassar",
                kind = ResourceKind.TAFSEER,
                name = LocalizedText("التفسير الميسر", "Al-Tafsir Al-Muyassar"),
                sizeBytes = 12 * MB,
                state = DownloadState.Downloaded,
            ),
            DownloadableResource(
                id = "tafseer_ibn_kathir",
                kind = ResourceKind.TAFSEER,
                name = LocalizedText("تفسير ابن كثير", "Tafsir Ibn Kathir"),
                sizeBytes = 48 * MB,
            ),
            DownloadableResource(
                id = "tafseer_saadi",
                kind = ResourceKind.TAFSEER,
                name = LocalizedText("تفسير السعدي", "Tafsir As-Sa'di"),
                sizeBytes = 21 * MB,
            ),
            DownloadableResource(
                id = "tafseer_qurtubi",
                kind = ResourceKind.TAFSEER,
                name = LocalizedText("تفسير القرطبي", "Tafsir Al-Qurtubi"),
                sizeBytes = 63 * MB,
            ),

            
            DownloadableResource(
                id = "translation_en_sahih",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("صحيح انترناشونال (إنجليزي)", "Sahih International (English)"),
                sizeBytes = 8 * MB,
            ),
            DownloadableResource(
                id = "translation_fr_hamidullah",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("حميد الله (فرنسي)", "Hamidullah (French)"),
                sizeBytes = 9 * MB,
            ),
            DownloadableResource(
                id = "translation_ur_junagarhi",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("جوناكرهي (أردو)", "Junagarhi (Urdu)"),
                sizeBytes = 10 * MB,
            ),
            DownloadableResource(
                id = "translation_id_affairs",
                kind = ResourceKind.TRANSLATION,
                name = LocalizedText("الشؤون الإسلامية (إندونيسي)", "Islamic Affairs (Indonesian)"),
                sizeBytes = 7 * MB,
            ),
        )
    }
}
