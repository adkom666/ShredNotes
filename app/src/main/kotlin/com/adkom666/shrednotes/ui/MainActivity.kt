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
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityMainBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.ask.AskFragment
import com.adkom666.shrednotes.ui.exercises.ExercisesFragment
import com.adkom666.shrednotes.ui.notes.NotesFragment
import com.adkom666.shrednotes.ui.statistics.StatisticsFragment
import com.adkom666.shrednotes.util.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.getCurrentlyDisplayedFragment
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.android.AndroidInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
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

        private const val REQUEST_CODE_AUTH_ON_READ = 229
        private const val REQUEST_CODE_AUTH_ON_WRITE = 230

        private const val TAG_CONFIRM_READ = "${BuildConfig.APPLICATION_ID}.tags.confirm_read"
        private const val TAG_CONFIRM_WRITE = "${BuildConfig.APPLICATION_ID}.tags.confirm_write"

        private const val TAG_CONFIRM_SIGN_OUT =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_sign_out"
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
        restoreFragmentListeners()

        model.stateAsLiveData.observe(this, StateObserver())

        lifecycleScope.launchWhenResumed {
            model.navigationChannel.consumeEach(::goToScreen)
        }

        lifecycleScope.launchWhenStarted {
            model.messageChannel.consumeEach(::show)
        }
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
            REQUEST_CODE_SIGN_IN -> model.handleSignInGoogleResult(this, resultCode, data)
            REQUEST_CODE_AUTH_ON_READ -> model.handleAuthOnReadResult(resultCode)
            REQUEST_CODE_AUTH_ON_WRITE -> model.handleAuthOnWriteResult(resultCode)
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

    private fun restoreFragmentListeners() {
        supportFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_READ) {
            it.setReadingListener()
        }
        supportFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_WRITE) {
            it.setWritingListener()
        }
        supportFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_SIGN_OUT) {
            it.setSigningOutListener()
        }
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
            R.id.action_filter -> onFilterItemSelected()
            R.id.action_read -> onReadItemSelected()
            R.id.action_write -> onWriteItemSelected()
            R.id.action_sign_out -> onSignOutItemSelected()
            R.id.action_sign_in -> onSignInItemSelected()
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

    private fun onFilterItemSelected() {
        Timber.d("Item selected: filter")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Filterable) {
            fragment.filter { filterEnabled ->
                Timber.d("filterEnabled=$filterEnabled")
            }
        }
    }

    private fun onReadItemSelected() {
        Timber.d("Item selected: read notes")
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_read_title,
            R.string.dialog_confirm_read_message
        )
        dialogFragment.setReadingListener()
        dialogFragment.show(supportFragmentManager, TAG_CONFIRM_READ)
    }

    private fun onWriteItemSelected() {
        Timber.d("Item selected: write notes")
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_write_title,
            R.string.dialog_confirm_write_message
        )
        dialogFragment.setWritingListener()
        dialogFragment.show(supportFragmentManager, TAG_CONFIRM_WRITE)
    }

    private fun onSignOutItemSelected() {
        Timber.d("Item selected: sign out")
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_sign_out_title,
            R.string.dialog_confirm_sign_out_message
        )
        dialogFragment.setSigningOutListener()
        dialogFragment.show(supportFragmentManager, TAG_CONFIRM_SIGN_OUT)
    }

    private fun onSignInItemSelected() {
        Timber.d("Item selected: sign in")
        model.signInGoogle(this)
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

    private fun goToScreen(direction: MainViewModel.NavDirection) = when (direction) {
        is MainViewModel.NavDirection.ToSignIn ->
            startActivityForResult(direction.intent, REQUEST_CODE_SIGN_IN)
        is MainViewModel.NavDirection.ToAuthOnRead ->
            startActivityForResult(direction.intent, REQUEST_CODE_AUTH_ON_READ)
        is MainViewModel.NavDirection.ToAuthOnWrite ->
            startActivityForResult(direction.intent, REQUEST_CODE_AUTH_ON_WRITE)
    }

    private fun show(message: MainViewModel.Message) = when (message) {
        MainViewModel.Message.ShredNotesUpdate ->
            toast(R.string.message_shred_notes_has_been_updated)
        MainViewModel.Message.NoShredNotesUpdate ->
            toast(R.string.message_shred_notes_has_not_been_updated, isShort = false)
        MainViewModel.Message.GoogleDriveUpdate ->
            toast(R.string.message_google_drive_has_been_updated)
        is MainViewModel.Message.Error ->
            showError(message)
    }

    private fun showError(message: MainViewModel.Message.Error) = when (message) {
        MainViewModel.Message.Error.UnauthorizedUser ->
            toast(R.string.error_unauthorized_user)
        MainViewModel.Message.Error.WrongJsonSyntax ->
            toast(R.string.error_wrong_json_syntax)
        is MainViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        MainViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
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

    private fun ConfirmationDialogFragment.setReadingListener() {
        setOnConfirmListener {
            model.read()
        }
    }

    private fun ConfirmationDialogFragment.setWritingListener() {
        setOnConfirmListener {
            model.write()
        }
    }

    private fun ConfirmationDialogFragment.setSigningOutListener() {
        setOnConfirmListener {
            model.signOutFromGoogle(this@MainActivity)
        }
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
