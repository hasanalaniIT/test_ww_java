package com.example.test_ww_java;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jlibrosa.audio.JLibrosa;
import com.jlibrosa.audio.exception.FileFormatNotSupportedException;
import com.jlibrosa.audio.wavFile.WavFileException;


import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;


public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.test_ww_java/asr";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL).setMethodCallHandler((call, result) -> {
            if (call.method.equals("process_audio")) {
                System.out.println("call recognize");
                String audioPath = call.argument("audio_path");

                float[] processedAudioResult = null;
                try {
                    processedAudioResult = new JlibrosaMFCC().processAudio(audioPath);
                    result.success(processedAudioResult);
                } catch (FileFormatNotSupportedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (WavFileException e) {
                    throw new RuntimeException(e);
                }


            }

            // This method is invoked on the main thread.
            // TODO
        });
    }

    public class JlibrosaMFCC {
        JLibrosa jLibrosa = new JLibrosa();
        NDArray mfccResult;
        float[] finalResult;
        int sampleRate = 16000;
        int nfcc = 40;

        public float[] processAudio(String audioPath) throws FileFormatNotSupportedException, IOException, WavFileException {
            String fullPath = assetFilePath(getApplicationContext(), audioPath);
            if (fullPath == null) return null;
            float[] audio_file = jLibrosa.loadAndRead(fullPath, sampleRate, 1);
            int[] axes = {0};
            sampleRate = jLibrosa.getSampleRate();
            float[][] mfccFeatures = jLibrosa.generateMFCCFeatures(audio_file, sampleRate, nfcc);
//            System.out.println("Audio Data======= " + Arrays.toString(audio_file));

            try (NDManager manager = NDManager.newBaseManager()) {
                NDArray nd = manager.create(mfccFeatures);
                NDArray mfccTransposed = nd.transpose();
                NDArray mfccMean = mfccTransposed.mean(axes);
                NDArray mfccExpanded = mfccMean.expandDims(0);
                mfccResult = mfccExpanded;
                finalResult = mfccResult.toFloatArray();
//                System.out.println("MFCC RESULT FROM JAVA :: " + Arrays.toString(finalResult));
            }
            return finalResult;
        }
    }

    private String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            System.out.println("file.getAbsolutePath() " + file.getAbsolutePath());

            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            System.out.println("file.getAbsolutePath() " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            System.out.println(assetName + ": " + e.getLocalizedMessage());
        }
        return null;
    }
    
}

