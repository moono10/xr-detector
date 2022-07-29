package com.smartfarm.ai;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smartfarm.common.helpers.CameraPermissionHelper;
import com.smartfarm.common.helpers.DisplayRotationHelper;
import com.smartfarm.common.helpers.FullScreenHelper;
import com.smartfarm.core.Scene;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class DeafultEngineActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = DeafultEngineActivity.class.getSimpleName();

    // GL Surface used to draw camera preview image.
    protected GLSurfaceView canvas;

    // Whether the GL surface has been created.
    protected boolean surfaceCreated;

    protected List<Scene> scenes = new ArrayList<Scene>();

    protected DisplayRotationHelper displayRotationHelper;

    protected abstract Scene createScene();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // GL surface view that renders camera preview image.
        canvas = findViewById(R.id.glsurfaceview);
        canvas.setPreserveEGLContextOnPause(true);
        canvas.setEGLContextClientVersion(2);
        canvas.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        canvas.setRenderer(this);
        canvas.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Helpers, see hello_ar_java sample to learn more.
        displayRotationHelper = new DisplayRotationHelper(this);
    }

    // Android permission request callback.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(getApplicationContext(), "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    // Android focus change callback.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        surfaceCreated = true;
        // Set GL clear color to black.
        GLES20.glClearColor(0f, 0f, 0f, 1.0f);
        scenes.add(createScene());
    }

    // GL surface changed callback. Will be called on the GL thread.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        displayRotationHelper.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {

    }


    // Looper handler thread.
    protected HandlerThread backgroundThread;

    // Looper handler.
    protected Handler backgroundHandler;

    // Start background handler thread, used to run callbacks without blocking UI thread.
    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // Stop background handler thread.
    protected void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while trying to join background handler thread", e);
            }
        }
    }
}
