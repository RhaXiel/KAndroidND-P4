package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    TODO: Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun getReminder(): ReminderDTO {
        return ReminderDTO(
            title = "title",
            description = "description",
            location = "location",
            latitude = -34.0,
            longitude = 151.0)
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun repository_saveReminder_getReminderById() = runBlocking {
        // GIVEN - Insert a reminder via repository
        val reminder = getReminder()
        repository.saveReminder(reminder)

        // WHEN  - Get reminder from repository
        val result = repository.getReminder(reminder.id)

        // THEN - Data loaded matches data inserted
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success

        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
        assertThat(result.data.location, `is`(reminder.location))
    }

    @Test
    fun repository_deleteAllReminders_getReminderById() = runBlocking {
        // GIVEN - The repository has saved data
        val reminder = getReminder()
        repository.saveReminder(reminder)
        // WHEN  - All reminders are deleted
        repository.deleteAllReminders()
        // THEN - No data has to match with any id and an error has to be returned
        val result = repository.getReminder(reminder.id)
        assertThat(result is Result.Error, `is`(true))
    }
}