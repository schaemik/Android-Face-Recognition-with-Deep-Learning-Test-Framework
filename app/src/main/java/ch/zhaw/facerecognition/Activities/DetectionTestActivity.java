package ch.zhaw.facerecognition.Activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.zhaw.facerecognition.R;
import ch.zhaw.facerecognitionlibrary.FaceRecognitionLibrary;
import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.Helpers.MatName;
import ch.zhaw.facerecognitionlibrary.Helpers.PreferencesHelper;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;
import ch.zhaw.facerecognitionlibrary.Recognition.Recognition;
import ch.zhaw.facerecognitionlibrary.Recognition.RecognitionFactory;

public class DetectionTestActivity extends AppCompatActivity {
    private static final String RESULT_NEGATIVE = "negative";
    private static final String RESULT_POSITIVE = "positive";
    TextView progress;
    Thread thread;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_test);
        progress = (TextView) findViewById(R.id.progressText);
        progress.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Handler handler = new Handler(Looper.getMainLooper());
        thread = new Thread(new Runnable() {
            public void run() {
                if(!Thread.currentThread().isInterrupted()){
                    PreProcessorFactory ppF = new PreProcessorFactory();

                    FileHelper fileHelper = new FileHelper();
                    File[] detectionFolders = fileHelper.getDetectionTestList();
                    if (detectionFolders.length > 0) {
                        // total and matches are used to calculate the accuracy afterwards
                        int total = 0;
                        int matches = 0;
                        List<String> results = new ArrayList<>();
                        results.add("Expected Name;Expected File;Result");
                        Date time_start = new Date();
                        for (File folder : detectionFolders) {
                            File[] files = folder.listFiles();
                            int counter = 1;
                            for (File file : files) {
                                if (FileHelper.isFileAnImage(file)) {
                                    Date time_preprocessing_start = new Date();
                                    Mat imgRgba = Imgcodecs.imread(file.getAbsolutePath());
                                    Imgproc.cvtColor(imgRgba, imgRgba, Imgproc.COLOR_BGRA2RGBA);

                                    List<Mat> images = ppF.getCroppedImage(imgRgba);

                                    String result = "";

                                    if (images == null) {
                                        result = RESULT_NEGATIVE;
                                    } else {
                                        result = RESULT_POSITIVE;
                                         // Save cropped images
                                        String[] tokens = file.getName().split("\\.");
                                        String name = tokens[0];
                                        for (int i=0; i<images.size();i++){
                                            MatName m = new MatName(name + "_" + (i + 1), images.get(i));
                                            fileHelper.saveMatToImage(m, FileHelper.RESULTS_PATH + "/" + time_start.toString() + "/");
                                        }
                                    }

                                    String[] tokens = file.getParent().split("/");
                                    final String name = tokens[tokens.length - 1];

                                    results.add(name + ";" + file.getName() + ";" + result);

                                    total++;

                                    if (name.equals(result)) {
                                        matches++;
                                    }
                                    // Update screen to show the progress
                                    final int counterPost = counter;
                                    final int filesLength = files.length;
                                    progress.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.append("Image " + counterPost + " of " + filesLength + " from " + name + "\n");
                                        }
                                    });
                                    counter++;
                                }
                            }
                        }
                        Date time_end = new Date();
                        long duration = time_end.getTime() - time_start.getTime();
                        int durationPerImage = (int) duration / total;
                        double accuracy = (double) matches / (double) total;
                        Map<String, ?> printMap = FaceRecognitionLibrary.sharedPreferences.getAll();
                        fileHelper.saveResultsToFile(printMap, accuracy, durationPerImage, results);

                        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("accuracy", accuracy);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(intent);
                            }
                        });
                    }
                } else {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        thread.interrupt();
    }

    @Override
    protected void onStop() {
        super.onStop();
        thread.interrupt();
    }
}
