package com.adkom666.shrednotes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import com.adkom666.shrednotes.data.google.Google
import javax.inject.Inject

/**
 * Main screen model.
 *
 * @property google [Google] to access "Google Drive" to store app data.
 */
class MainViewModel @Inject constructor(
    private val google: Google
) : ViewModel() {

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
