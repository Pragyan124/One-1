package com.example.calculator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private TextView output;
    private ArrayList<String> outputList;
    private String currentInput = "";
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        output = findViewById(R.id.output);
        outputList = new ArrayList<>();

        // Request necessary permissions
        requestPermissions();

        // Initialize SpeechRecognizer and TextToSpeech
        initializeSpeechRecognizer();
        initializeTextToSpeech();

        // Start listening for the trigger phrase
        startListeningForTriggerPhrase();

        // Numbers
        setupNumberButton(R.id.one, "1");
        setupNumberButton(R.id.two, "2");
        setupNumberButton(R.id.three, "3");
        setupNumberButton(R.id.four, "4");
        setupNumberButton(R.id.five, "5");
        setupNumberButton(R.id.six, "6");
        setupNumberButton(R.id.seven, "7");
        setupNumberButton(R.id.eight, "8");
        setupNumberButton(R.id.nine, "9");
        setupNumberButton(R.id.zero2, "00");

        // Setting up operator buttons
        setupOperatorButton(R.id.add, "+");
        setupOperatorButton(R.id.sub, "-");
        setupOperatorButton(R.id.multiply, "x");
        setupOperatorButton(R.id.divide, "/");
        setupOperatorButton(R.id.modulo, "%");

        setupSpecialButton(R.id.decimal, ".");
        setupDeleteButton(R.id.del);
        setupClearButton(R.id.ac);
        setupEqualsButton(R.id.equal);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION_CODE);
        }
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);

                    // Adjust speech rate here (example: slower rate)
                    textToSpeech.setSpeechRate(0.5f); // Adjust rate as needed
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
    }

    private void setupNumberButton(int buttonId, final String value) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput += value; // Append the current number
                output.setText(currentInput); // Update the EditText
                outputList.add(value); // Add the value to the list
            }
        });
    }

    private void setupOperatorButton(int buttonId, final String value) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput += " " + value + " "; // Append the operator with spaces for readability
                output.setText(currentInput); // Update the EditText
                outputList.add(value); // Add the value to the list
            }
        });
    }

    private void setupSpecialButton(int buttonId, final String value) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput += value; // Append the special character
                output.setText(currentInput); // Update the EditText
                outputList.add(value); // Add the value to the list
            }
        });
    }

    private void setupDeleteButton(int buttonId) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    currentInput = currentInput.substring(0, currentInput.length() - 1); // Remove last character
                    output.setText(currentInput); // Update the EditText
                    if (!outputList.isEmpty()) {
                        outputList.remove(outputList.size() - 1); // Remove last item from list
                    }
                }
            }
        });
    }

    private void setupClearButton(int buttonId) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput = ""; // Clear the current input
                output.setText(currentInput); // Update the EditText
                outputList.clear(); // Clear the list
            }
        });
    }

    private void setupEqualsButton(int buttonId) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double result = evaluateExpression(currentInput); // Evaluate the expression
                    currentInput = String.valueOf(result); // Update current input to result
                    output.setText(currentInput); // Update the EditText
                    outputList.clear(); // Clear the list
                    outputList.add(currentInput); // Add the result to the list

                    // Convert result to speech
                    textToSpeech.speak("The result is " + currentInput, TextToSpeech.QUEUE_FLUSH, null, null);
                } catch (Exception e) {
                    output.setText("Error"); // Display error

                    // Convert error message to speech
                    textToSpeech.speak("There was an error evaluating the expression", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }

    private double evaluateExpression(String expression) {
        // This is a simple implementation, you may need a more robust solution for complex expressions
        // Assuming the input is well-formed (spaces between operators and numbers)
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();
        String[] tokens = expression.split(" ");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (isNumber(token)) {
                numbers.push(Double.parseDouble(token));
            } else if (token.length() == 1 && isOperator(token.charAt(0))) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(token.charAt(0))) {
                    double b = numbers.pop();
                    double a = numbers.pop();
                    char op = operators.pop();
                    numbers.push(applyOperation(a, b, op));
                }
                operators.push(token.charAt(0));
            }
        }
        while (!operators.isEmpty()) {
            double b = numbers.pop();
            double a = numbers.pop();
            char op = operators.pop();
            numbers.push(applyOperation(a, b, op));
        }
        return numbers.pop();
    }

    private boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == 'x' || c == '/';
    }

    private int precedence(char op) {
        switch (op) {
            case '+':
            case '-':
                return 1;
            case 'x':
            case '/':
                return 2;
            default:
                return -1;
        }
    }

    private double applyOperation(double a, double b, char op) {
        switch (op) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case 'x':
                return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            default:
                return 0;
        }
    }



    private void startListeningForTriggerPhrase() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());

        RecognitionListener listener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // Restart listening after an error
                speechRecognizer.startListening(intent);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String result : matches) {
                        if (result.equalsIgnoreCase("Radhika")) {
                            // Respond with voice output
                            textToSpeech.speak("Yes", TextToSpeech.QUEUE_FLUSH, null, null);
                            // Start voice recognition for input
                            startListeningForVoiceInput();
                            break;
                        }
                    }
                }
                // Restart listening for trigger phrase
                speechRecognizer.startListening(intent);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialMatches != null) {
                    for (String result : partialMatches) {
                        if (result.equalsIgnoreCase("Hey Radhika")) {
                            // Respond with voice output
                            textToSpeech.speak("How can I help you, sir?", TextToSpeech.QUEUE_FLUSH, null, null);
                            // Start voice recognition for input
                            startListeningForVoiceInput();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        };

        speechRecognizer.setRecognitionListener(listener);
        speechRecognizer.startListening(intent);
    }

    private void startListeningForVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking");
        startActivityForResult(intent, 100);
    }


    private String mapSpokenWordsToSymbols(String spokenText) {
        spokenText = spokenText.toLowerCase();

        // Exception
        spokenText = spokenText.replaceAll("\\boneplus\\b", "one plus ");

        // Numbers
        spokenText = spokenText.replaceAll("\\bone\\b", "1");
        spokenText = spokenText.replaceAll("\\btwo\\b", "2");
        spokenText = spokenText.replaceAll("\\bthree\\b", "3");
        spokenText = spokenText.replaceAll("\\bfour\\b", "4");
        spokenText = spokenText.replaceAll("\\bfive\\b", "5");
        spokenText = spokenText.replaceAll("\\bsix\\b", "6");
        spokenText = spokenText.replaceAll("\\bseven\\b", "7");
        spokenText = spokenText.replaceAll("\\beight\\b", "8");
        spokenText = spokenText.replaceAll("\\bnine\\b", "9");
        spokenText = spokenText.replaceAll("\\bzero\\b", "0");

        // Operators
        spokenText = spokenText.replaceAll("\\bplus\\b", "+");
        spokenText = spokenText.replaceAll("\\bminus\\b", "-");
        spokenText = spokenText.replaceAll("\\btimes\\b", "x");
        spokenText = spokenText.replaceAll("\\bmultiplied by\\b", "x");
        spokenText = spokenText.replaceAll("\\bdivided by\\b", "/");
        spokenText = spokenText.replaceAll("\\bdivide by\\b", "/");
        spokenText = spokenText.replaceAll("\\binto\\b", "x");
        spokenText = spokenText.replaceAll("\\bover\\b", "/");

        // Handle other variations as needed
        return spokenText;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            spokenText = mapSpokenWordsToSymbols(spokenText); // Map spoken words to symbols
            try {
                double result = evaluateExpression(spokenText);
                String resultText = String.valueOf(result);
                output.setText(resultText);

                // Convert result to speech
                textToSpeech.speak("Aap ka result aya hain" + resultText , TextToSpeech.QUEUE_FLUSH, null, null);
            } catch (Exception e) {
                output.setText("Error");

                // Convert error message to speech
                textToSpeech.speak("There was an error evaluating the expression", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}
