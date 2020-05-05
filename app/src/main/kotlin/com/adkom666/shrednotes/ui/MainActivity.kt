package com.adkom666.shrednotes.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityMainBinding
import com.adkom666.shrednotes.ui.ask.AskFragment
import com.adkom666.shrednotes.ui.exercises.ExercisesFragment
import com.adkom666.shrednotes.ui.notes.NotesFragment
import com.adkom666.shrednotes.ui.statistics.StatisticsFragment
import com.adkom666.shrednotes.util.getCurrentlyDisplayedFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main screen.
 */
class MainActivity :
    AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {

    private val binding: ActivityMainBinding
        get() = _binding ?: error("View binding is not initialized!")

    private val model: MainViewModel
        get() = _model ?: error("View model is not initialized!")

    private var _binding: ActivityMainBinding? = null
    private var _model: MainViewModel? = null

    private val onQueryTextListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = search(query)
            override fun onQueryTextChange(newText: String?): Boolean = preview(newText)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = ViewModelProvider(this).get(MainViewModel::class.java)

        observeActionBarButtons()
        setContentIfNeed(savedInstanceState)
        initNavigation()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tools, menu)
        menu?.let {
            initSearch(it)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            prepareSearch(it)
            prepareFilter(it)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return if (handleToolSelection(it)) {
                true
            } else {
                super.onOptionsItemSelected(it)
            }
        } ?: return false
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return handleNavSelection(item)
    }

    private fun observeActionBarButtons() {
        model.isSearchVisibleAsLiveData.observe(this, Observer {
            invalidateOptionsMenu()
        })
        model.isFilterVisibleAsLiveData.observe(this, Observer {
            invalidateOptionsMenu()
        })
        model.isFilterEnabledAsLiveData.observe(this, Observer {
            invalidateOptionsMenu()
        })
    }

    private fun setContentIfNeed(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            setScreen(model.section)
        }
    }

    private fun initNavigation() {
        binding.bottomNavigation.selectedItemId = model.section.getActionId()
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
    }

    private fun initSearch(menu: Menu) {
        val itemSearch = menu.findItem(R.id.action_search)
        val searchView = itemSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(onQueryTextListener)
    }

    private fun prepareSearch(menu: Menu) {
        val searchVisible = model.isSearchVisible
        menu.setGroupVisible(R.id.group_search, searchVisible)
    }

    private fun prepareFilter(menu: Menu) {
        val itemFilter = menu.findItem(R.id.action_filter)
        val filterVisible = model.isFilterVisibleAsLiveData.value ?: true
        if (filterVisible) {
            val filterEnabled = model.isFilterEnabledAsLiveData.value ?: false
            val iconRes = if (filterEnabled) {
                R.drawable.ic_filter_on
            } else {
                R.drawable.ic_filter_off
            }
            itemFilter?.icon = getDrawable(iconRes)
        }
        menu.setGroupVisible(R.id.group_filter, filterVisible)
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
        when (item.itemId) {
            R.id.action_notes -> switchScreen(Section.NOTES)
            R.id.action_exercises -> switchScreen(Section.EXERCISES)
            R.id.action_statistics -> switchScreen(Section.STATISTICS)
            R.id.action_ask -> switchScreen(Section.ASK)
            else -> return false
        }
        return true
    }

    private fun search(query: String?): Boolean {
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        return if (fragment is Searchable) {
            fragment.search(query)
        } else {
            false
        }
    }

    private fun preview(newText: String?): Boolean {
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        return if (fragment is Searchable) {
            fragment.preview(newText)
        } else {
            false
        }
    }

    private fun filter() {
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Filterable) {
            fragment.filter { filterEnabled ->
                model.isFilterEnabled = filterEnabled
            }
        }
    }

    private fun read() {
        TODO("Not yet implemented")
    }

    private fun write() {
        TODO("Not yet implemented")
    }

    private fun signIn() {
        TODO("Not yet implemented")
    }

    private fun signOut() {
        TODO("Not yet implemented")
    }

    private fun setScreen(section: Section) {
        val fragment = section.getFragment()
        adjustToolbar(fragment)
        addFragment(fragment)
    }

    private fun switchScreen(section: Section) {
        if (section != model.section) {
            val fragment = section.getFragment()
            adjustToolbar(fragment)
            replaceFragment(fragment)
            model.section = section
        }
    }

    private fun adjustToolbar(fragment: Fragment) {
        model.isSearchVisible = fragment is Searchable
        if (fragment is Filterable) {
            model.isFilterVisible = true
            model.isFilterEnabled = fragment.isFilterEnabled
        } else {
            model.isFilterVisible = false
        }
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.content.id, fragment)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(binding.content.id, fragment)
            .commit()
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
}
