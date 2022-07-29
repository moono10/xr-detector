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
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.smartfarm.common.rendering.BackgroundRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.common.rendering.geometry.Vector3;
import com.smartfarm.core.Color4;
import com.smartfarm.core.Node;
import com.smartfarm.core.Scene;
import com.smartfarm.core.activity.DefaultAREngineActivity;
import com.smartfarm.core.components.ARBackgroundComponent;
import com.smartfarm.core.components.ARSurfaceDetectComponent;
import com.smartfarm.core.components.ARTrackedPointComponent;
import com.smartfarm.core.components.ARHitTestComponent;
import com.smartfarm.core.components.LineRendererComponent;
import com.smartfarm.core.components.ObjectDetectComponent;

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

    private ObjectDetectComponent odc;
    @Override
    protected Scene createScene() {

        Scene scene = new Scene();





        Node ar = new Node();

        arBackgroundComponent = new ARBackgroundComponent(this);
        ar.addComponent(arBackgroundComponent);
        ar.addComponent(new ARSurfaceDetectComponent(this));
        ar.addComponent(new ARTrackedPointComponent(this));
        scene.getNodes().add(ar);
       // private final LineRenderer rayLineRenderer = new LineRenderer(255.0f/ 255.0f, 255.0f/ 255.0f, 255.0f/ 255.0f);



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










        Node objectDetector = new Node();
        odc = new ObjectDetectComponent(this);
        objectDetector.addComponent(odc);
        scene.getNodes().add(objectDetector);

        scene.getNodes().add(hitTest);

        return scene;
    }
    ARBackgroundComponent arBackgroundComponent;
    @Override
    public BackgroundRenderer getBackgroundRenderer() {
        return arBackgroundComponent.backgroundRenderer;
    }

    Node hitTest;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extraBundle = getIntent().getExtras();
        if (extraBundle != null && 1 == extraBundle.getShort(AUTOMATOR_KEY, AUTOMATOR_DEFAULT)) {
            automatorRun.set(true);
        }

        hitTest = new Node();
        hitTest.addComponent(new ARHitTestComponent(this));

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
        odc.onImageAvailable(imageReader, displayRotationHelper, cameraId);

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
