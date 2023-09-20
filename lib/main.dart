import 'dart:async';
import 'package:flutter/material.dart';
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
  String platformChannelResult = 'Unknown  level.';
  Future<void> getResult() async {
    var result = await TflitePredictor().wakeWordResult();
    setState(() {
      platformChannelResult = result;
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
                await getResult();
              },
              child: const Text(
                'Start Recording',
                style: TextStyle(fontSize: 24),
              ),
            ),
            ElevatedButton(
              onPressed: () async {
                await Mic().stop();
              },
              child: const Text(
                'Stop Recording',
                style: TextStyle(fontSize: 24),
              ),
            ),
            Text(
              platformChannelResult,
              style: const TextStyle(fontSize: 24),
            ),
          ],
        ),
      ),
    );
  }
}
