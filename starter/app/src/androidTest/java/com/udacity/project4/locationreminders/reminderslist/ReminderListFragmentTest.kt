package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initRepository() {
        stopKoin()
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }

            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }

        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }

        repository = GlobalContext.get().koin.get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private fun getReminder(): ReminderDTO {
        return ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            latitude = -34.0,
            longitude = 151.0
        )
    }

    //    TODO: test the navigation of the fragments.
    //    TODO: test the displayed data on the UI.
    //    TODO: add testing for the error messages.
    @Test
    fun view_reminders_displayed() = runBlockingTest {
        // GIVEN - A reminder exists
        val reminder = getReminder()
        // WHEN  - It get saved into a repository
        runBlocking {
            repository.saveReminder(reminder)
        }
        // THEN - It's details can be displayed on screen
        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(
            ViewAssertions.matches(
                CoreMatchers.not(
                    ViewMatchers.isDisplayed()
                )
            )
        )
        onView(ViewMatchers.withText(reminder.title)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminder.description)).check(
            ViewAssertions.matches(
                ViewMatchers.isDisplayed()
            )
        )
        onView(ViewMatchers.withText(reminder.location)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun view_noReminders_showsNoData() = runBlockingTest {
        // GIVEN - No reminders are currently saved
        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        // WHEN  - The application loads
        // THEN - A message that says "no data" is displayed
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun view_clickOnFabIcon_navigatesTo_saveReminderFragment() {
        // GIVEN - The reminder list is displayed
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN  - The users clicks on the add new reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())
        // THEN - Navigation occurs
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}