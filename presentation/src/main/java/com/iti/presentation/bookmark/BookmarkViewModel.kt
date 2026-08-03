package com.iti.presentation.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.domain.usecase.GetAyahTextUseCase
import com.example.mushaf.domain.usecase.GetTargetPageUseCase
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.iti.domain.model.Bookmark
import com.iti.domain.model.BookmarkType
import com.iti.domain.usecase.bookmark.ObserveAllBookmarksUseCase
import com.iti.domain.usecase.bookmark.RemoveBookmarkUseCase
import com.iti.domain.usecase.sheikh.GetSheikhByIdUseCase
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val observeAllBookmarks: ObserveAllBookmarksUseCase,
    private val removeBookmark: RemoveBookmarkUseCase,
    private val getTargetPage: GetTargetPageUseCase,
    private val getAyahText: GetAyahTextUseCase,
    private val getSheikhById: GetSheikhByIdUseCase,
) : ViewModel(),
    StateHolder<BookmarkState> by DefaultStateHolder(BookmarkState()),
    EffectPublisher<BookmarkEffect> by DefaultEffectPublisher() {

    private var resolveJob: Job? = null

    init {
        observeAllBookmarks()
            .onEach { result ->
                when (result) {
                    is Result.Success -> resolveAndUpdate(result.data)
                    is Result.Error -> updateState { copy(isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: BookmarkIntent) {
        when (intent) {
            is BookmarkIntent.SelectTab -> updateState { copy(selectedTab = intent.tab) }
            is BookmarkIntent.SearchQueryChanged -> updateState { copy(searchQuery = intent.query) }
            is BookmarkIntent.RequestRemove -> updateState { copy(pendingRemoval = intent.item) }
            BookmarkIntent.CancelRemove -> updateState { copy(pendingRemoval = null) }
            BookmarkIntent.ConfirmRemove -> confirmRemove()
            is BookmarkIntent.OpenBookmark -> open(intent.item)
        }
    }

    private fun confirmRemove() {
        val item = currentState.pendingRemoval ?: return
        viewModelScope.launch {
            removeBookmark(item.bookmarkId)
            updateState { copy(pendingRemoval = null) }
        }
    }

    private fun open(item: BookmarkListItem) {
        when (item) {
            is BookmarkListItem.SurahItem -> item.startPage?.let { sendEffect(BookmarkEffect.OpenMushafAtPage(it)) }
            is BookmarkListItem.AyahItem -> item.page?.let { sendEffect(BookmarkEffect.OpenMushafAtPage(it)) }
            is BookmarkListItem.PageItem -> sendEffect(BookmarkEffect.OpenMushafAtPage(item.pageNumber))
            is BookmarkListItem.SheikhItem -> sendEffect(BookmarkEffect.OpenSheikhDetails(item.sheikhId))
        }
    }

    private fun resolveAndUpdate(bookmarks: List<Bookmark>) {
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }
            val resolved = coroutineScope {
                bookmarks.map { bookmark -> async { resolve(bookmark) } }.awaitAll()
            }.filterNotNull()
            updateState { copy(items = resolved, isLoading = false) }
        }
    }

    private suspend fun resolve(bookmark: Bookmark): BookmarkListItem? = when (bookmark.type) {
        BookmarkType.SURAH -> resolveSurah(bookmark)
        BookmarkType.AYAH -> resolveAyah(bookmark)
        BookmarkType.PAGE -> resolvePage(bookmark)
        BookmarkType.SHEIKH -> resolveSheikh(bookmark)
    }

    private suspend fun resolveSurah(bookmark: Bookmark): BookmarkListItem.SurahItem? {
        val surahNumber = bookmark.surahNumber ?: return null
        val surah = SurahCatalog.all.find { it.number == surahNumber } ?: return null
        val startPage = getTargetPage.forSurah(surahNumber).getOrNull()
        return BookmarkListItem.SurahItem(
            bookmarkId = bookmark.id,
            surahNumber = surahNumber,
            nameAr = surah.nameAr,
            nameEn = surah.nameEn,
            ayahCount = surah.verseCount,
            startPage = startPage,
        )
    }

    private suspend fun resolveAyah(bookmark: Bookmark): BookmarkListItem.AyahItem? {
        val surahNumber = bookmark.surahNumber ?: return null
        val ayahNumber = bookmark.ayahNumber ?: return null
        val surah = SurahCatalog.all.find { it.number == surahNumber }
        val ayahText = getAyahText(surahNumber, ayahNumber).getOrNull()
        val page = bookmark.pageNumber ?: getTargetPage.forAyah(surahNumber, ayahNumber).getOrNull()
        return BookmarkListItem.AyahItem(
            bookmarkId = bookmark.id,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            ayahText = ayahText,
            surahNameAr = surah?.nameAr.orEmpty(),
            surahNameEn = surah?.nameEn.orEmpty(),
            page = page,
        )
    }

    private fun resolvePage(bookmark: Bookmark): BookmarkListItem.PageItem? {
        val pageNumber = bookmark.pageNumber ?: return null
        val surah = SurahCatalog.all.find { it.number == MushafConstants.surahForPage(pageNumber) }
        return BookmarkListItem.PageItem(
            bookmarkId = bookmark.id,
            pageNumber = pageNumber,
            surahNameAr = surah?.nameAr.orEmpty(),
            surahNameEn = surah?.nameEn.orEmpty(),
            juz = MushafConstants.juzForPage(pageNumber),
        )
    }

    private suspend fun resolveSheikh(bookmark: Bookmark): BookmarkListItem.SheikhItem? {
        val sheikhId = bookmark.sheikhId ?: return null
        val sheikh = getSheikhById(sheikhId).getOrNull() ?: return null
        return BookmarkListItem.SheikhItem(
            bookmarkId = bookmark.id,
            sheikhId = sheikhId,
            name = sheikh.name,
            initials = sheikh.initials,
            rating = sheikh.rating,
        )
    }
}
