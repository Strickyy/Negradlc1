"""Generate original gentle UI tones; requires ffmpeg with libvorbis."""
from pathlib import Path
import math
import struct
import subprocess
import tempfile
import wave

DEST = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/negradlc/sounds'
DEST.mkdir(parents=True, exist_ok=True)
RATE = 44100
for name, frequencies, duration in [
    ('toggle_on', (523.25, 659.25, 783.99), .20),
    ('toggle_off', (659.25, 523.25, 392.00), .22),
    ('click', (880.00,), .065),
]:
    data = bytearray()
    for i in range(int(RATE * duration)):
        t = i / RATE
        value = 0
        for j, frequency in enumerate(frequencies):
            local = t - j * .032
            if local < 0:
                continue
            fade_in = min(1, local / .008)
            fade_out = max(0, min(1, (duration - t) / .035))
            envelope = fade_in * fade_out * math.exp(-local * 14)
            value += math.sin(2 * math.pi * frequency * local) * envelope / len(frequencies)
        data += struct.pack('<h', round(max(-1, min(1, value * .48)) * 32767))
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / 'tone.wav'
        with wave.open(str(wav), 'wb') as stream:
            stream.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
            stream.writeframes(data)
        subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(wav),
                        '-c:a', 'libvorbis', '-q:a', '5', str(DEST / (name + '.ogg'))], check=True)
    print(name)
