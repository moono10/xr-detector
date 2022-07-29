/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartfarm.core.activity;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Build;
import android.os.ConditionVariable;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import androidx.annotation.NonNull;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.smartfarm.common.helpers.CameraPermissionHelper;
import com.smartfarm.common.helpers.TrackingStateHelper;
import com.smartfarm.common.rendering.BackgroundRenderer;
import com.smartfarm.core.ARUtil;
import com.smartfarm.core.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class DefaultAREngineActivity extends DeafultEngineActivity implements ImageReader.OnImageAvailableListener {

  private static final String TAG = DefaultAREngineActivity.class.getSimpleName();

  // ARCore session that supports camera sharing.
  public Session sharedSession;

  // Camera capture session. Used by both non-AR and AR modes.
  private CameraCaptureSession captureSession;

  // Reference to the camera system service.
  private CameraManager cameraManager;

  // A list of CaptureRequest keys that can cause delays when switching between AR and non-AR modes.
  private List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified;

  // Camera device. Used by both non-AR and AR modes.
  private CameraDevice cameraDevice;

  // ARCore shared camera instance, obtained from ARCore session that supports sharing.
  private SharedCamera sharedCamera;

  // Camera ID for the camera used by ARCore.
  private String cameraId;

  // Ensure GL surface draws only occur when new frames are available.
  protected final AtomicBoolean shouldUpdateSurfaceTexture = new AtomicBoolean(false);

  // Whether ARCore is currently active.
  protected boolean arcoreActive;

  /**
   * Whether an error was thrown during session creation.
   */
  private boolean errorCreatingSession = false;

  // Camera preview capture request builder
  private CaptureRequest.Builder previewCaptureRequestBuilder;

  // Image reader that continuously processes CPU images.
  private ImageReader cpuImageReader;

  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

  // Prevent any changes to camera capture session after CameraManager.openCamera() is called, but
  // before camera device becomes active.
  private boolean captureSessionChangesPossible = true;

  // A check mechanism to ensure that the camera closed properly so that the app can safely exit.
  private final ConditionVariable safeToExitApp = new ConditionVariable();

  // Repeating camera capture session capture callback.
  private final CameraCaptureSession.CaptureCallback cameraCaptureCallback =
      new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull TotalCaptureResult result) {
          shouldUpdateSurfaceTexture.set(true);
        }

        @Override
        public void onCaptureBufferLost(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull Surface target,
            long frameNumber) {
          Log.e(TAG, "onCaptureBufferLost: " + frameNumber);
        }

        @Override
        public void onCaptureFailed(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull CaptureFailure failure) {
          Log.e(TAG, "onCaptureFailed: " + failure.getFrameNumber() + " " + failure.getReason());
        }

        @Override
        public void onCaptureSequenceAborted(
            @NonNull CameraCaptureSession session, int sequenceId) {
          Log.e(TAG, "onCaptureSequenceAborted: " + sequenceId + " " + session);
        }
      };

  // GL surface created callback. Will be called on the GL thread.
  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
   super.onSurfaceCreated(gl, config);
    openCamera();
  }

  @Override
  protected void onDestroy() {
    if (sharedSession != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      sharedSession.close();
      sharedSession = null;
    }

    super.onDestroy();
  }

  private synchronized void waitUntilCameraCaptureSessionIsActive() {
    while (!captureSessionChangesPossible) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        Log.e(TAG, "Unable to wait for a safe time to make changes to the capture session", e);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    waitUntilCameraCaptureSessionIsActive();
    startBackgroundThread();
    canvas.onResume();

    // When the activity starts and resumes for the first time, openCamera() will be called
    // from onSurfaceCreated(). In subsequent resumes we call openCamera() here.
    if (surfaceCreated) {
      openCamera();
    }

    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    shouldUpdateSurfaceTexture.set(false);
    canvas.onPause();
    waitUntilCameraCaptureSessionIsActive();
    displayRotationHelper.onPause();

    pauseARCore();

    closeCamera();
    stopBackgroundThread();
    super.onPause();
  }

  protected void resumeARCore() {
    // Ensure that session is valid before triggering ARCore resume. Handles the case where the user
    // manually uninstalls ARCore while the app is paused and then resumes.
    if (sharedSession == null) {
      return;
    }

    if (!arcoreActive) {
      try {
        // To avoid flicker when resuming ARCore mode inform the renderer to not suppress rendering
        // of the frames with zero timestamp.
        getBackgroundRenderer().suppressTimestampZeroRendering(false);
        // Resume ARCore.
        sharedSession.resume();
        arcoreActive = true;

        // Set capture session callback while in AR mode.
        sharedCamera.setCaptureCallback(cameraCaptureCallback, backgroundHandler);
      } catch (CameraNotAvailableException e) {
        Log.e(TAG, "Failed to resume ARCore session", e);
        return;
      }
    }
  }

  protected void pauseARCore() {
    if (arcoreActive) {
      // Pause ARCore.
      sharedSession.pause();

      arcoreActive = false;
    }
  }
  public abstract BackgroundRenderer getBackgroundRenderer();

  // Called when starting non-AR mode or switching to non-AR mode.
  // Also called when app starts in AR mode, or resumes in AR mode.
  private void setRepeatingCaptureRequest() {
    try {

      captureSession.setRepeatingRequest(previewCaptureRequestBuilder.build(), cameraCaptureCallback, backgroundHandler);
    } catch (CameraAccessException e) {
      Log.e(TAG, "Failed to set repeating request", e);
    }
  }

  private void createCameraPreviewSession() {
    try {
      sharedSession.setCameraTextureName(getBackgroundRenderer().getTextureId());
      //sharedCamera.getSurfaceTexture().setOnFrameAvailableListener(this);

      // Create an ARCore compatible capture request using `TEMPLATE_RECORD`.
      previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

      // Build surfaces list, starting with ARCore provided surfaces.
      List<Surface> surfaceList = sharedCamera.getArCoreSurfaces();

      // Add a CPU image reader surface. On devices that don't support CPU image access, the image
      // may arrive significantly later, or not arrive at all.
      surfaceList.add(cpuImageReader.getSurface());

      // Surface list should now contain three surfaces:
      // 0. sharedCamera.getSurfaceTexture()
      // 1. …
      // 2. cpuImageReader.getSurface()

      // Add ARCore surfaces and CPU image surface targets.
      for (Surface surface : surfaceList) {
        previewCaptureRequestBuilder.addTarget(surface);
      }

      // Wrap our callback in a shared camera callback.
      CameraCaptureSession.StateCallback wrappedCallback =
          sharedCamera.createARSessionStateCallback( new CameraCaptureSession.StateCallback() {

            // Called when the camera capture session is first configured after the app
            // is initialized, and again each time the activity is resumed.
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
              Log.d(TAG, "Camera capture session configured.");
              captureSession = session;

              setRepeatingCaptureRequest();
            }

            @Override
            public void onSurfacePrepared(
                    @NonNull CameraCaptureSession session, @NonNull Surface surface) {
              Log.d(TAG, "Camera capture surface prepared.");
            }

            @Override
            public void onReady(@NonNull CameraCaptureSession session) {
              Log.d(TAG, "Camera capture session ready.");
            }

            @Override
            public void onActive(@NonNull CameraCaptureSession session) {
              Log.d(TAG, "Camera capture session active.");
              if (!arcoreActive) {
                resumeARCore();
              }
              synchronized (DefaultAREngineActivity.this) {
                captureSessionChangesPossible = true;
                DefaultAREngineActivity.this.notify();
              }
            }

            @Override
            public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
              Log.w(TAG, "Camera capture queue empty.");
            }

            @Override
            public void onClosed(@NonNull CameraCaptureSession session) {
              Log.d(TAG, "Camera capture session closed.");
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
              Log.e(TAG, "Failed to configure camera capture session.");
            }
          }, backgroundHandler);

      // Create camera capture session for camera preview using ARCore wrapped callback.
      cameraDevice.createCaptureSession(surfaceList, wrappedCallback, backgroundHandler);
    } catch (CameraAccessException e) {
      Log.e(TAG, "CameraAccessException", e);
    }
  }

  // Perform various checks, then open camera device and create CPU image reader.
  private void openCamera() {
    // Don't open camera if already opened.
    if (cameraDevice != null) {
      return;
    }

    // Verify CAMERA_PERMISSION has been granted.
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      CameraPermissionHelper.requestCameraPermission(this);
      return;
    }

    // Make sure that ARCore is installed, up to date, and supported on this device.
    if (!ARUtil.isARCoreSupportedAndUpToDate(this)) {
      return;
    }

    if (sharedSession == null) {
      try {
        // Create ARCore session that supports camera sharing.
        sharedSession = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));
      } catch (Exception e) {
        errorCreatingSession = true;

        Log.e(TAG, "Failed to create ARCore session that supports camera sharing", e);
        return;
      }

      errorCreatingSession = false;

      // Enable auto focus mode while ARCore is running.
      Config config = sharedSession.getConfig();
      config.setFocusMode(Config.FocusMode.AUTO);
      sharedSession.configure(config);
    }

    // Store the ARCore shared camera reference.
    sharedCamera = sharedSession.getSharedCamera();

    // Store the ID of the camera used by ARCore.
    cameraId = sharedSession.getCameraConfig().getCameraId();

    // Use the currently configured CPU image size.
    Size desiredCpuImageSize = sharedSession.getCameraConfig().getImageSize();
    cpuImageReader =
        ImageReader.newInstance(
            desiredCpuImageSize.getWidth(),
            desiredCpuImageSize.getHeight(),
            ImageFormat.YUV_420_888,
            20);
    cpuImageReader.setOnImageAvailableListener(this, backgroundHandler);

    // When ARCore is running, make sure it also updates our CPU image surface.
    sharedCamera.setAppSurfaces(this.cameraId, Arrays.asList(cpuImageReader.getSurface()));

    try {

      // Wrap our callback in a shared camera callback.
      CameraDevice.StateCallback wrappedCallback =
          sharedCamera.createARDeviceStateCallback(new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
              Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " opened.");
              DefaultAREngineActivity.this.cameraDevice = cameraDevice;
              createCameraPreviewSession();
            }

            @Override
            public void onClosed(@NonNull CameraDevice cameraDevice) {
              Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " closed.");
              DefaultAREngineActivity.this.cameraDevice = null;
              safeToExitApp.open();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
              Log.w(TAG, "Camera device ID " + cameraDevice.getId() + " disconnected.");
              cameraDevice.close();
              DefaultAREngineActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
              Log.e(TAG, "Camera device ID " + cameraDevice.getId() + " error " + error);
              cameraDevice.close();
              DefaultAREngineActivity.this.cameraDevice = null;
              // Fatal error. Quit application.
              finish();
            }
          }, backgroundHandler);

      // Store a reference to the camera system service.
      cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

      // Get the characteristics for the ARCore camera.
      CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.cameraId);

      // On Android P and later, get list of keys that are difficult to apply per-frame and can
      // result in unexpected delays when modified during the capture session lifetime.
      if (Build.VERSION.SDK_INT >= 28) {
        keysThatCanCauseCaptureDelaysWhenModified = characteristics.getAvailableSessionKeys();
        if (keysThatCanCauseCaptureDelaysWhenModified == null) {
          // Initialize the list to an empty list if getAvailableSessionKeys() returns null.
          keysThatCanCauseCaptureDelaysWhenModified = new ArrayList<>();
        }
      }

      // Prevent app crashes due to quick operations on camera open / close by waiting for the
      // capture session's onActive() callback to be triggered.
      captureSessionChangesPossible = false;

      // Open the camera device using the ARCore wrapped callback.
      cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler);
    } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
      Log.e(TAG, "Failed to open camera", e);
    }
  }

  // Close the camera device.
  private void closeCamera() {
    if (captureSession != null) {
      captureSession.close();
      captureSession = null;
    }
    if (cameraDevice != null) {
      waitUntilCameraCaptureSessionIsActive();
      safeToExitApp.close();
      cameraDevice.close();
      safeToExitApp.block();
    }
    if (cpuImageReader != null) {
      cpuImageReader.close();
      cpuImageReader = null;
    }
  }

  // CPU image reader callback.
  @Override
  public void onImageAvailable(ImageReader imageReader) {

  }

  // GL draw callback. Will be called each frame on the GL thread.
  @Override
  public void onDrawFrame(GL10 gl) {
    // Use the cGL clear color specified in onSurfaceCreated() to erase the GL surface.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (!shouldUpdateSurfaceTexture.get()) {
      // Not ready to draw.
      return;
    }

    // Handle display rotations.
    displayRotationHelper.updateSessionIfNeeded(sharedSession);
    if (!arcoreActive) {
      // ARCore not yet active, so nothing to draw yet.
      return;
    }

    if (errorCreatingSession) {
      // Session not created, so nothing to draw.
      return;
    }
    try {
      // Perform ARCore per-frame update.
      Frame frame = sharedSession.update();
      Camera camera = frame.getCamera();

      for (Scene scene : scenes) {
        scene.draw(camera, frame);
      }
      // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
      trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());
      // If not tracking, don't draw 3D objects.
      if (camera.getTrackingState() == TrackingState.PAUSED) {
        return;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}