package com.adkom666.shrednotes.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adkom666.shrednotes.common.CoroutineTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * This [ViewModel] can execute [CoroutineTask] objects sequentially.
 */
open class ExecutiveViewModel : ViewModel() {

    private val taskFlow: Flow<CoroutineTask>
        get() = _taskChannel.receiveAsFlow()

    private val _taskChannel: Channel<CoroutineTask> = Channel(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            taskFlow.collect { it() }
        }
    }

    /**
     * Send a [task] for execution.
     *
     * @param task [CoroutineTask] to execute upon completion of previous ones.
     */
    protected fun execute(task: CoroutineTask) {
        Timber.d("Execute: task=$task")
        _taskChannel.offer(task)
    }
}
