import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:test_ww_java/predictor.dart';

import 'mic.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('com.example.test_ww_java/asr');

  String platformChannelResult = 'Unknown  level.';

  Future<void> _getResult() async {
    String channelResult;
    try {
      // example audios : "negative_reference.wav" , "positive_ww.wav"
      // Make sure to add the audio files inside /data/data/com.example.test_ww_java/files/
      var result = await platform.invokeMethod(
          'process_audio', {"audio_path": "test_mic.m4a"});
      result = await TflitePredictor().predict(result);
      channelResult = 'Wake-Word Accuracy level :: ${result.toString()}';
    } on PlatformException catch (e) {
      channelResult = "Failed to get Accuracy level: '${e.message}'.";
    }

    setState(() {
      platformChannelResult = channelResult;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            ElevatedButton(
              onPressed: () async {
                await Mic().start();
              },
              child: const Text('Get Result'),
            ),
            Text(platformChannelResult),
          ],
        ),
      ),
    );
  }
}
