package com.example.test_ww_java;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.content.Context;
import android.util.Log;

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
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;


public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.test_ww_java/asr";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL).setMethodCallHandler((call, result) -> {
            if (call.method.equals("process_audio")) {
                System.out.println("call recognize");
                String audioPath = call.argument("audio_path");

                float[] processedAudioResult;
                try {
                    processedAudioResult = new LibRosaMFCC().processAudio(audioPath);
                    result.success(processedAudioResult);
                } catch (FileFormatNotSupportedException | IOException | WavFileException e) {
                    throw new RuntimeException(e);
                }


            }

            // This method is invoked on the main thread.
            // TODO
        });
    }

    public class LibRosaMFCC {
        JLibrosa jLibrosa = new JLibrosa();
        NDArray mfccResult;
        float[] finalResult;
        int sampleRate = 16000;
        int nfcc = 40;

        public float[] processAudio(String audioPath) throws FileFormatNotSupportedException, IOException, WavFileException {
            String fullPath = assetFilePath(getApplicationContext(), audioPath);
            if (fullPath == null) return null;
            fullPath = convertM4AToWAV(fullPath);
            float[] audio_file = jLibrosa.loadAndRead(fullPath, sampleRate, 1);
            int[] axes = {0};
            sampleRate = jLibrosa.getSampleRate();
            float[][] mfccFeatures = jLibrosa.generateMFCCFeatures(audio_file, sampleRate, nfcc);
//            System.out.println("Audio Data======= " + Arrays.toString(audio_file));

            try (NDManager manager = NDManager.newBaseManager()) {
                NDArray nd = manager.create(mfccFeatures);
                NDArray mfccTransposed = nd.transpose();
                NDArray mfccMean = mfccTransposed.mean(axes);
                mfccResult = mfccMean.expandDims(0);
                finalResult = mfccResult.toFloatArray();
//                System.out.println("MFCC RESULT FROM JAVA :: " + Arrays.toString(finalResult));
            }
            return finalResult;
        }
    }

    public static String convertM4AToWAV(String inputPath) {
        String output= inputPath.replaceAll("\\.m4a$", ".wav");
        String[] cmd = {
                "-i", inputPath,
                "-acodec", "pcm_s16le",
                "-ar", "16000", // Set the sample rate to 16,000 Hz
                output
        };
        System.out.println(Arrays.toString(cmd));
        int rc = FFmpeg.execute(cmd);
        if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.");
            return null;
        }
        return output;
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

