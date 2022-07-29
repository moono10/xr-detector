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
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.common.rendering.geometry.Ray;
import com.smartfarm.common.rendering.geometry.Vector3;
import com.smartfarm.core.Component;

import java.util.ArrayList;
import java.util.List;

public class ObjectDetectComponent extends Component {

    private static final String TAG = ObjectDetectComponent.class.getSimpleName();

    private ObjectDetector objectDetector;
    private Activity context;
    private CustomView view;

    private LineString rays = new LineString();

    private List<Ray> rays2 = new ArrayList<>();
    private Integer removeIndex = null;


    public ObjectDetectComponent(Activity context) {
        this.context = context;
        LocalModel localModel =
                new LocalModel.Builder()
                        .setAssetFilePath("custom_models/object_labeler.tflite")
                        .build();

        //LocalModel localModel =
        //
        //        new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();

        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.2f)
                        .setMaxPerObjectLabelCount(3)
                        .build();


        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);

       CustomView view = context.findViewById(R.id.customView);
    }

    @Override
    public void draw(float[] viewmtx, float[] projmtx, Frame frame) {

        float[] viewmtx2 = new float[16];
        frame.getCamera().getDisplayOrientedPose().toMatrix(viewmtx2, 0);


        float[] coords = view.getCoordinates();

        if (coords != null) {
            Vector3 origin = new Vector3(0, 0, 0);
            origin.setFromMatrixPosition(viewmtx2);

            Vector3 direction = new Vector3(0, 0, -1);

            direction.set(coords[0], coords[1], 0.5f).unproject(projmtx, viewmtx2).sub(origin).normalize();

            rays2.add(new Ray(new Vector3(origin.x, origin.y, origin.z), new Vector3(direction.x, direction.y, direction.z)));

            rays.getPointList().add(origin);
            rays.getPointList().add(direction.multiplyScalar(100.0f).add(origin));
        }



        //rayLineRenderer.update(rays);
        //rayLineRenderer.draw(viewmtx, projmtx);
        double min = Double.MAX_VALUE;
        removeIndex = null;
        if (rays2.size() > 0) {
            Vector3 v = new Vector3(0,0,0);
            int cnt2 = 0;
            for (int i = 0; i < rays2.size(); i++) {
                for (int j = i + 1; j < rays2.size(); j++) {
                    Ray ra1 = rays2.get(i);
                    Ray ra2 = rays2.get(j);
                    if (i < 40) {
                        double len = new Vector3(ra1.direction.x, ra1.direction.y, ra1.direction.z).sub(ra2.direction).length();
                        if (len < min) {
                            min = len;
                            removeIndex = i;
                        }
                    }
                    try {

                        Vector3[] ps = ra1.getSkewPoints(ra2);

                        ps[0].add(ps[1]).divideScalar(2.0f);
                        v.add(ps[0]);
                        cnt2++;
                    } catch (Exception e) {

                    }
                }



            }
            v.divideScalar(cnt2);
            //Log.e(TAG, "position : " + v.x + "," + v.y+ "," + v.z);

            float scaleFactor = 0.3f;
            float[] anchorMatrix2 = new float[]{1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1};
            anchorMatrix2[12] = v.x;
            anchorMatrix2[13] = v.y - 0.025f;
            anchorMatrix2[14] = v.z;
            // Update and draw the model and its shadow.
            //virtualObject.updateModelMatrix(anchorMatrix2, scaleFactor);
            // virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, new float[]{66.0f, 133.0f, 244.0f, 255.0f});

        }
        if (removeIndex != null && rays2.size() > 50) {
            Log.e(TAG, "removeIndex : " + removeIndex + ", " + rays2.size());
            rays2.remove(rays2.get(removeIndex));

            rays.getPointList().remove(rays.getPointList().get(removeIndex * 2 + 1));
            rays.getPointList().remove(rays.getPointList().get(removeIndex * 2));

        }
    }

    public void onImageAvailable(ImageReader imageReader) {

        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            Log.w(TAG, "onImageAvailable: Skipping null image.");
            return;
        }

        Log.d(TAG, "--------------------------------" + image.getWidth() + ", " + image.getHeight());



        int degree = 90;
        if (view.getWidth() > view.getHeight()) {
            degree = 0;
        }

        InputImage image2 = InputImage.fromMediaImage(image, degree);

        objectDetector.process(image2)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {


                                view.setDetectedObjects(detectedObjects, image.getWidth(), image.getHeight());

                                image.close();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "인식실패");
                                e.printStackTrace();
                                // Task failed with an exception
                                // ...
                                image.close();
                            }
                        });
    }
}
