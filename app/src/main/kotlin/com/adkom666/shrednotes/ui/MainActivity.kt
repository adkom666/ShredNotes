package com.adkom666.shrednotes.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityMainBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.ask.AskFragment
import com.adkom666.shrednotes.ui.exercises.ExercisesFragment
import com.adkom666.shrednotes.ui.notes.NotesFragment
import com.adkom666.shrednotes.ui.statistics.StatisticsFragment
import com.adkom666.shrednotes.util.getCurrentlyDisplayedFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Inject

/**
 * Main screen.
 */
@ExperimentalCoroutinesApi
class MainActivity :
    AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityMainBinding
        get() = requireNotNull(_binding)

    private val model: MainViewModel
        get() = requireNotNull(_model)

    private var _binding: ActivityMainBinding? = null
    private var _model: MainViewModel? = null

    private val onExpandSearchViewListener: MenuItem.OnActionExpandListener =
        SearchActivenessListener()

    private val onQueryTextListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = search(query)
            override fun onQueryTextChange(newText: String?): Boolean = preview(newText)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        observeSection(isInitialScreenPresent = savedInstanceState != null)
        initNavigation()

        supportFragmentManager.registerFragmentLifecycleCallbacks(OptionsMenuInvalidator(), false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tools, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
            prepareSearch(fragment, it)
            prepareFilter(fragment, it)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (handleToolSelection(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return handleNavSelection(item)
    }

    private fun observeSection(isInitialScreenPresent: Boolean) {
        var isScreenPresent = isInitialScreenPresent
        var isScreenNotFirst = false

        model.sectionAsLiveData.observe(this) { section ->
            val fragment = section.getFragment()
            if (isScreenPresent) {
                if (isScreenNotFirst) {
                    replaceFragment(fragment)
                } else {
                    // First screen is already set by system
                    isScreenNotFirst = true
                }
            } else {
                addFragment(fragment)
                isScreenPresent = true
                isScreenNotFirst = true
            }
        }
    }

    private fun initNavigation() {
        binding.bottomNavigation.selectedItemId = model.section.getActionId()
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
    }

    private fun prepareSearch(fragment: Fragment?, menu: Menu) {

        fun MenuItem.ensureActionViewExpanded() {
            if (!isActionViewExpanded) {
                expandActionView()
            }
        }

        fun MenuItem.ensureActionViewCollapsed() {
            if (isActionViewExpanded) {
                collapseActionView()
            }
        }

        val itemSearch = menu.findItem(R.id.action_search)
        itemSearch.setOnActionExpandListener(null)
        val searchView = itemSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(null)
        val isSearchVisible = if (fragment is Searchable) {
            val isSearchActive = fragment.isSearchActive
            val currentQuery = fragment.currentQuery
            if (currentQuery.isNullOrEmpty() && isSearchActive.not()) {
                itemSearch.ensureActionViewCollapsed()
            } else {
                itemSearch.ensureActionViewExpanded()
                searchView?.setQuery(currentQuery, false)
            }
            true
        } else {
            itemSearch.ensureActionViewCollapsed()
            false
        }
        searchView?.isVisible = isSearchVisible
        menu.setGroupVisible(R.id.group_search, isSearchVisible)
        if (isSearchVisible) {
            itemSearch.setOnActionExpandListener(onExpandSearchViewListener)
            searchView?.setOnQueryTextListener(onQueryTextListener)
        }
    }

    private fun prepareFilter(fragment: Fragment?, menu: Menu) {
        val isFilterVisible = if (fragment is Filterable) {
            val iconRes = if (fragment.isFilterEnabled) {
                R.drawable.ic_filter_on
            } else {
                R.drawable.ic_filter_off
            }
            val itemFilter = menu.findItem(R.id.action_filter)
            itemFilter?.icon = ContextCompat.getDrawable(this, iconRes)
            true
        } else {
            false
        }
        menu.setGroupVisible(R.id.group_filter, isFilterVisible)
    }

    private fun handleToolSelection(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> filter()
            R.id.action_read -> read()
            R.id.action_write -> write()
            R.id.action_sign_in -> signIn()
            R.id.action_sign_out -> signOut()
            else -> return false
        }
        return true
    }

    private fun handleNavSelection(item: MenuItem): Boolean {
        getTargetSection(item)?.let { section ->
            Timber.d("Navigation button pressed: section=$section")
            model.section = section
            return true
        } ?: return false
    }

    private fun reportSearchActiveness(isActive: Boolean) {
        Timber.d("Search active: $isActive")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Searchable) {
            fragment.isSearchActive = isActive
        }
    }

    private fun search(query: String?): Boolean {
        Timber.d("Search: query=$query")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        return if (fragment is Searchable) {
            fragment.search(query)
        } else {
            false
        }
    }

    private fun preview(newText: String?): Boolean {
        Timber.d("Preview search query: newText=$newText")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        return if (fragment is Searchable) {
            fragment.preview(newText)
        } else {
            false
        }
    }

    private fun filter() {
        Timber.d("Button pressed: filter")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Filterable) {
            fragment.filter { filterEnabled ->
                Timber.d("filterEnabled=$filterEnabled")
            }
        }
    }

    private fun read() {
        Timber.d("Button pressed: read notes")
    }

    private fun write() {
        Timber.d("Button pressed: write notes")
    }

    private fun signIn() {
        Timber.d("Button pressed: sign in")
    }

    private fun signOut() {
        Timber.d("Button pressed: sign out")
    }

    private fun addFragment(fragment: Fragment) {
        Timber.d("Add fragment: $fragment")
        supportFragmentManager
            .beginTransaction()
            .replace(binding.content.id, fragment)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        Timber.d("Replace fragment: $fragment")
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(binding.content.id, fragment)
            .commit()
    }

    private fun getTargetSection(item: MenuItem): Section? = when (item.itemId) {
        R.id.action_notes -> Section.NOTES
        R.id.action_exercises -> Section.EXERCISES
        R.id.action_statistics -> Section.STATISTICS
        R.id.action_ask -> Section.ASK
        else -> null
    }

    @IdRes
    private fun Section.getActionId(): Int = when (this) {
        Section.NOTES -> R.id.action_notes
        Section.EXERCISES -> R.id.action_exercises
        Section.STATISTICS -> R.id.action_statistics
        Section.ASK -> R.id.action_ask
    }

    private fun Section.getFragment(): Fragment = when (this) {
        Section.NOTES -> NotesFragment.newInstance()
        Section.EXERCISES -> ExercisesFragment.newInstance()
        Section.STATISTICS -> StatisticsFragment.newInstance()
        Section.ASK -> AskFragment.newInstance()
    }

    private inner class OptionsMenuInvalidator : FragmentManager.FragmentLifecycleCallbacks() {

        var isFirstFragmentActivityCreated: Boolean = false

        override fun onFragmentActivityCreated(
            fragmentManager: FragmentManager,
            fragment: Fragment,
            savedInstanceState: Bundle?
        ) {
            Timber.d("onFragmentActivityCreated")
            if (isFirstFragmentActivityCreated) {
                invalidateOptionsMenu()
            } else {
                isFirstFragmentActivityCreated = true
            }
        }
    }

    private inner class SearchActivenessListener : MenuItem.OnActionExpandListener {

        override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
            reportSearchActiveness(true)
            return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
            reportSearchActiveness(false)
            return true
        }
    }
}
