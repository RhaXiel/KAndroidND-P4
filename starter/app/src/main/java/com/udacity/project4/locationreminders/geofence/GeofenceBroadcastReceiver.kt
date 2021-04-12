package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.ACTION_GEOFENCE_EVENT

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            GeofenceTransitionsJobIntentService.enqueueWork(context, intent)

        }
    }
}

/*
private const val TAG = "GeofenceReceiver"

val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.v(TAG, context.getString(R.string.geofence_entered))

                val fenceId = when {
                    geofencingEvent.triggeringGeofences.isNotEmpty() ->
                        geofencingEvent.triggeringGeofences[0].requestId
                    else -> {
                        Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                        return
                    }
                }
                // Check geofence against the constants listed in GeofenceUtil.kt to see if the
                // user has entered any of the locations we track for geofences.

                //Check if geofencing has been entered
                val title = "_viewModel.reminderTitle.value"
                val description = "_viewModel.reminderDescription.value"
                val location = "_viewModel.reminderSelectedLocationStr.value"
                val latitude = 0.0
                val longitude = 0.0
                val foundItem = ReminderDataItem(title, description, location, latitude, longitude)

                // Unknown Geofences aren't helpful to us
                if ( foundItem == null  ) {
                    Log.e(TAG, "Unknown Geofence: Abort Mission")
                    return
                }

                val notificationManager = ContextCompat.getSystemService(
                        context,
                        NotificationManager::class.java
                ) as NotificationManager

                notificationManager.sendGeofenceEnteredNotification(
                        context, foundItem
                )
            }*/