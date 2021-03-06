package edu.isel.pdm.memorymatrix

import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import edu.isel.pdm.memorymatrix.GameState.State.*
import kotlinx.android.parcel.Parcelize

/**
 * Runs on the UI thread the given action after the specified delay
 *
 * @param delay     the number of milliseconds the execution will be delayed
 * @param action    the action to be executed on the UI thread (the main thread)
 */
private fun runDelayed(delay: Long, action: ()-> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({ action() }, delay)
}

/**
 * Data class used to represent the game saved state
 */
@Parcelize
data class GameState(
    val toGuess: MatrixPattern? = null,
    val currentGuess: MatrixPattern? = null,
    val state: State = NOT_STARTED) : Parcelable {

    enum class State { NOT_STARTED, MEMORIZING, GUESSING, ENDED }
}

private const val SAVED_STATE_KEY = "MatrixViewModel.SavedState"

/**
 * View model for the memory game main activity
 */
class MatrixViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    /**
     * The game state.
     */
    val game: MutableLiveData<GameState> by lazy {
        MutableLiveData<GameState>(savedState.get<GameState>(SAVED_STATE_KEY) ?: GameState())
    }

    /**
     * Starts a new guessing game. The game will be in the [GameState.State.MEMORIZING] for the next
     * [memorizeFor] seconds
     *
     * @param guessCount    the number of elements in the pattern to be memorized
     * @param matrixSide    the side of the matrix containing the pattern to be memorized
     * @param memorizeFor   the number of seconds that the game remains in the [GameState.State.MEMORIZING]
     * state before moving on to the [GameState.State.GUESSING].
     */
    fun startGame(guessCount: Int, matrixSide: Int, memorizeFor: Int): MatrixViewModel {
        if (game.value?.state != NOT_STARTED && game.value?.state != ENDED)
            throw IllegalStateException()

        game.value = GameState(
            toGuess = MatrixPattern.fromRandom(guessCount, matrixSide),
            currentGuess = MatrixPattern.empty(matrixSide),
            state = MEMORIZING)

        runDelayed(memorizeFor * 1000L) {
            game.value = GameState(game.value?.toGuess, game.value?.currentGuess, GUESSING)
            savedState[SAVED_STATE_KEY] = game.value
        }
        return this
    }

    /**
     * Adds a new guess to the current set of guesses.
     */
    fun addGuess(guess: Position): MatrixViewModel {
        if (game.value?.state != GUESSING)
            throw IllegalStateException()

        val toGuess = game.value?.toGuess ?: throw IllegalStateException()
        val current = game.value?.currentGuess?.plus(guess) ?: throw IllegalStateException()
        game.value = GameState(toGuess, current, if (toGuess.count == current.count) ENDED else GUESSING)
        savedState[SAVED_STATE_KEY] = game.value
        return this
    }
}
