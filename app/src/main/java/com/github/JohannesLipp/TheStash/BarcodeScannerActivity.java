package com.github.JohannesLipp.TheStash;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "BarcodeScannerActivity";
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private boolean isBarcodeDetected = false; // avoids multiple triggers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_barcode_scanner);
        previewView = findViewById(R.id.viewFinder);

        cameraExecutor = Executors.newSingleThreadExecutor();
        checkCameraPermissionAndStartCamera();
    }

    private void checkCameraPermissionAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission has already been granted, start the camera
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, start the camera
                startCamera();
            } else {
                // Permission denied, handle appropriately (e.g., show a message to the user)
                Log.e(TAG, "Camera permission denied");
                Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_LONG).show();
                finish(); // Finish if permission is denied, as camera is crucial
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // Configure BarcodeScanner (optional, but good for specifying formats)
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS // Or specify particular formats like EAN_13, QR_CODE, etc.
                        )
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
                Toast.makeText(this, "Error starting camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        //.setTargetResolution(new Size(1280, 720)) // Optional: set target resolution
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Process only the latest frame
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (!isBarcodeDetected) { // Only process if a barcode hasn't been detected yet
                processImageProxy(barcodeScanner, imageProxy);
            } else {
                imageProxy.close(); // Close the image if we've already detected a barcode
            }
        });

        try {
            cameraProvider.unbindAll(); // Unbind previous use cases before rebinding
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed: " + e.getMessage(), e);
        }
    }

    private void processImageProxy(BarcodeScanner scanner, ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (isBarcodeDetected) { // Double check, in case of race condition
                        imageProxy.close(); // Make sure to close if already detected
                        return;
                    }
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.isEmpty()) {
                            Log.d(TAG, "Barcode detected: " + rawValue);
                            isBarcodeDetected = true; // Set flag
                            sendResult(rawValue);

                            // Optional: Stop the camera and scanner after detection
                            if (cameraProviderFuture != null && cameraProviderFuture.isDone()) {
                                try {
                                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                                    cameraProvider.unbindAll();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error unbinding camera", e);
                                }
                            }
                            barcodeScanner.close(); // Release scanner resources

                            break; // Process only the first detected barcode
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Barcode scanning failed: " + e.getMessage(), e))
                .addOnCompleteListener(task -> {
                    // Crucial: Close the ImageProxy in all cases to allow processing of the next frame.
                    // If you don't close it, the camera will stop sending frames.
                    imageProxy.close();
                });
    }

    private void sendResult(String barcodeValue) {
        // Ensure this runs on the main thread if you are updating UI or finishing activity
        runOnUiThread(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("barcode", barcodeValue);
            setResult(RESULT_OK, resultIntent);
            Log.d(TAG, "Sending result and finishing activity.");
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown(); // Shutdown the executor
        if (barcodeScanner != null) {
            barcodeScanner.close(); // Release ML Kit scanner resources
        }
        // Unbinding the camera is generally handled by the lifecycle,
        // but explicitly unbinding in onDestroy if the activity is finishing
        // due to barcode detection can be done in sendResult or here.
    }
}