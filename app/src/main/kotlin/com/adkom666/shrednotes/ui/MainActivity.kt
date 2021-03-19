package com.adkom666.shrednotes.ui

import android.content.Intent
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
import androidx.lifecycle.Observer
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

    companion object {
        private const val REQUEST_CODE_SIGN_IN = 228
    }

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

        invalidateSubtitle()
        observeSection(isInitialScreenPresent = savedInstanceState != null)
        initNavigation()
        supportFragmentManager.registerFragmentLifecycleCallbacks(OptionsMenuInvalidator(), false)
        model.stateAsLiveData.observe(this, StateObserver())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tools, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            val state = model.state
            val forceInvisible = state == null || state is MainViewModel.State.Waiting
            val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
            prepareSearch(it, forceInvisible, fragment)
            prepareFilter(it, forceInvisible, fragment)
            val itemsVisibility = model.googleDriveItemsVisibility
            prepareGoogleDrivePanel(it, forceInvisible, itemsVisibility)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d(
            """onActivityResult:
                |requestCode=$requestCode,
                |resultCode=$resultCode,
                |data=$data""".trimMargin()
        )
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> model.handleSignInGoogleResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
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

    private fun invalidateSubtitle() {
        supportActionBar?.subtitle = model.googleAccountDisplayName
    }

    private fun initNavigation() {
        binding.bottomNavigation.selectedItemId = model.section.getActionId()
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
    }

    private fun prepareSearch(
        menu: Menu,
        forceInvisible: Boolean,
        fragment: Fragment?
    ) {
        fun prepare(itemSearch: MenuItem, fragment: Fragment?) {

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

            itemSearch.setOnActionExpandListener(null)
            val searchView = itemSearch.actionView as? SearchView
            searchView?.setOnQueryTextListener(null)
            val isSearchVisible = if (forceInvisible.not() && fragment is Searchable) {
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

        val itemSearch = menu.findItem(R.id.action_search)
        itemSearch?.let { prepare(it, fragment) }
    }

    private fun prepareFilter(
        menu: Menu,
        forceInvisible: Boolean,
        fragment: Fragment?
    ) {
        val isFilterVisible = if (forceInvisible.not() && fragment is Filterable) {
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

    private fun prepareGoogleDrivePanel(
        menu: Menu,
        forceInvisible: Boolean,
        itemsVisibility: MainViewModel.GoogleDriveItemsVisibility
    ) {
        val itemRead = menu.findItem(R.id.action_read)
        val itemWrite = menu.findItem(R.id.action_write)
        val itemSignOut = menu.findItem(R.id.action_sign_out)
        val itemSignIn = menu.findItem(R.id.action_sign_in)
        itemRead?.isVisible = forceInvisible.not() && itemsVisibility.isItemReadVisibile
        itemWrite?.isVisible = forceInvisible.not() && itemsVisibility.isItemWriteVisibile
        itemSignOut?.isVisible = forceInvisible.not() && itemsVisibility.isItemSignOutVisibile
        itemSignIn?.isVisible = forceInvisible.not() && itemsVisibility.isItemSignInVisibile
    }

    private fun handleToolSelection(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> filter()
            R.id.action_read -> read()
            R.id.action_write -> write()
            R.id.action_sign_out -> signOut()
            R.id.action_sign_in -> signIn()
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
        val intent = model.signInGoogleIntent
        startActivityForResult(intent, REQUEST_CODE_SIGN_IN)
    }

    private fun signOut() {
        Timber.d("Button pressed: sign out")
        model.signOutFromGoogle()
    }

    private fun addFragment(fragment: Fragment) {
        Timber.d("Add fragment: $fragment")
        supportFragmentManager
            .beginTransaction()
            .replace(binding.section.id, fragment)
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
            .replace(binding.section.id, fragment)
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

        private var isFirstFragmentActivityCreated: Boolean = false

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

    private inner class StateObserver : Observer<MainViewModel.State> {

        override fun onChanged(state: MainViewModel.State?) {
            Timber.d("State is $state")
            when (state) {
                is MainViewModel.State.Preparation -> {
                    prepare(state)
                    model.ok()
                }
                MainViewModel.State.Working ->
                    setWaiting(false)
                MainViewModel.State.Waiting -> {
                    setWaiting(true)
                    invalidateOptionsMenu()
                }
            }
        }

        private fun prepare(state: MainViewModel.State.Preparation) = when (state) {
            MainViewModel.State.Preparation.Initial ->
                Unit
            MainViewModel.State.Preparation.Continuing ->
                invalidateOptionsMenu()
            MainViewModel.State.Preparation.GoogleDriveStateChanged -> {
                invalidateSubtitle()
                invalidateOptionsMenu()
            }
        }

        private fun setWaiting(active: Boolean) {
            binding.progressBar.isVisible = active
            binding.content.isVisible = active.not()
        }
    }
}
