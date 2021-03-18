package com.adkom666.shrednotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged

/**
 * Main screen model.
 *
 * @param application current [Application].
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        private val DEFAULT_SECTION = Section.NOTES
    }

    /**
     * Subscribe to the current section in the UI thread.
     */
    val sectionAsLiveData: LiveData<Section>
        get() = distinctUntilChanged(_sectionAsLiveData)

    /**
     * Get and set the current section in the UI thread.
     */
    var section: Section
        get() = _sectionAsLiveData.value ?: DEFAULT_SECTION
        set(value) {
            _sectionAsLiveData.value = value
        }

    private val _sectionAsLiveData: MutableLiveData<Section> = MutableLiveData(DEFAULT_SECTION)
}
