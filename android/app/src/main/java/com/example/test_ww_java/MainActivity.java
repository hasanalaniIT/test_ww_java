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

                float[] batteryLevel = null;
                try {
                    batteryLevel = new JlibrosaMFCC().processAudio(audioPath);
                    result.success(batteryLevel);
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
//            System.out.println("audio_file_Librosa==== " + Arrays.toString(audio_file));
            int[] axes = {0};
            sampleRate = jLibrosa.getSampleRate();
            float[][] mfccFeatures = jLibrosa.generateMFCCFeatures(audio_file, sampleRate, nfcc);


//            System.out.println(Arrays.deepToString(mfccFeatures));
//            System.out.println("sampleRate==== " + sampleRate);
//            System.out.println("Length of mfccFeatures==== " + mfccFeatures.length);

            try (NDManager manager = NDManager.newBaseManager()) {
                NDArray nd = manager.create(mfccFeatures);
//                System.out.println("Numpy Array=====" + Arrays.toString(nd.toFloatArray()));

                NDArray mfccTransposed = nd.transpose();
                NDArray mfccMean = mfccTransposed.mean(axes);
                NDArray mfccExpanded = mfccMean.expandDims(0);
                mfccResult = mfccExpanded;
                finalResult = mfccResult.toFloatArray();
                System.out.println("MFCC RESULT FROM JAVA :: " + Arrays.toString(finalResult));
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

//    public class MFCCExtractor {
//        public double[] processAudio(String audioPath, int sampleRate, int nMfcc) throws IOException {
//            String fullPath = assetFilePath(getApplicationContext(), audioPath);
//            assert fullPath != null;
//            File audioFile = new File(fullPath); // Create a File object from the path
//            InputStream inStream = new FileInputStream(audioFile);
//            int bufferSize = 128;
//            AudioDispatcher dispatcher = new AudioDispatcher(new UniversalAudioInputStream(inStream, new TarsosDSPAudioFormat(sampleRate, bufferSize, 1, true, true)), bufferSize, 1);
//            int samplesPerFrame = 1024; // Set your desired samples per frame
//            float lowerFilterFreq = 300; // Set your desired lower filter frequency
//            float upperFilterFreq = 3700; // Set your desired upper filter frequency
//
//            MFCC mfcc = new MFCC(samplesPerFrame, sampleRate, nMfcc, nMfcc, lowerFilterFreq, upperFilterFreq);
//
//            final List<float[]> mfccList = new ArrayList<>();
//
//            dispatcher.addAudioProcessor(mfcc);
//
//            dispatcher.addAudioProcessor(new AudioProcessor() {
//                @Override
//                public void processingFinished() {
//                    // Do nothing here
//                }
//
//                @Override
//                public boolean process(AudioEvent audioEvent) {
//                    // Convert MFCC coefficients to an array and add them to the list
//                    float[] mfccCoefficients = mfcc.getMFCC();
//                    mfccList.add(mfccCoefficients);
//                    return true;
//                }
//            });
//
//            dispatcher.run();
//
//            // Compute the mean of MFCC coefficients
//            double[] mfccMean = new double[nMfcc];
//            for (int i = 0; i < nMfcc; i++) {
//                double sum = 0;
//                for (float[] frame : mfccList) {
//                    sum += frame[i];
//                }
//                mfccMean[i] = sum / mfccList.size();
//            }
//
//            return mfccMean;
//        }
//
//    }

}

