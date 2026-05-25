package com.dealplanner.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DealPlanner";
    private WebView webView;
    private ValueCallback<Uri[]> fileUploadCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1;
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private Uri cameraImageUri;
    private String pendingCameraCallback;
    
    // Speech recognition
    private SpeechRecognizer speechRecognizer;
    private String speechCallback;
    private boolean pendingSpeechStart;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions upfront
        requestPermissions();

        webView = findViewById(R.id.webView);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);

        // Add JavaScript interface for native camera
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidCamera");

        // Set WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        // Set WebChromeClient for file uploads and permissions
        webView.setWebChromeClient(new WebChromeClient() {
            
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                // Create chooser with both camera and gallery options
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                
                // Create file for camera image
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e(TAG, "Error creating image file", ex);
                }
                
                if (photoFile != null) {
                    cameraImageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.dealplanner.app.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                }

                // Gallery intent
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

                // Chooser
                Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});

                try {
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    Toast.makeText(MainActivity.this, "Cannot open chooser", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    String[] resources = request.getResources();
                    for (String resource : resources) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            request.grant(resources);
                            return;
                        }
                    }
                    request.deny();
                });
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, consoleMessage.message() + " -- Line " 
                        + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });

        // Load the app
        webView.loadUrl("file:///android_asset/index.html");
        
        // Setup speech recognizer
        setupSpeechRecognizer();
    }
    
    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available");
            return;
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }
            
            @Override
            public void onBeginningOfSpeech() {}
            
            @Override
            public void onRmsChanged(float rmsdB) {}
            
            @Override
            public void onBufferReceived(byte[] buffer) {}
            
            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "End of speech");
            }
            
            @Override
            public void onError(int error) {
                Log.e(TAG, "Speech error: " + error);
                String errorMsg = "error";
                if (error == SpeechRecognizer.ERROR_NETWORK) {
                    errorMsg = "network";
                }
                if (speechCallback != null) {
                    webView.evaluateJavascript(speechCallback + "Error('" + errorMsg + "')", null);
                }
            }
            
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d(TAG, "Speech result: " + text);
                    if (speechCallback != null) {
                        text = text.replace("'", "\\'");
                        webView.evaluateJavascript(speechCallback + "Result('" + text + "')", null);
                    }
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {}
            
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    // JavaScript interface for native camera access
    public class WebAppInterface {
        
        @JavascriptInterface
        public void openCamera(String callbackName) {
            pendingCameraCallback = callbackName;
            
            runOnUiThread(() -> {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) 
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, 
                            new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
                    return;
                }
                
                launchCamera();
            });
        }
        
        @JavascriptInterface
        public void openGallery(String callbackName) {
            pendingCameraCallback = callbackName;
            
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select Image"), FILE_CHOOSER_REQUEST_CODE + 100);
            });
        }
        
        @JavascriptInterface
        public boolean hasCameraSupport() {
            return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        }
        
        @JavascriptInterface
        public boolean hasSpeechSupport() {
            return SpeechRecognizer.isRecognitionAvailable(MainActivity.this);
        }
        
        @JavascriptInterface
        public void startSpeechRecognition(String callbackPrefix) {
            speechCallback = callbackPrefix;
            pendingSpeechStart = true;
            runOnUiThread(() -> {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                    return;
                }
                
                startSpeechRecognitionInternal();
            });
        }
        
        @JavascriptInterface
        public void stopSpeechRecognition() {
            runOnUiThread(() -> {
                if (speechRecognizer != null) {
                    try {
                        speechRecognizer.stopListening();
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping speech", e);
                    }
                }
            });
        }
    }
    
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(this,
                    "com.dealplanner.app.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            
            try {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            } catch (Exception e) {
                Log.e(TAG, "Error launching camera", e);
                Toast.makeText(this, "Cannot open camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        };

        boolean needsPermission = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }

                if (Manifest.permission.CAMERA.equals(permissions[i]) && pendingCameraCallback != null) {
                    launchCamera();
                    return;
                }

                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i]) && pendingSpeechStart) {
                    startSpeechRecognitionInternal();
                    return;
                }
            }
        }
    }

    private void startSpeechRecognitionInternal() {
        if (speechRecognizer == null) {
            setupSpeechRecognizer();
        }

        if (speechRecognizer == null) {
            Log.e(TAG, "Speech recognizer unavailable");
            pendingSpeechStart = false;
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            speechRecognizer.startListening(intent);
            pendingSpeechStart = false;
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle native camera result
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && cameraImageUri != null) {
                processImageAndCallback(cameraImageUri);
            } else {
                // Cancelled
                if (pendingCameraCallback != null) {
                    webView.evaluateJavascript(pendingCameraCallback + "(null)", null);
                    pendingCameraCallback = null;
                }
            }
            return;
        }
        
        // Handle gallery result from JavaScript interface
        if (requestCode == FILE_CHOOSER_REQUEST_CODE + 100) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                processImageAndCallback(data.getData());
            } else {
                if (pendingCameraCallback != null) {
                    webView.evaluateJavascript(pendingCameraCallback + "(null)", null);
                    pendingCameraCallback = null;
                }
            }
            return;
        }
        
        // Handle WebView file chooser result
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (fileUploadCallback != null) {
                Uri[] results = null;
                
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        // Gallery selection
                        results = new Uri[]{data.getData()};
                    } else if (cameraImageUri != null) {
                        // Camera capture
                        results = new Uri[]{cameraImageUri};
                    }
                }
                
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
        }
    }
    
    private void processImageAndCallback(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // Resize if too large (max 1920px on longest side)
            int maxSize = 1920;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            if (width > maxSize || height > maxSize) {
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                width = Math.round(width * scale);
                height = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            
            // Convert to base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            
            // Create data URL
            String dataUrl = "data:image/jpeg;base64," + base64Image;
            
            // Call JavaScript callback
            if (pendingCameraCallback != null) {
                String js = pendingCameraCallback + "('" + dataUrl + "')";
                webView.evaluateJavascript(js, null);
                pendingCameraCallback = null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            if (pendingCameraCallback != null) {
                webView.evaluateJavascript(pendingCameraCallback + "(null)", null);
                pendingCameraCallback = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Let the WebView handle back navigation via JavaScript
        webView.evaluateJavascript(
            "if (typeof handleAndroidBack === 'function') { handleAndroidBack(); } else { window.history.back(); }",
            null
        );
    }
}
