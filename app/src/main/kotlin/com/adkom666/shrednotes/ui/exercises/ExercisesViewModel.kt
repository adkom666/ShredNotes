package com.adkom666.shrednotes.ui.exercises

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import androidx.paging.toLiveData
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.util.Selector
import com.adkom666.shrednotes.util.containsDifferentTrimmedTextIgnoreCaseThan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Exercises section model.
 *
 * @property exerciseRepository exercise storage management.
 */
@ExperimentalCoroutinesApi
class ExercisesViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private companion object {
        private const val EXERCISE_PAGE_SIZE = 15
        private const val EXERCISE_INITIAL_LOAD_HINT = 30
        private const val EXERCISE_PREFETCH_DISTANCE = 15

        private const val MESSAGE_CHANNEL_CAPACITY = 3
    }

    /**
     * Exercise list state.
     */
    sealed class State {

        /**
         * Waiting for the end of some operation.
         */
        object Waiting : State()

        /**
         * The exercise list is ready for interaction.
         */
        object Ready : State()

        /**
         * Error when working with the list.
         */
        sealed class Error : State() {

            /**
             * The details of this error are described in the [message].
             *
             * @param message the details of the error.
             */
            data class Clarified(val message: String) : Error()

            /**
             * Unknown error.
             */
            object Unknown : Error()
        }
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Message about the count of deleted messages.
         *
         * @param count count of deleted messages.
         */
        data class Deleted(val count: Int) : Message()
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = _stateAsLiveData

    /**
     * Subscribe to the current list of exercises in the UI thread.
     */
    val exercisePagedListAsLiveData: LiveData<PagedList<Exercise>>
        get() = exerciseSourceFactory.toLiveData(pagedListConfig)

    /**
     * Consume information messages from this channel in the UI thread.
     */
    val messageChannel: ReceiveChannel<Message>
        get() = _messageChannel.openSubscription()

    /**
     * Information about the selected exercises.
     */
    val selector: Selector = Selector(0)

    /**
     * The text that must be contained in the names of the displayed exercises (case-insensitive).
     */
    var subname: String? by Delegates.observable<String?>(null) { _, old, new ->
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            viewModelScope.launch {
                exerciseSourceFactory.subname = new?.trim()
                val exerciseCount = exerciseRepository.countSuspending(new)
                selector.reset(exerciseCount)
                invalidateExercises()
            }
        }
    }

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isSearchActive: Boolean = false

    private val pagedListConfig: PagedList.Config = PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPageSize(EXERCISE_PAGE_SIZE)
        .setInitialLoadSizeHint(EXERCISE_INITIAL_LOAD_HINT)
        .setPrefetchDistance(EXERCISE_PREFETCH_DISTANCE)
        .build()

    private val exerciseSourceFactory = ExerciseSourceFactory(exerciseRepository, subname)

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    init {
        viewModelScope.launch {
            // Ignore initial value
            exerciseRepository.countFlow.drop(1).collect { exerciseCount ->
                Timber.d("Exercise list changed: exerciseCount=$exerciseCount")
                selector.reset(exerciseCount)
                invalidateExercises()
            }
        }
    }

    /**
     * Prepare working with the exercise list.
     */
    fun prepare() {
        viewModelScope.launch {
            val exerciseCount = exerciseRepository.countSuspending(subname)
            selector.reset(exerciseCount)
        }
    }

    /**
     * Start working with the exercise list.
     */
    fun start() {
        setState(State.Ready)
    }

    /**
     * Initiate the deletion of the selected exercises.
     */
    fun deleteSelectedExercises() {
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val deletionCount = deleteSelectedExercises(selector.state)
                setState(State.Ready)
                _messageChannel.offer(Message.Deleted(deletionCount))
            } catch (e: Exception) {
                e.localizedMessage?.let {
                    setState(State.Error.Clarified(it))
                } ?: setState(State.Error.Unknown)
            }
        }
    }

    private suspend fun deleteSelectedExercises(
        selectorState: Selector.State
    ): Int = when (selectorState) {
        is Selector.State.Active.Inclusive -> {
            val selectedItemIdList = selectorState.selectedItemIdSet.toList()
            exerciseRepository.deleteSuspending(selectedItemIdList, subname)
        }
        is Selector.State.Active.Exclusive -> {
            val unselectedItemIdList = selectorState.unselectedItemIdSet.toList()
            exerciseRepository.deleteOtherSuspending(unselectedItemIdList, subname)
        }
        Selector.State.Inactive -> 0
    }

    private fun invalidateExercises() {
        setState(State.Waiting)
        exerciseSourceFactory.invalidate()
        setState(State.Ready)
    }

    private fun setState(state: State) {
        _stateAsLiveData.value = state
    }

    private class ExerciseSourceFactory(
        private val exerciseRepository: ExerciseRepository,
        var subname: String?
    ) : DataSource.Factory<Int, Exercise>() {

        private var exerciseSource: ExerciseSource? = null

        override fun create(): DataSource<Int, Exercise> {
            val exerciseSource = ExerciseSource(exerciseRepository, subname)
            this.exerciseSource = exerciseSource
            return exerciseSource
        }

        fun invalidate() {
            exerciseSource?.invalidate()
        }
    }

    private class ExerciseSource(
        private val exerciseRepository: ExerciseRepository,
        private var subname: String?
    ) : PositionalDataSource<Exercise>() {

        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<Exercise>
        ) {
            val (exerciseList, startPosition) = exerciseRepository.page(
                params.requestedLoadSize,
                params.requestedStartPosition,
                subname
            )
            Timber.d("Load initial exercises: exerciseList.size=${exerciseList.size}")
            callback.onResult(exerciseList, startPosition)
        }

        override fun loadRange(
            params: LoadRangeParams,
            callback: LoadRangeCallback<Exercise>
        ) {
            val exerciseList = exerciseRepository.list(
                params.loadSize,
                params.startPosition,
                subname
            )
            Timber.d("Load next exercises: exerciseList.size=${exerciseList.size}")
            callback.onResult(exerciseList)
        }
    }
}
