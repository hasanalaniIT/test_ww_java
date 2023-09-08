import 'package:tflite_flutter/tflite_flutter.dart';

class TflitePredictor{
  Future<double> predict(List<dynamic> audioChunk) async {
    var output = [];
    final interpreter = await Interpreter.fromAsset('assets/converted_model.tflite');
    final outputShape = interpreter.getOutputTensor(0).shape;
    output = outputShape.reshape([1,2]);
    interpreter.run(audioChunk, output);
    print("Output Prediction and Accuracy :: $output");
    return output[0][1];
  }
}

