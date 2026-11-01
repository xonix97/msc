Original deterministic 3-second stereo PCM sine fixtures generated mathematically for msc tests. CC0-1.0 dedication. Not commercial music. tone-a: 440Hz; tone-b: 660Hz; 44100Hz, signed 16-bit PCM. No output continuity or acoustic test has yet run.

tone.flac and tone.mp3 are FFmpeg encodings of the original WAV test tones, with original test metadata. The same CC0 dedication applies. No commercial media is included.

tone-a.m4a repeats tone-a.wav into a 15-second AAC/MP4 fixture for real HTTP streaming, seeking and background MediaSession tests. Generated with: ffmpeg -stream_loop 4 -i tone-a.wav -c:a aac -b:a 128k -movflags +faststart -metadata title='Original msc HTTP fixture' tone-a.m4a. It shares the same CC0 dedication and is only in the test APK. The longer fixture and explicit repeat mode keep end-of-track timing separate from slow emulator Activity transitions.
