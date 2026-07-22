package com.example.mushaf.domain.model.recite

data class RecitationPacerConfig(

    val initialFramesPerWord: Float = 5f,


    val maxLookaheadWords: Int = 6,

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
        get() = predictedIndex - confirmedIndex >= config.maxLookaheadWords


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
        while (framesIntoWord >= framesPerWord && !isAtLookaheadLimit) {
            framesIntoWord -= framesPerWord
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
