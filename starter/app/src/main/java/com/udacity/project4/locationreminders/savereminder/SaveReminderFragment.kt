package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                    NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminderData = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateAndSaveReminder(reminderData)) {
                //            TODO: use the user entered reminder details to:
                //             1) add a geofencing request
                //             2) save the reminder to the local db
                addGeofence(reminderData)
            }

        }
    }

    private fun addGeofence(reminderData: ReminderDataItem) {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence(reminderData)
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
                if (runningQOrLater) {
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(
                                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                } else {
                    true
                }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsArray,
                resultCode
        )
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(reminderData: ReminderDataItem, resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
                settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(requireActivity(),
                            REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Toast.makeText(context, getString(R.string.permission_denied_explanation), Toast.LENGTH_LONG).show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder(reminderData)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder(reminderData: ReminderDataItem) {
        if (reminderData != null) {
            val geofence = Geofence.Builder()
                    .setRequestId(reminderData.id)
                    .setCircularRegion(
                            reminderData.latitude!!,
                            reminderData.longitude!!,
                            GEOFENCE_RADIUS_IN_METERS.toFloat()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()

            val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()
            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_EVENT
            val geofencePendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                addOnCompleteListener {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        addOnSuccessListener {
                            Log.e("Add Geofence", geofence.requestId)
                        }
                        addOnFailureListener {
                            if ((it.message != null)) {
                                Log.w(TAG, it.message!!)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }
}
