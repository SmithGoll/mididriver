/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobileer.midikeyboard;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.mobileer.miditools.MusicKeyboardView;

import org.billthefarmer.mididriver.MidiDriver;
import org.billthefarmer.mididriver.MidiConstants;
import org.billthefarmer.mididriver.ReverbConstants;

import java.io.IOException;

/**
 * Main activity for the keyboard app.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MidiKeyboard";
    private static final int MAX_CHANNELS = 16;
    private static final int DEFAULT_VELOCITY = 64;

    private MidiDriver midi;
    private MusicKeyboardView mKeyboard;
    private Button mProgramButton;
    private int mChannel; // ranges from 0 to 15
    private int[] mPrograms = new int[MAX_CHANNELS]; // ranges from 0 to 127
    private byte[] mByteBuffer = new byte[3];

    public class ChannelSpinnerActivity implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            mChannel = pos & 0x0F;
            updateProgramText();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create midi driver
        midi = MidiDriver.getInstance();

        mKeyboard = (MusicKeyboardView) findViewById(R.id.musicKeyboardView);
        mKeyboard.addMusicKeyListener(new MusicKeyboardView.MusicKeyListener() {
            @Override
            public void onKeyDown(int keyIndex) {
                noteOn(mChannel, keyIndex, DEFAULT_VELOCITY);
            }

            @Override
            public void onKeyUp(int keyIndex) {
                noteOff(mChannel, keyIndex, DEFAULT_VELOCITY);
            }
        });

        mProgramButton = (Button) findViewById(R.id.button_program);

        Spinner spinner = (Spinner) findViewById(R.id.spinner_channels);
        spinner.setOnItemSelectedListener(new ChannelSpinnerActivity());
    }

    @Override
    public void onDestroy() {
        if (midi != null) {
            midi.stop();
        }

        super.onDestroy();
    }

    // On resume
    @Override
    public void onResume() {
        super.onResume();

        // Start midi
        if (midi != null) {
            midi.start();
            midi.setReverb(ReverbConstants.OFF);
        }
    }

    // On pause
    @Override
    public void onPause() {
        // Stop midi
        if (midi != null) {
            midi.stop();
        }

        super.onPause();
    }

    public void onProgramSend(View view) {
        sendMidi(MidiConstants.PROGRAM_CHANGE | mChannel, mPrograms[mChannel]);
    }

    public void onProgramDelta(View view) {
        Button button = (Button) view;
        int delta = Integer.parseInt(button.getText().toString());
        changeProgram(delta);
    }

    private void changeProgram(int delta) {
        int program = mPrograms[mChannel];
        program += delta;
        if (program < 0) {
            program = 0;
        } else if (program > 127) {
            program = 127;
        }
        sendMidi(MidiConstants.PROGRAM_CHANGE | mChannel, program);
        mPrograms[mChannel] = program;
        updateProgramText();
    }

    private void updateProgramText() {
        mProgramButton.setText("" + mPrograms[mChannel]);
    }

    private void noteOff(int channel, int pitch, int velocity) {
        sendMidi(MidiConstants.NOTE_OFF | channel, pitch, velocity);
    }

    private void noteOn(int channel, int pitch, int velocity) {
        sendMidi(MidiConstants.NOTE_ON | channel, pitch, velocity);
    }

    // Send a midi message, 2 bytes
    private void sendMidi(int m, int n)
    {
        byte msg[] = new byte[2];

        msg[0] = (byte) m;
        msg[1] = (byte) n;

        midi.write(msg);
    }

    // Send a midi message, 3 bytes
    private void sendMidi(int m, int n, int v)
    {
        byte msg[] = new byte[3];

        msg[0] = (byte) m;
        msg[1] = (byte) n;
        msg[2] = (byte) v;

        midi.write(msg);
    }
}
