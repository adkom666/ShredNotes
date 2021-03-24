package com.adkom666.shrednotes.ui.exercises

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import androidx.paging.toLiveData
import com.adkom666.shrednotes.data.model.Exercise
import com.adkom666.shrednotes.data.repository.ExerciseRepository
import com.adkom666.shrednotes.data.repository.NoteRepository
import com.adkom666.shrednotes.util.containsDifferentTrimmedTextIgnoreCaseThan
import com.adkom666.shrednotes.util.selection.ManageableSelection
import com.adkom666.shrednotes.util.selection.SelectableItems
import com.adkom666.shrednotes.util.selection.Selection
import com.adkom666.shrednotes.util.selection.SelectionDashboard
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
    private val exerciseRepository: ExerciseRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private companion object {
        private const val PAGE_SIZE = 15
        private const val PAGED_LIST_INITIAL_LOAD_HINT = 30
        private const val PAGED_LIST_PREFETCH_DISTANCE = 15

        private const val NAVIGATION_CHANNEL_CAPACITY = 1
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
         * Interacting with the user.
         */
        object Working : State()
    }

    /**
     * Navigation direction.
     */
    sealed class NavDirection {

        /**
         * To the screen that allows you to add a new exercise.
         */
        object ToAddExerciseScreen : NavDirection()

        /**
         * To the screen that allows you to update an existing exercise.
         *
         * @property exercise [Exercise] to update.
         */
        data class ToUpdateExerciseScreen(val exercise: Exercise) : NavDirection()
    }

    /**
     * Information message.
     */
    sealed class Message {

        /**
         * Message about adding an exercise.
         */
        object Addition : Message()

        /**
         * Message about the exercise update.
         */
        object Update : Message()

        /**
         * Message about the count of notes associated with the selected exercises.
         *
         * @property noteCount count of notes associated with the selected exercises.
         */
        data class AssociatedNoteCount(val noteCount: Int) : Message()

        /**
         * Message about the count of deleted exercises.
         *
         * @property count count of deleted exercises.
         */
        data class Deletion(val count: Int) : Message()

        /**
         * Error message.
         */
        sealed class Error : Message() {

            /**
             * The details of this error are described in the [details].
             *
             * @property details the details of the error.
             */
            data class Clarified(val details: String) : Error()

            /**
             * Unknown error.
             */
            object Unknown : Error()
        }
    }

    /**
     * Subscribe to the current state in the UI thread.
     */
    val stateAsLiveData: LiveData<State>
        get() = distinctUntilChanged(_stateAsLiveData)

    /**
     * Subscribe to the current list of exercises in the UI thread.
     */
    val exercisePagedListAsLiveData: LiveData<PagedList<Exercise>>
        get() = exerciseSourceFactory.toLiveData(pagedListConfig)

    /**
     * Consume navigation directions from this channel in the UI thread.
     */
    val navigationChannel: ReceiveChannel<NavDirection>
        get() = _navigationChannel.openSubscription()

    /**
     * Consume information messages from this channel in the UI thread.
     */
    val messageChannel: ReceiveChannel<Message>
        get() = _messageChannel.openSubscription()

    /**
     * Exercises to select.
     */
    val selectableExercises: SelectableItems
        get() = _manageableSelection

    /**
     * Tools for manipulating the selection.
     */
    val selectionDashboard: SelectionDashboard
        get() = _manageableSelection

    /**
     * Information about the presence of selected exercises.
     */
    val selection: Selection
        get() = _manageableSelection

    /**
     * The text that must be contained in the names of the displayed exercises (case-insensitive).
     */
    var subname: String? by Delegates.observable(null) { _, old, new ->
        Timber.d("Change subname: old=$old, new=$new")
        if (new containsDifferentTrimmedTextIgnoreCaseThan old) {
            viewModelScope.launch {
                exerciseSourceFactory.subname = new?.trim()
                val exerciseCount = exerciseRepository.countSuspending(new)
                _manageableSelection.reset(exerciseCount)
                invalidateExercises()
            }
        }
    }

    /**
     * Property for storing a flag indicating whether the search is active.
     */
    var isSearchActive: Boolean by Delegates.observable(false) { _, old, new ->
        Timber.d("Change isSearchActive: old=$old, new=$new")
    }

    private val _manageableSelection: ManageableSelection = ManageableSelection()

    private val pagedListConfig: PagedList.Config = PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPageSize(PAGE_SIZE)
        .setInitialLoadSizeHint(PAGED_LIST_INITIAL_LOAD_HINT)
        .setPrefetchDistance(PAGED_LIST_PREFETCH_DISTANCE)
        .build()

    private val exerciseSourceFactory: ExerciseSourceFactory = ExerciseSourceFactory(
        exerciseRepository,
        subname
    )

    private val _stateAsLiveData: MutableLiveData<State> = MutableLiveData(State.Waiting)

    private val _navigationChannel: BroadcastChannel<NavDirection> =
        BroadcastChannel(NAVIGATION_CHANNEL_CAPACITY)

    private val _messageChannel: BroadcastChannel<Message> =
        BroadcastChannel(MESSAGE_CHANNEL_CAPACITY)

    init {
        Timber.d("Init")
        viewModelScope.launch {
            val exerciseInitialCount = exerciseRepository.countSuspending(subname)
            Timber.d("exerciseInitialCount=$exerciseInitialCount")
            _manageableSelection.init(exerciseInitialCount)
            setState(State.Working)
            // Ignore initial value
            exerciseRepository.countFlow.drop(1).collect { exerciseCount ->
                Timber.d("Exercise list changed: exerciseCount=$exerciseCount")
                _manageableSelection.reset(exerciseCount)
                invalidateExercises()
            }
        }
    }

    /**
     * Start adding a new exercise.
     */
    fun addExercise() {
        Timber.d("Add exercise")
        navigateTo(NavDirection.ToAddExerciseScreen)
    }

    /**
     * Request to generate the message [Message.AssociatedNoteCount].
     */
    fun requestAssociatedNoteCount() {
        Timber.d("Request associated notes count")
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val noteCount = requestAssociatedNoteCount(_manageableSelection.state)
                setState(State.Working)
                report(Message.AssociatedNoteCount(noteCount))
            } catch (e: Exception) {
                setState(State.Working)
                reportAbout(e)
            }
        }
    }

    /**
     * Initiate the deletion of the selected exercises.
     */
    fun deleteSelectedExercises() {
        Timber.d("Delete selected exercises")
        setState(State.Waiting)
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val deletionCount = deleteSelectedExercises(_manageableSelection.state)
                setState(State.Working)
                report(Message.Deletion(deletionCount))
            } catch (e: Exception) {
                setState(State.Working)
                reportAbout(e)
            }
        }
    }

    /**
     * Handle the clicked exercise when the selection is inactive.
     */
    fun onExerciseClick(exercise: Exercise) {
        Timber.d("onExerciseClick: exercise=$exercise")
        navigateTo(NavDirection.ToUpdateExerciseScreen(exercise))
    }

    /**
     * Call this method when the results of adding the exercise are available.
     *
     * @param isResultOk true if the exercise was added, false otherwise.
     */
    fun onAddExerciseResult(isResultOk: Boolean) {
        Timber.d("onAddExerciseResult: isResultOk=$isResultOk")
        if (isResultOk) {
            Timber.d("Exercise has been added")
            report(Message.Addition)
        }
    }

    /**
     * Call this method when the results of the exercise update are available.
     *
     * @param isResultOk true if the exercise was updated, false otherwise.
     */
    fun onUpdateExerciseResult(isResultOk: Boolean) {
        Timber.d("onUpdateExerciseResult: isResultOk=$isResultOk")
        if (isResultOk) {
            Timber.d("Exercise has been updated")
            report(Message.Update)
        }
    }

    private suspend fun requestAssociatedNoteCount(
        selectionState: ManageableSelection.State
    ): Int = when (selectionState) {
        is ManageableSelection.State.Active.Inclusive -> {
            val selectedExerciseIdList = selectionState.selectedItemIdSet.toList()
            noteRepository.countByExerciseIdsSuspending(selectedExerciseIdList)
        }
        is ManageableSelection.State.Active.Exclusive -> {
            val unselectedExerciseIdList = selectionState.unselectedItemIdSet.toList()
            noteRepository.countOtherByExerciseIdsSuspending(unselectedExerciseIdList)
        }
        ManageableSelection.State.Inactive -> 0
    }

    private suspend fun deleteSelectedExercises(
        selectionState: ManageableSelection.State
    ): Int = when (selectionState) {
        is ManageableSelection.State.Active.Inclusive -> {
            val selectedExerciseIdList = selectionState.selectedItemIdSet.toList()
            exerciseRepository.deleteSuspending(selectedExerciseIdList, subname)
        }
        is ManageableSelection.State.Active.Exclusive -> {
            val unselectedExerciseIdList = selectionState.unselectedItemIdSet.toList()
            exerciseRepository.deleteOtherSuspending(unselectedExerciseIdList, subname)
        }
        ManageableSelection.State.Inactive -> 0
    }

    private fun invalidateExercises() {
        setState(State.Waiting)
        exerciseSourceFactory.invalidate()
        setState(State.Working)
    }

    private fun reportAbout(e: Exception) {
        e.localizedMessage?.let {
            report(Message.Error.Clarified(it))
        } ?: report(Message.Error.Unknown)
    }

    private fun setState(state: State) {
        Timber.d("Set state: state=$state")
        _stateAsLiveData.postValue(state)
    }

    private fun navigateTo(direction: NavDirection) {
        Timber.d("Navigate to: direction=$direction")
        _navigationChannel.offer(direction)
    }

    private fun report(message: Message) {
        Timber.d("Report: message=$message")
        _messageChannel.offer(message)
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
