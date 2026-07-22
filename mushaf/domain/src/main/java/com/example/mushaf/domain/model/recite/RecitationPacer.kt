package com.example.mushaf.domain.model.recite

data class RecitationPacerConfig(

    val initialFramesPerWord: Float = 5f,


    val maxLookaheadWords: Int = 20,


    val easeAfterWords: Int = 10,

    val maxDrag: Float = 2.5f,

    val paceSmoothing: Float = 0.3f,

    val minFramesPerWord: Float = 1.5f,
    val maxFramesPerWord: Float = 20f,
)


class RecitationPacer(
    private val config: RecitationPacerConfig = RecitationPacerConfig(),
) {
    private var words: List<String> = emptyList()

    private var confirmedIndex = -1
    private var predictedIndex = -1
    private var framesIntoWord = 0f
    private var framesPerWord = config.initialFramesPerWord

    val currentWordId: String? get() = words.getOrNull(predictedIndex)

    val estimatedFramesPerWord: Float get() = framesPerWord

    val isAtLookaheadLimit: Boolean
        get() = leadWords >= config.maxLookaheadWords

    private val leadWords: Int get() = predictedIndex - confirmedIndex


    private val drag: Float
        get() {
            val runway = config.maxLookaheadWords - config.easeAfterWords
            if (runway <= 0) return 1f
            val past = (leadWords - config.easeAfterWords).coerceAtLeast(0)
            val ratio = (past.toFloat() / runway).coerceIn(0f, 1f)
            return 1f + (config.maxDrag - 1f) * ratio
        }


    fun setWords(wordIds: List<String>) {
        val previous = currentWordId
        words = wordIds
        val restored = previous?.let { wordIds.indexOf(it) } ?: -1
        predictedIndex = restored
        confirmedIndex = restored
        framesIntoWord = 0f
    }


    fun confirm(wordId: String) {
        val index = words.indexOf(wordId)
        if (index < 0) return
        confirmedIndex = index
        predictedIndex = index
        framesIntoWord = 0f
    }


    fun observePace(wordCount: Int, speechFrames: Int) {
        if (wordCount <= 0 || speechFrames <= 0) return
        val measured = (speechFrames.toFloat() / wordCount)
            .coerceIn(config.minFramesPerWord, config.maxFramesPerWord)
        framesPerWord += (measured - framesPerWord) * config.paceSmoothing
    }


    fun onSpeechFrame(): String? {
        if (words.isEmpty()) return null

        if (predictedIndex < 0) {
            predictedIndex = 0
            framesIntoWord = 0f
            return currentWordId
        }

        if (isAtLookaheadLimit) return currentWordId

        framesIntoWord += 1f
        
        
        while (!isAtLookaheadLimit) {
            val framesNeeded = framesPerWord * drag
            if (framesIntoWord < framesNeeded) break
            framesIntoWord -= framesNeeded
            if (predictedIndex < words.lastIndex) predictedIndex++ else break
        }
        return currentWordId
    }

    





 
    fun placeAtStart() {
        if (words.isEmpty()) return
        predictedIndex = 0
        confirmedIndex = -1
        framesIntoWord = 0f
    }

    val isAtEndOfWords: Boolean
        get() = words.isNotEmpty() && predictedIndex >= words.lastIndex

    fun reset() {
        confirmedIndex = -1
        predictedIndex = -1
        framesIntoWord = 0f
        framesPerWord = config.initialFramesPerWord
    }
}
