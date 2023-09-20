import 'dart:io';

import 'package:flutter/services.dart';
import 'package:tflite_flutter/tflite_flutter.dart';

import 'mic.dart';

class TflitePredictor {
  static const platform = MethodChannel('com.example.test_ww_java/asr');

  Future<double> predict(List<dynamic> audioChunk) async {
    var output = [];
    final interpreter =
        await Interpreter.fromAsset('assets/converted_model.tflite');
    final outputShape = interpreter.getOutputTensor(0).shape;
    output = outputShape.reshape([1, 2]);
    interpreter.run(audioChunk, output);
    print("Output Prediction and Accuracy :: $output");
    if (output[0][1] <= 0.94) await wakeWordResult();

    return output[0][1];
  }

  Future<dynamic> processAudio(String audioPath) async {
    try {
      // example audios : "negative_reference.wav" , "positive_ww.wav"
      // Make sure to add the audio files inside /data/data/com.example.test_ww_java/files/
      var processedAudioResult = await platform
          .invokeMethod('process_audio', {"audio_path": audioPath});
      return processedAudioResult;
    } on PlatformException catch (e) {
      print("Failed to get Accuracy level: '${e.message}'.");
      return null;
    }
  }

  Future<String> wakeWordResult() async {
    String channelResult;
    try {
      // example audios : "negative_reference.wav" , "positive_ww.wav"
      // Make sure to add the audio files inside /data/data/com.example.test_ww_java/files/
      await Mic().start();
      print("Mic().start() Audio");
      sleep(const Duration(milliseconds: 420));
      print("Mic Stopped");
      var audioRecordPath = await Mic().stop();
      var result = await processAudio(audioRecordPath!);
      result = await predict(result);
      channelResult = 'Wake-Word Accuracy level :: ${result.toString()}';
      return channelResult;
    } on PlatformException catch (e) {
      channelResult = "Failed to get Accuracy level: '${e.message}'.";
      return channelResult;
    }
  }
}
