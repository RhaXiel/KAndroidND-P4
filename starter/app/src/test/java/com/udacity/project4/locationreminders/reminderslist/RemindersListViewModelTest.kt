package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(maxSdk = Build.VERSION_CODES.P)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeRepo: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    private fun getReminder(): ReminderDTO {
        return ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            latitude = -34.0,
            longitude = 151.0)
    }

    @Before
    fun createRepository() {
        stopKoin()

        fakeRepo = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeRepo
        )
    }

    @Test
    fun load_reminders_when_reminders_are_unavailable() = runBlockingTest {
        fakeRepo.setShouldReturnError(true)
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), CoreMatchers.`is`("Reminders not found"))
    }

    @Test
    fun show_noData() = runBlockingTest {
        fakeRepo.deleteAllReminders()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(true))
    }

    @Test
    fun show_loadingWithData() = runBlocking {
        fakeRepo.deleteAllReminders()
        val reminder = getReminder()
        fakeRepo.saveReminder(reminder)
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))
        mainCoroutineRule.resumeDispatcher()
        MatcherAssert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(false))


    }
}