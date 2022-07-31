package com.smartfarm.core.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Frame;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.smartfarm.ai.CustomView;
import com.smartfarm.ai.R;
import com.smartfarm.ai.SharedCameraActivity;
import com.smartfarm.common.helpers.DisplayRotationHelper;
import com.smartfarm.common.rendering.ObjectRenderer;
import com.smartfarm.common.rendering.ObjectWireFrameRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.common.rendering.geometry.Ray;
import com.smartfarm.common.rendering.geometry.Vector3;
import com.smartfarm.core.Component;
import com.smartfarm.core.components.objectDetect.ObjectDetectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ObjectDetectComponent extends Component {

    private static final String TAG = ObjectDetectComponent.class.getSimpleName();

    private ObjectDetector objectDetector;
    private Activity context;
    private CustomView view;

    private ObjectDetectMap odMap = new ObjectDetectMap();

    private LineString rays = new LineString();
    private List<Ray> rays2 = new ArrayList<>();
    private Integer removeIndex = null;

    private float[] viewmtx;
    private float[] projmtx;

    private final ObjectWireFrameRenderer virtualObject = new ObjectWireFrameRenderer();

    public ObjectDetectComponent(Activity context) {
        this.context = context;
        LocalModel localModel = new LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build();
        //LocalModel localModel = new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();

        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.9f)
                        .setMaxPerObjectLabelCount(1)
                        .build();

        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
        view = context.findViewById(R.id.customView);
        try {
            virtualObject.createOnGlThread(context, "models/cube.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void draw(float[] viewmtx, float[] projmtx, Frame frame) {

        this.projmtx = projmtx;

        float[] viewmtx2 = new float[16];
        frame.getCamera().getDisplayOrientedPose().toMatrix(viewmtx2, 0);
        this.viewmtx = viewmtx2;

        float scaleFactor = 0.025f;

        final float[] colorCorrectionRgba = new float[4];
        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);
        Set<Integer> keyset = odMap.getMap().keySet();
        for (Integer key : keyset) {
            if (odMap.getMap().get(key).getAvgDirectionLength() > 0.07) {

                virtualObject.updateModelMatrix(odMap.getMap().get(key).getAnchorMatrix(), scaleFactor);
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, new float[]{66.0f, 133.0f, 244.0f, 255.0f});
            }
        }
    }

    public void onImageAvailable(ImageReader imageReader, DisplayRotationHelper displayRotationHelper, String cameraId) {

        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            Log.w(TAG, "onImageAvailable: Skipping null image.");
            return;
        }
        if (projmtx == null) {
            image.close();
            return;
        }
        int rotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId);
        InputImage image2 = InputImage.fromMediaImage(image, rotation);
        float[] projmtxcopy = new float[16];
        System.arraycopy(this.projmtx, 0, projmtxcopy, 0, 16);

        float[] viewmtxcopy = new float[16];
        System.arraycopy(this.viewmtx, 0, viewmtxcopy, 0, 16);

        int imageWidth =  rotation != 90 ? image.getWidth() : image.getHeight();
        int imageHeight = rotation != 90 ? image.getHeight() : image.getWidth();

        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();



        objectDetector.process(image2).addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
            @Override
            public void onSuccess(List<DetectedObject> detectedObjects) {
                try {
                    odMap.detectedObjects(detectedObjects, imageWidth, imageHeight, viewWidth, viewHeight, projmtxcopy, viewmtxcopy);
                    view.render(odMap);
                } catch (Exception e) {
                } finally {
                    try {image.close();} catch (Exception e2) {}
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "인식실패");
                e.printStackTrace();
            }
        });
    }
}
