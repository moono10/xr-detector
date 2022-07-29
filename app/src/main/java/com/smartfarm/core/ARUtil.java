package com.smartfarm.core;

import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableException;
import com.smartfarm.core.components.ObjectDetectComponent;

public class ARUtil {

    private static final String TAG = ARUtil.class.getSimpleName();
    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.

    public static boolean isARCoreSupportedAndUpToDate(android.app.Activity applicationActivity) {
        // Make sure ARCore is installed and supported on this device.
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(applicationActivity);
        switch (availability) {
            case SUPPORTED_INSTALLED:
                break;
            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                try {
                    // Request ARCore installation or update if needed.
                    ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(applicationActivity, /*userRequestedInstall=*/ true);
                    switch (installStatus) {
                        case INSTALL_REQUESTED:
                            Log.e(TAG, "ARCore installation requested.");
                            return false;
                        case INSTALLED:
                            break;
                    }
                } catch (UnavailableException e) {
                    Log.e(TAG, "ARCore not installed", e);
                    applicationActivity.runOnUiThread(
                            () ->
                                    Toast.makeText(applicationActivity.getApplicationContext(), "ARCore not installed\n" + e, Toast.LENGTH_LONG).show());
                    applicationActivity.finish();
                    return false;
                }
                break;
            case UNKNOWN_ERROR:
            case UNKNOWN_CHECKING:
            case UNKNOWN_TIMED_OUT:
            case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                Log.e(
                        TAG,
                        "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned "
                                + availability);
                applicationActivity.runOnUiThread(
                        () ->
                                Toast.makeText(
                                                applicationActivity.getApplicationContext(),
                                                "ARCore is not supported on this device, "
                                                        + "ArCoreApk.checkAvailability() returned "
                                                        + availability,
                                                Toast.LENGTH_LONG)
                                        .show());
                return false;
        }
        return true;
    }
}
