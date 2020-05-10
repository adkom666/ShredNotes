package com.adkom666.shrednotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged

/**
 * Main screen model.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        private const val IS_SEARCH_VISIBLE_BY_DEFAULT = true
        private const val IS_FILTER_VISIBLE_BY_DEFAULT = true
        private const val IS_FILTER_ENABLED_BY_DEFAULT = false

        private val DEFAULT_SECTION = Section.NOTES
    }

    private val _isSearchVisibleAsLiveData: MutableLiveData<Boolean> =
        MutableLiveData(IS_SEARCH_VISIBLE_BY_DEFAULT)

    private val _isFilterVisibleAsLiveData: MutableLiveData<Boolean> =
        MutableLiveData(IS_FILTER_VISIBLE_BY_DEFAULT)

    private val _isFilterEnabledAsLiveData: MutableLiveData<Boolean> =
        MutableLiveData(IS_FILTER_ENABLED_BY_DEFAULT)

    private val _sectionAsLiveData: MutableLiveData<Section> =
        MutableLiveData(DEFAULT_SECTION)

    val isSearchVisibleAsLiveData: LiveData<Boolean> =
        distinctUntilChanged(_isSearchVisibleAsLiveData)

    val isFilterVisibleAsLiveData: LiveData<Boolean> =
        distinctUntilChanged(_isFilterVisibleAsLiveData)

    val isFilterEnabledAsLiveData: LiveData<Boolean> =
        distinctUntilChanged(_isFilterEnabledAsLiveData)

    val sectionAsLiveData: LiveData<Section> =
        distinctUntilChanged(_sectionAsLiveData)

    var isSearchVisible: Boolean
        get() = _isSearchVisibleAsLiveData.value ?: IS_SEARCH_VISIBLE_BY_DEFAULT
        set(value) {
            _isSearchVisibleAsLiveData.value = value
        }

    var isFilterVisible: Boolean
        get() = _isFilterVisibleAsLiveData.value ?: IS_FILTER_VISIBLE_BY_DEFAULT
        set(value) {
            _isFilterVisibleAsLiveData.value = value
        }

    var isFilterEnabled: Boolean
        get() = _isFilterEnabledAsLiveData.value ?: IS_FILTER_ENABLED_BY_DEFAULT
        set(value) {
            _isFilterEnabledAsLiveData.value = value
        }

    var section: Section
        get() = _sectionAsLiveData.value ?: DEFAULT_SECTION
        set(value) {
            _sectionAsLiveData.value = value
        }
}
