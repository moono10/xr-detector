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

package com.smartfarm.ai;


import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.smartfarm.common.rendering.LineRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.common.rendering.geometry.Vector3;
import com.smartfarm.core.Color4;
import com.smartfarm.core.Node;
import com.smartfarm.core.Scene;
import com.smartfarm.core.components.LineRendererComponent;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.opengles.GL10;

public class SharedCameraActivity extends DefaultAREngineActivity {

    private static final String TAG = SharedCameraActivity.class.getSimpleName();

    private final AtomicBoolean automatorRun = new AtomicBoolean(false);

    // Linear layout that contains preview image and status text.
    private LinearLayout imageTextLinearLayout;

    // Text view for displaying on screen status message.
    private TextView statusTextView;

    // Total number of CPU images processed.
    private int cpuImagesProcessed;

    // Required for test run.
    private static final Short AUTOMATOR_DEFAULT = 0;
    private static final String AUTOMATOR_KEY = "automator";

    @Override
    protected Scene createScene() {

        Scene scene = new Scene();

        LineString lsx = new LineString();
        lsx.getPointList().add(new Vector3(0,0,0));
        lsx.getPointList().add(new Vector3(1000, 0, 0));
        lsx.setColor4(new Color4(255.0f/ 255.0f, 0.0f/ 255.0f, 0.0f/ 255.0f, 255.0f / 255.0f));
        Node xAxis = new Node();
        xAxis.addComponent(new LineRendererComponent(this, lsx));
        scene.getNodes().add(xAxis);

        LineString lsY = new LineString();
        lsY.getPointList().add(new Vector3(0,0,0));
        lsY.getPointList().add(new Vector3(0, 1000, 0));
        lsY.setColor4(new Color4(0.0f/ 255.0f, 255.0f/ 255.0f, 0.0f/ 255.0f, 255.0f / 255.0f));
        Node yAxis = new Node();
        xAxis.addComponent(new LineRendererComponent(this, lsY));
        scene.getNodes().add(yAxis);

        LineString lsz = new LineString();
        lsz.getPointList().add(new Vector3(0,0,0));
        lsz.getPointList().add(new Vector3(0, 0, 1000));
        lsz.setColor4(new Color4(0.0f/ 255.0f, 0.0f/ 255.0f, 255.0f/ 255.0f, 255.0f / 255.0f));
        Node zAxis = new Node();
        xAxis.addComponent(new LineRendererComponent(this, lsz));
        scene.getNodes().add(zAxis);







       // private final LineRenderer rayLineRenderer = new LineRenderer(255.0f/ 255.0f, 255.0f/ 255.0f, 255.0f/ 255.0f);



        return scene;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extraBundle = getIntent().getExtras();
        if (extraBundle != null && 1 == extraBundle.getShort(AUTOMATOR_KEY, AUTOMATOR_DEFAULT)) {
            automatorRun.set(true);
        }

        // Switch to allow pausing and resuming of ARCore.
        Switch arcoreSwitch = findViewById(R.id.arcore_switch);
        // Ensure initial switch position is set based on initial value of `arMode` variable.
        /**
        arcoreSwitch.setChecked(arMode);
        arcoreSwitch.setOnCheckedChangeListener(
                (view, checked) -> {
                    Log.i(TAG, "Switching to " + (checked ? "AR" : "non-AR") + " mode.");
                    if (checked) {
                        arMode = true;
                        resumeARCore();
                    } else {
                        arMode = false;
                        pauseARCore();
                        resumeCamera2();
                    }
                    updateSnackbarMessage();
                });*/

        messageSnackbarHelper.setMaxLines(4);
        updateSnackbarMessage();

        imageTextLinearLayout = findViewById(R.id.image_text_layout);
        statusTextView = findViewById(R.id.text_view);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        runOnUiThread(
                () -> {
                    // Adjust layout based on display orientation.
                    imageTextLinearLayout.setOrientation(
                            width > height ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
                });
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        super.onImageAvailable(imageReader);

        cpuImagesProcessed++;

        // Reduce the screen update to once every two seconds with 30fps if running as automated test.
        if (!automatorRun.get() || (automatorRun.get() && cpuImagesProcessed % 60 == 0)) {
            runOnUiThread(
                    () ->
                            statusTextView.setText(
                                    "CPU images processed: "
                                            + cpuImagesProcessed
                                            + "\n\nMode: "
                                            + "AR"
                                            + " \nARCore active: "
                                            + arcoreActive
                                            + " \nShould update surface texture: "
                                            + shouldUpdateSurfaceTexture.get()));
        }
    }


}
