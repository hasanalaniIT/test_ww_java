import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

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
  String? audio;
  final StreamController<bool> listeningStateController =
      StreamController<bool>.broadcast();

  Future<void> start() async {
    if (await _audioRecorder.hasPermission()) {
      print("STARTED RECORDING");
      _amplitudeSub = _audioRecorder
          .onAmplitudeChanged(const Duration(milliseconds: 300))
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

      _audioRecorder.start(encoder: AudioEncoder.wav);
      listeningStateController.add(true);
      startCountDown();
    }
  }

  void startCountDown() {
    countDownTimer = Timer.periodic(const Duration(milliseconds: 500), (_) {
      _recordingDuration += 0.5;
      if (_recordingDuration > 14) {
        print("RECORDING LIMIT EXCEEDED");
        stop();
        return;
      }

      if (_recordingDuration > 2 && _silenceDetectedTimes > 2) {
        print("SILENCE LIMIT EXCEEDED");
        stop();
      }
    });
  }

  Future<String?> stop() async {
    if (await _audioRecorder.isRecording()) {
      print("STOPPED RECORDING");
      listeningStateController.add(false);
      countDownTimer?.cancel();
      audio = await _audioRecorder.stop();
      _audioRecorder.dispose();

      _recordingDuration = 0;
      _silenceDetectedTimes = 0;
      print("Audio path :: $audio");
      return audio;
    }
    return null;
  }

  Future<bool> isRecording() async {
    return await _audioRecorder.isRecording();
  }

  String? getAudioPath() {
    return audio;
  }

  Future<Uint8List> getAudioData(String path) async {
    final wav = await Wav.readFile(path);
    var byteData = wav.write().buffer.asUint8List();
    print("Byte data :: $byteData");
    return byteData;
  }
}
