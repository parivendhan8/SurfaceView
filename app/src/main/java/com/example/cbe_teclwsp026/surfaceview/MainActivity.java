package com.example.cbe_teclwsp026.surfaceview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Size preview_size;
    Size JPEG_size[] = null;

    TextureView textureView;
    CameraDevice cameraDevice;
    CaptureRequest.Builder previewBuilder;
    CameraCaptureSession previewSession;
    CameraManager cameraManager;

    Button getPicture;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.textureView);

        textureView.setSurfaceTextureListener(surfaceTextureListener);

        getPicture = (Button) findViewById(R.id.getpicture);

        getPicture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                try {

                    getPicture();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }


            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void getPicture() throws CameraAccessException {

        if (cameraDevice == null)
            return;

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            if (cameraCharacteristics != null) {


                JPEG_size = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

                int width = 640, height = 480;

                if (JPEG_size != null && JPEG_size.length > 0) {

                    width = JPEG_size[0].getWidth();
                    height = JPEG_size[0].getHeight();
                }

                final ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

                List<Surface> output_surface = new ArrayList<Surface>(2);

                output_surface.add(imageReader.getSurface());
                output_surface.add(new Surface(textureView.getSurfaceTexture()));

                final CaptureRequest.Builder capture_Builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

                capture_Builder.addTarget(imageReader.getSurface());
                capture_Builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

                int rotation = getWindowManager().getDefaultDisplay().getRotation();

                capture_Builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                ImageReader.OnImageAvailableListener ImageAvailableListener =

                        new ImageReader.OnImageAvailableListener() {
                            @Override
                            public void onImageAvailable(ImageReader reader) {

                                Image image = null;

                                try {

                                    image = imageReader.acquireLatestImage();
                                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                    byte[] bytes = new byte[buffer.capacity()];
                                    buffer.get(bytes);

                                } catch (Exception e) {

                                } finally {

                                    if (image != null)
                                        image.close();

                                }
                            }

                            void save(byte[] bytes) {
                                File file = getOutputMediaFile();

                                OutputStream outputStream = null;
                                try {
                                    outputStream = new FileOutputStream(file);
                                    outputStream.write(bytes);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                        if (outputStream != null)
                                            outputStream.close();
                                    } catch (Exception e) {
                                    }
                                }
                            }


                        };


                HandlerThread handlerThread = new HandlerThread("takepicture");
                handlerThread.start();
                final Handler handler = new Handler(handlerThread.getLooper());
                imageReader.setOnImageAvailableListener(ImageAvailableListener, handler);

                final CameraCaptureSession.CaptureCallback previewSSession = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                    }

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);

                        startCamera();
                    }
                };


                cameraDevice.createCaptureSession(output_surface, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            session.capture(capture_Builder.build(), previewSSession, handler);
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                    }
                }, handler);

            }


        } catch (Exception e) {


        }

    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String camerId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(camerId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            preview_size = map.getOutputSizes(SurfaceTexture.class)[0];
            manager.openCamera(camerId, stateCallback, null);
        } catch (Exception e) {
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };




    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();

        if (cameraDevice != null)
        {
            cameraDevice.close();
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void startCamera()
    {

        if(cameraDevice == null || !textureView.isAvailable())
        {
            return;
        }

        SurfaceTexture texture= textureView.getSurfaceTexture();
        if (texture == null)
        {
            return;
        }

        texture.setDefaultBufferSize(preview_size.getWidth(),preview_size.getHeight());
        Surface surface=new Surface(texture);


        try
        {
            previewBuilder= cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        previewBuilder.addTarget(surface);

        try {

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {

                    getChangedPreview();

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);

        }catch (Exception e)
        {

        }

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void getChangedPreview() {
        if (cameraDevice == null)
            return;
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("changed Preview");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
        } catch (Exception e)
        {

        }
    }


    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "MyCameraApp");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }
}
