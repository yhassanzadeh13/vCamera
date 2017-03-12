package com.example.yahya.camera;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class MainActivity extends Activity implements RecognitionListener
    {
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final String KEY_CAMERA = "camera";
    private final int REQ_CODE_SPEECH_INPUT = 100;
    static final int REQ_IMAGE_CAPTURE = 1;
    private boolean isRecording = false;
    private boolean isTakingPhoto = false;
    //*****************Current Camera Parameters****************************************************
    private static int currnetCamera = 0;
    private static String currnetFlashMode = Camera.Parameters.FLASH_MODE_OFF;
    private static String currentFocusMode = Camera.Parameters.FOCUS_MODE_AUTO;
    private static String currentEffect    = Camera.Parameters.EFFECT_NONE;
    private static String currentSenseMode = Camera.Parameters.SCENE_MODE_AUTO;
    private static String currentWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
    //**********************************************************************************************

    ImageButton recordButton;
    File recordFile;
    ImageButton captureButton;
    ImageButton flashButton;
    ImageView recordSign;
    FrameLayout preview;
    ImageButton flipButton;
    private SpeechRecognizer recognizer;
    private boolean buttonVisibility = false;
    //*******************************************************************
    //Speech Recognizer Variables and Constants
    /* Named searches allow to quickly reconfigure the decoder */

    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";
    /* Keyword we are looking for to activate menu */
    //private static final String KEYPHRASE = "camera wakeup please";
    //********************************************************************

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        prepareCamera();

        //Buttons Initialization
        captureButtonInit();
        recordButtonInit();
        flashButtonInit();
        frameLayoutInit();
        flipButtonInit();
        if(savedInstanceState != null)
        {
            currnetFlashMode = savedInstanceState.getString("FLASH");
            currentFocusMode = savedInstanceState.getString("FOCUS");
            currentEffect    = savedInstanceState.getString("EFFECT");
            currentSenseMode = savedInstanceState.getString("SCENE");
            currentWhiteBalance = savedInstanceState.getString("WHITE");
            if(savedInstanceState.getInt("CAMERA") != Camera.CameraInfo.CAMERA_FACING_BACK)
                toggleCamera();

            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(currnetFlashMode);
            if(currnetFlashMode == Camera.Parameters.FLASH_MODE_AUTO)
                flashButton.setImageResource(R.drawable.ic_camera_flash_auto_512);
            else if (currnetFlashMode == Camera.Parameters.FLASH_MODE_OFF)
                flashButton.setImageResource(R.drawable.ic_camera_flash_off_128);
            else
                flashButton.setImageResource(R.drawable.ic_thunder_512);
            p.setColorEffect(currentEffect);
            p.setFocusMode(currentFocusMode);
            p.setWhiteBalance(currentWhiteBalance);
            p.setSceneMode(currentSenseMode);

            mCamera.setParameters(p);



        }
        adjustCameraRotation();
        speechRecognizerInit();

    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
//            File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MyImages");
//            imagesFolder.mkdirs(); // <----
//            File pictureFile = new File(imagesFolder, "image_001.jpg");
//
//            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//            bmp = rotateImage(bmp, 90);
//            MediaStore.Images.Media.insertImage(getContentResolver(), bmp, Calendar.getInstance().toString(), Calendar.getInstance().toString());
//            mCamera.startPreview();

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null)
            {

                return;
            }

            try
            {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            }
            catch (Exception e)
            {

            }

            //Do a scan for the recently saved file
            MediaScannerConnection.scanFile(MainActivity.this, new String[]{recordFile.getAbsolutePath()}, null, null);

            mCamera.startPreview();
            isTakingPhoto = false;

        }
    };

    /** A safe way to get an instance of the Camera object. */
    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public static Bitmap rotateImage(Bitmap src, float degree) {
        // create new matrix
        Matrix matrix = new Matrix();
        // setup rotation degree
        matrix.postRotate(degree);
        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        return bmp;
    }



    private boolean prepareVideoRecorder()
    {

        //mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try
            {
            mMediaRecorder.prepare();
            }
        catch (IllegalStateException e)
        {
            //Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        catch (IOException e)
        {

            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder()
    {
        if (mMediaRecorder != null)
        {
//            mMediaRecorder.stop();
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseCamera()
    {
        if (mCamera != null)
        {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type)
        {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

       // File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VCC")
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "vCamera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        recordFile = mediaFile;
        return mediaFile;
    }

    @Override
    protected void onStop()
        {
            //mCamera.release();
            //releaseMediaRecorder();
            super.onStop();
        }

    @Override
    protected void onDestroy()
        {
            //mCamera.release();
            //mMediaRecorder.release();
            super.onDestroy();
            if(recognizer!=null)
            {
                recognizer.cancel();
                recognizer.shutdown();
            }

        }

    @Override
    protected void onResume()
    {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getActionBar();

        if(actionBar != null)
            actionBar.hide();
        super.onResume();

    }



    void setRecordSignVisibility(boolean flag)
    {
        if(flag)
            {
                recordSign.setVisibility(View.VISIBLE);
                recordButton.setImageResource(R.drawable.ic_player_stop_outline_128);
            }
        else
            {
                recordSign.setVisibility(View.INVISIBLE);
                recordButton.setImageResource(R.drawable.ic_movie_film_icon);
            }

    }
    //***********************************Camera Part****************************
    private void prepareCamera()
        {
            // Open up the back camera
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
    public void toggleCamera()
        {
            //releaseMediaRecorder();
            mCamera.stopPreview();
            mCamera.release();
            if(currnetCamera == Camera.CameraInfo.CAMERA_FACING_BACK)
                {
                    currnetCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
            else
                currnetCamera = Camera.CameraInfo.CAMERA_FACING_BACK;

            mCamera  = Camera.open(currnetCamera);
            preview.removeView(mPreview);
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview);

            captureButton.bringToFront();
            recordButton.bringToFront();
            flashButton.bringToFront();
            flipButton.bringToFront();

        }

        public void prepareForStop()
        {
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder
            setRecordSignVisibility(false);
            Toast.makeText(MainActivity.this, recordFile.getAbsolutePath().toString(), Toast.LENGTH_LONG).show();
            // inform the user that recording has stopped
            isRecording = false;
            //Now that we have stoped the recorder we need to launch the speech recognizer back
            speechRecognizerInit();
        }

        public void prepareForAction()
        {
            // initialize video camera
            if (prepareVideoRecorder())
            {
                //First we need to shutdown the speech recognizer, since it uses the mic and hence
                //If we want to use the camera recording as well then we will face a problem
                recognizer.cancel();
                recognizer.shutdown();
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();
                setRecordSignVisibility(true);
                // inform the user that recording has started
                //recordButton.setText("Stop");
                isRecording = true;

                //Do a scan for the recently saved file
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{recordFile.getAbsolutePath()}, null, null);
            }
            else
            {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                // inform user
            }
        }

        public void captureButtonInit()
            {
                // Add a listener to the Capture button
                captureButton = (ImageButton) findViewById(R.id.button_capture);
                captureButton.bringToFront();
                captureButton.setOnClickListener(
                        new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                    {
                                        capturePhoto();
                                    }
                            }
                );
            }

        public void recordButtonInit()
            {
                recordSign = (ImageView) findViewById(R.id.img_rec_sign);
                recordSign.setVisibility(View.INVISIBLE);
                // Add a listener to the record button
                recordButton = (ImageButton) findViewById(R.id.button_record);
                recordButton.bringToFront();
                recordButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v)
                                {
                                    if (isRecording)
                                        {
                                            prepareForStop();
                                        }
                                    else
                                        {
                                            prepareForAction();
                                        }
                                }
                        }
                );
            }

        public void flashButtonInit()
            {
                // Add a listener to the record button
                flashButton = (ImageButton) findViewById(R.id.button_flash);
                flashButton.bringToFront();
                flashButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v)
                                {
                                    flashModeUpdate();
                                }
                        }
                );
            }

        public void capturePhoto()
            {
                isTakingPhoto = true;
                mCamera.takePicture(null, null, mPicture);

            }

        private void flashModeUpdate()
            {
                Camera.Parameters p = mCamera.getParameters();
                if(currnetFlashMode == Camera.Parameters.FLASH_MODE_OFF)
                    {
                        currnetFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
                        flashButton.setImageResource(R.drawable.ic_camera_flash_auto_512);
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    }
                else if(currnetFlashMode == Camera.Parameters.FLASH_MODE_AUTO)
                    {
                        currnetFlashMode = Camera.Parameters.FLASH_MODE_ON;
                        flashButton.setImageResource(R.drawable.ic_thunder_512);
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    }
                else
                    {
                            currnetFlashMode = Camera.Parameters.FLASH_MODE_OFF;
                            flashButton.setImageResource(R.drawable.ic_camera_flash_off_128);
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                mCamera.setParameters(p);

            }
        private void frameLayoutInit()
            {
//                recordButton.setVisibility(View.INVISIBLE);
//                flashButton.setVisibility(View.INVISIBLE);
//                captureButton.setVisibility(View.INVISIBLE);
                preview.setOnClickListener(
                      new View.OnClickListener()
                          {
                              @Override
                              public void onClick(View v)
                                  {
                                      if(buttonVisibility)
                                          {
                                              buttonVisibility = false;
                                              flipButton.setVisibility(View.INVISIBLE);
                                              recordButton.setVisibility(View.INVISIBLE);
                                              flashButton.setVisibility(View.INVISIBLE);
                                              captureButton.setVisibility(View.INVISIBLE);
                                          }
                                      else
                                          {
                                              buttonVisibility = true;
                                              flipButton.setVisibility(View.VISIBLE);
                                              recordButton.setVisibility(View.VISIBLE);
                                              flashButton.setVisibility(View.VISIBLE);
                                              captureButton.setVisibility(View.VISIBLE);
                                          }
                                  }
                          }
                );
            }
        private void flipButtonInit()
            {
                flipButton = (ImageButton) findViewById(R.id.button_flip);
                flipButton.bringToFront();
                flipButton.setOnClickListener(
                        new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                    {
                                        toggleCamera();
                                    }
                            }
                );
            }

        private void adjustCameraRotation()
        {

                mCamera.setDisplayOrientation(90);
        }


    //***********************************Speech Part****************************

    private void speechRecognizerInit()
        {
            //Speech Recognizer Initialization
            // Recognizer initialization is a time-consuming and it involves IO,
            // so we execute it in async task

            new AsyncTask<Void, Void, Exception>() {
                @Override
                protected Exception doInBackground(Void... params)
                    {
                        try
                            {
                                Assets assets = new Assets(MainActivity.this);
                                File assetDir = assets.syncAssets();
                                setupRecognizer(assetDir);
                            }
                        catch (IOException e)
                            {
                                return e;
                            }
                        return null;
                    }

                @Override
                protected void onPostExecute(Exception result)
                    {
                        if (result != null)
                            {
                                Toast.makeText(MainActivity.this, "Failed to initialize the speech recognizer :(", Toast.LENGTH_LONG).show();
                            }
                        else
                            {
                                Toast.makeText(MainActivity.this, "For help say: Ok camera help command (or setting)", Toast.LENGTH_LONG).show();
                                switchSearch(KWS_SEARCH);
                            }
                    }
            }.execute();
        }
    private void setupRecognizer(File assetsDir) throws IOException
        {
            // The recognizer can be configured to perform multiple searches
            // of different kind and switch between them

            recognizer = defaultSetup()
                    .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                    .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                    // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                    .setRawLogDir(assetsDir)

                    // Threshold to tune for keyphrase to balance between false alarms and misses
                    .setKeywordThreshold(1e-45f)

                    // Use context-independent phonetic search, context-dependent is too slow for mobile
                    .setBoolean("-allphone_ci", true)

                    .getRecognizer();
            recognizer.addListener(this);

            /** In your application you might not need to add all those searches.
             * They are added here for demonstration. You can leave just one.
             */

//            // Create keyword-activation search.
//            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

            // Create grammar-based search for selection between demos
            File menuGrammar = new File(assetsDir, "menu.gram");
            recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        }

    private void switchSearch(String searchName)
        {
            recognizer.stop();

            // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
            if (searchName.equals(KWS_SEARCH))
                recognizer.startListening(MENU_SEARCH);
            else
                recognizer.startListening(MENU_SEARCH, 10000);
        }

    @Override
    public void onBeginningOfSpeech()
        {

        }

    @Override
    public void onEndOfSpeech()
        {
            if (!recognizer.getSearchName().equals(KWS_SEARCH))
                switchSearch(MENU_SEARCH);
        }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis)
        {
//            if (hypothesis == null)
//                return;
//
//            String text = hypothesis.getHypstr();
//            Toast.makeText(MainActivity.this, "Partial " + text, Toast.LENGTH_LONG).show();

            if (hypothesis == null)
                return;

            String text = hypothesis.getHypstr();
//            if (text.equals(KEYPHRASE))
//                switchSearch(MENU_SEARCH);
//            else if (text.equals(DIGITS_SEARCH))
//                switchSearch(DIGITS_SEARCH);
//            else if (text.equals(PHONE_SEARCH))
//                switchSearch(PHONE_SEARCH);
//            else if (text.equals(FORECAST_SEARCH))
//                switchSearch(FORECAST_SEARCH);
        }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis)
        {
            if (hypothesis != null)
                {
                    String text = hypothesis.getHypstr();
                    Interpret(text);
//                    switchSearch(KWS_SEARCH);
                }
        }

    @Override
    public void onError(Exception e)
        {

        }

    @Override
    public void onTimeout()
        {
            switchSearch(KWS_SEARCH);
        }

        @Override
        protected void onSaveInstanceState(Bundle savedInstanceState)
        {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putString("FLASH", currnetFlashMode);
            savedInstanceState.putInt("CAMERA", currnetCamera);
            savedInstanceState.putString("SCENE", currentSenseMode);
            savedInstanceState.putString("WHITE",currentWhiteBalance);
            savedInstanceState.putString("EFFECT",currentEffect);
            savedInstanceState.putString("FOCUS", currentFocusMode);
        }

        //**********************Interpreter*************************************
        public void Interpret(String message)
        {
            message = message.replaceAll(" the", "");
            message = message.replaceAll(" to", " ");
            message = message.replaceAll(" a ", " ");
            message = message.replaceAll("\\s+", " ");
            String[] split = message.split(" ");
            //If the command does not starts with the key phrase
            if(!(split[0].toLowerCase().contains("ok")
                    && split[1].toLowerCase().contains("camera")
                    ) || split.length < 3)
                return;


            else
            {
                if(split[2].contains("switch"))
                {
                    toggleCamera();
                }

                else if (split[2].contains("open") && split.length > 3 && split[3].contains("gallery"))
                {
                    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "vCamera");
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivity(galleryIntent);

                }

                else if (split[2].contains("zoom") && split.length > 3 && split[3].contains("in"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(p.getZoom() < p.getMaxZoom() - 2)
                            p.setZoom(p.getZoom() + 2);
                        mCamera.setParameters(p);
                    }
                else if (split[2].contains("zoom") && split.length > 3 && split[3].contains("out"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(p.getZoom() > 2)
                            p.setZoom(p.getZoom() - 2);
                        mCamera.setParameters(p);
                    }
//                else if(split[2].contains("action"))
//                {
//                    if (isRecording)
//                    {
//                        prepareForStop();
//                        prepareForAction();
//                    }
//                    else
//                    {
//                        prepareForAction();
//                    }
//                }
//
//                else if(split[2].contains("stop"))
//                {
//                    prepareForStop();
//                }

//                else if(split[2].contains("capture"))
//                {
//                    capturePhoto();
//                }

                /*
                help part
                 */
                else if(split[2].contains("help"))
                {

                    if(split[3].contains("setting"))
                    {
                        Toast.makeText(MainActivity.this, "Say ok camera, \"help effect mode\" or \"help scene mode\" or \"help flashlight\" or  or \"help white balance mode\" ", Toast.LENGTH_LONG).show();
                    }

                    /*
                    help setting
                     */
                    else if(split[3].contains("effect"))
                    {
                        Toast.makeText(MainActivity.this, "You can say Ok camera set effect to mono (or negative or whiteboard or blackboard auto or solar) ", Toast.LENGTH_LONG).show();
                    }

                    else if(split[3].contains("scene"))
                    {
                        Toast.makeText(MainActivity.this, "You can say Ok camera set scene to auto (or action or candle or firework or beach or landscape or portrait or snow or sport) ", Toast.LENGTH_LONG).show();
                    }

                    else if(split[3].contains("white"))
                    {
                        Toast.makeText(MainActivity.this, "You can say Ok camera set white to auto (or cloudy or daylight) ", Toast.LENGTH_LONG).show();
                    }

                    else if(split[3].contains("flash") && split[4].contains("light"))
                    {
                        Toast.makeText(MainActivity.this, "You can say Ok camera set flashlight to on (or off or auto)  ", Toast.LENGTH_LONG).show();
                    }

                    /*
                    help command
                     */
                    else if(split[3].contains("command"))
                    {
                        Toast.makeText(MainActivity.this, "You can say Ok camera take a photo (or record a video or switch or take three photo) ", Toast.LENGTH_LONG).show();
                    }

                }

                else if(split[2].toLowerCase().contains("take") && split[3].toLowerCase().contains("photo"))
                    {
                        capturePhoto();
                    }

                else if(split[2].toLowerCase().contains("record"))
                    {
                        if (isRecording)
                            {
                                prepareForStop();
                                prepareForAction();
                            }
                        else
                            {
                                prepareForAction();
                            }
                    }


                else if(split[2].toLowerCase().contains("take"))
                {
                    int photoNumber = 1;
                    if (split[3].toLowerCase().contains("one"))
                        photoNumber = 1;
                    else if (split[3].toLowerCase().contains("two"))
                        photoNumber = 2;
                    else if (split[3].toLowerCase().contains("three"))
                        photoNumber = 3;
                    else if (split[3].toLowerCase().contains("four"))
                        photoNumber = 4;
                    else if (split[3].toLowerCase().contains("five"))
                        photoNumber = 5;
                    else if (split[3].toLowerCase().contains("six"))
                        photoNumber = 6;
                    else if (split[3].toLowerCase().contains("seven"))
                        photoNumber = 7;
                    else if (split[3].toLowerCase().contains("eight"))
                        photoNumber = 8;
                    else if (split[3].toLowerCase().contains("nine"))
                        photoNumber = 9;
                    else if (split[3].toLowerCase().contains("ten"))
                        photoNumber = 10;

                    final int q = photoNumber;
                    new Thread(new Runnable()
                    {
                        public void run() {

                            int count = 0;

                            while (count < q)
                            {

                                mCamera.takePicture(null, null, mPicture);

                                count++;

                                try
                                {
                                    Thread.sleep(1000);
                                } catch (InterruptedException exception)
                                {
                                    exception.printStackTrace();
                                }
                            }
                        }
                    }).start();

                }

                else if(split[2].toLowerCase().contains("set"))
                {
                    //Flash Light
                    if(split[3].toLowerCase().contains("flash") && split[4].toLowerCase().contains("light"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(split[5].toLowerCase().contains("auto"))
                        {
                            currnetFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
                            flashButton.setImageResource(R.drawable.ic_camera_flash_auto_512);
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                            Toast.makeText(MainActivity.this, "Flash Light was changed to auto", Toast.LENGTH_LONG).show();
                        }
                        else if(split[5].toLowerCase().contains("on"))
                        {
                            currnetFlashMode = Camera.Parameters.FLASH_MODE_ON;
                            flashButton.setImageResource(R.drawable.ic_thunder_512);
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                            Toast.makeText(MainActivity.this, "Flash Light was changed to on", Toast.LENGTH_LONG).show();
                        }
                        else if(split[5].toLowerCase().contains("off"))
                        {
                            currnetFlashMode = Camera.Parameters.FLASH_MODE_OFF;
                            flashButton.setImageResource(R.drawable.ic_camera_flash_off_128);
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            Toast.makeText(MainActivity.this, "Flash Light was changed to off", Toast.LENGTH_LONG).show();
                        }

                        mCamera.setParameters(p);

                    }
                    /*
                    Focus mode
                     */
                    else if(split[3].toLowerCase().contains("focus") && split[4].toLowerCase().contains("mode"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(split[5].toLowerCase().contains("auto"))
                        {
                            currentFocusMode = Camera.Parameters.FOCUS_MODE_AUTO;
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            Toast.makeText(MainActivity.this, "Focus mode was changed to auto", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("depth"))
                        {
                            currentFocusMode = Camera.Parameters.FOCUS_MODE_EDOF;
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                            Toast.makeText(MainActivity.this, "Focus mode was changed to depth", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("fixed"))
                        {
                            currentFocusMode = Camera.Parameters.FOCUS_MODE_FIXED;
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                            Toast.makeText(MainActivity.this, "Focus mode was changed to fixed", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("infinity"))
                        {
                            currentFocusMode = Camera.Parameters.FOCUS_MODE_INFINITY;
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                            Toast.makeText(MainActivity.this, "Focus mode was changed to infinity", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("close"))
                        {
                            currentFocusMode = Camera.Parameters.FOCUS_MODE_MACRO;
                            p.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                            Toast.makeText(MainActivity.this, "Focus mode was changed to close", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("continuous") && split.length > 6)
                        {
                            if(split[6].toLowerCase().contains("picture"))
                            {
                                currentFocusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                                p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                Toast.makeText(MainActivity.this, "Focus mode was changed to continuous picture", Toast.LENGTH_LONG).show();
                            }

                            else if(split[6].toLowerCase().contains("picture"))
                            {
                                currentFocusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
                                p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                                Toast.makeText(MainActivity.this, "Focus mode was changed to continuous video", Toast.LENGTH_LONG).show();
                            }


                        }

                        mCamera.setParameters(p);
                    }

                    /*
                    Effect mode
                     */
                    else if(split[3].toLowerCase().contains("effect") && split[4].toLowerCase().contains("mode"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(split[5].toLowerCase().contains("mono"))
                        {
                            currentEffect = Camera.Parameters.EFFECT_MONO;
                            p.setColorEffect(Camera.Parameters.EFFECT_MONO);
                            Toast.makeText(MainActivity.this, "Effect mode was changed to mono", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("negative"))
                        {
                            currentEffect = Camera.Parameters.EFFECT_NEGATIVE;
                            p.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                            Toast.makeText(MainActivity.this, "Effect mode was changed to negative", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("auto"))
                        {
                            currentEffect = Camera.Parameters.EFFECT_NONE;
                            p.setColorEffect(Camera.Parameters.EFFECT_NONE);
                            Toast.makeText(MainActivity.this, "Effect mode was changed to auto", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("solar"))
                        {
                            currentEffect = Camera.Parameters.EFFECT_SOLARIZE;
                            p.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
                            Toast.makeText(MainActivity.this, "Effect mode was changed to solar", Toast.LENGTH_LONG).show();
                        }


                        else if(split[5].toLowerCase().contains("black") && split.length > 6)
                        {
                            if(split[6].toLowerCase().contains("board"))
                            {
                                currentEffect = Camera.Parameters.EFFECT_BLACKBOARD;
                                p.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
                                Toast.makeText(MainActivity.this, "Effect mode was changed to blackboard", Toast.LENGTH_LONG).show();
                            }
                        }

                        else if(split[5].toLowerCase().contains("white") && split.length > 6)
                        {
                            if(split[6].toLowerCase().contains("board"))
                            {
                                currentEffect = Camera.Parameters.EFFECT_WHITEBOARD;
                                p.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD);
                                Toast.makeText(MainActivity.this, "Effect mode was changed to whiteboard", Toast.LENGTH_LONG).show();
                            }
                        }

                        mCamera.setParameters(p);
                    }

                    /*
                    Scene Mode
                     */
                    else if(split[3].toLowerCase().contains("scene") && split[4].toLowerCase().contains("mode"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(split[5].toLowerCase().contains("auto"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_AUTO;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to auto", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("action"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_ACTION;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to action", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("candle"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_CANDLELIGHT;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_CANDLELIGHT);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to candle", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("firework"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_FIREWORKS;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_FIREWORKS);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to firework", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("beach"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_BEACH;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_BEACH);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to beach", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("landscape"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_LANDSCAPE;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_LANDSCAPE);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to landscape", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("portrait"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_PORTRAIT;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_PORTRAIT);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to portrait", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("snow"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_SNOW;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_SNOW);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to snow", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("sport"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_SPORTS;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to sport", Toast.LENGTH_LONG).show();
                        }

                        else if(split[5].toLowerCase().contains("balance"))
                        {
                            currentSenseMode = Camera.Parameters.SCENE_MODE_STEADYPHOTO;
                            p.setSceneMode(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
                            Toast.makeText(MainActivity.this, "Scene mode was changed to steady", Toast.LENGTH_LONG).show();
                        }


                        mCamera.setParameters(p);
                    }

                    /*
                    White balance Mode
                     */
                    else if(split[3].toLowerCase().contains("white") && split[4].toLowerCase().contains("balance"))
                    {
                        Camera.Parameters p = mCamera.getParameters();
                        if(split[6].toLowerCase().contains("auto"))
                        {
                            currentWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
                            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                            Toast.makeText(MainActivity.this, "White balance was changed to auto", Toast.LENGTH_LONG).show();
                        }

                        else if(split[6].toLowerCase().contains("cloudy"))
                        {
                            currentWhiteBalance = Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT;
                            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
                            Toast.makeText(MainActivity.this, "White balance was changed to cloudy", Toast.LENGTH_LONG).show();
                        }

                        else if(split[6].toLowerCase().contains("daylight"))
                        {
                            currentWhiteBalance = Camera.Parameters.WHITE_BALANCE_DAYLIGHT;
                            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
                            Toast.makeText(MainActivity.this, "White balance was changed to daylight", Toast.LENGTH_LONG).show();
                        }



                        mCamera.setParameters(p);
                    }

                }


//                    mCamera.takePicture(null, null, mPicture);
//                else if(split[1].toLowerCase().contains("action"))
//                {

//                }
//
//                else if(split[1].toLowerCase().contains("stop"))
//                {
//                    prepareForStop();
//                }
            }
        }




    }




