import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:wav/wav_file.dart';

class Mic {
  static final Mic _singleton = Mic._internal();

  factory Mic() {
    return _singleton;
  }
  Mic._internal();

  final _audioRecorder = Record();
  StreamSubscription<Amplitude>? _amplitudeSub;
  Amplitude? _amplitude;
  double _recordingDuration = 0.0;
  int _silenceDetectedTimes = 0;
  Timer? countDownTimer;
  String? audioPath;
  final StreamController<bool> listeningStateController =
      StreamController<bool>.broadcast();

  Future<String?> start() async {
    if (await _audioRecorder.hasPermission()) {
      print("STARTED RECORDING");
      _amplitudeSub = _audioRecorder
          .onAmplitudeChanged(const Duration(milliseconds: 100))
          .listen((amp) {
        if (amp.current <= -22.0) {
          print("SILENCE DETECTED");
          _silenceDetectedTimes += 1;
        } else if (amp.current >= -20.0) {
          _silenceDetectedTimes != 0 ? _silenceDetectedTimes = 0 : null;
        }
        _amplitude = amp;
        print("Amp change :: ${amp.current}");
      });
      String? tempAudioSamplePath = await getTempAudioSamplePath();
      _audioRecorder.start(
        path: tempAudioSamplePath,
        encoder: AudioEncoder.wav,
        samplingRate: 16000,
      );
      listeningStateController.add(true);
      return await startCountDown();
    }
    return null;
  }

  Future<String?> startCountDown() async {
    countDownTimer =
        Timer.periodic(const Duration(milliseconds: 500), (_) async {
      _recordingDuration += 0.5;
      if (_recordingDuration > 3) {
        print("RECORDING LIMIT EXCEEDED");
        audioPath = await stop();
        return;
      }

      if (_recordingDuration > 2 && _silenceDetectedTimes > 2) {
        print("SILENCE LIMIT EXCEEDED");
        audioPath = await stop();
        return;
      }
    });
    return audioPath;
  }

  Future<String?> stop() async {
    if (await _audioRecorder.isRecording()) {
      print("STOPPED RECORDING");
      listeningStateController.add(false);
      countDownTimer?.cancel();
      var audio = await _audioRecorder.stop();
      _audioRecorder.dispose();

      _recordingDuration = 0;
      _silenceDetectedTimes = 0;
      print("Audio at stop :: $audio");
      return audio?.split("/")[6];
    }
    return null;
  }

  Future<bool> isRecording() async {
    return await _audioRecorder.isRecording();
  }

  Future<String?> getTempAudioSamplePath() async {
    try {
      final cacheDir = await getTemporaryDirectory();
      const filename = 'temp_mic_prediction_sample.wav';
      final tempAudioSamplePath = '${cacheDir.path}/$filename';
      return tempAudioSamplePath;
    } catch (e) {
      print('Error getting temporary audio sample path: $e');
    }
    return null;
  }

  Future<Uint8List> getAudioData(String path) async {
    final wav = await Wav.readFile(path);
    var byteData = wav.write().buffer.asUint8List();
    print("Byte data :: $byteData");
    return byteData;
  }
}
