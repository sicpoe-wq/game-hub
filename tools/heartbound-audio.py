#!/usr/bin/env python3
"""Original Heartbound music and SFX. Written for this game; released under CC0 1.0."""
import math, os, struct, wave
import numpy as np

SR = 44100
OUT = "/tmp/hb-audio"

def midi(n):
    return 440.0 * (2 ** ((n - 69) / 12.0))

def env_adsr(n, a=0.01, d=0.08, s=0.7, r=0.15, peak=1.0):
    e = np.zeros(n, dtype=np.float64)
    na, nd, nr = int(a*SR), int(d*SR), int(r*SR)
    ns = max(0, n - na - nd - nr)
    i = 0
    if na:
        e[i:i+na] = np.linspace(0, peak, na, endpoint=False); i += na
    if nd:
        e[i:i+nd] = np.linspace(peak, peak*s, nd, endpoint=False); i += nd
    if ns:
        e[i:i+ns] = peak * s; i += ns
    if nr and i < n:
        e[i:i+nr] = np.linspace(e[i-1] if i else peak*s, 0, min(nr, n-i), endpoint=True)
    return e

def sine(freq, n, phase=0.0):
    t = np.arange(n) / SR
    return np.sin(2*np.pi*freq*t + phase)

def tri(freq, n):
    t = np.arange(n) / SR
    return 2 * np.abs(2*((t*freq)%1) - 1) - 1

def square_soft(freq, n, width=0.5):
    t = np.arange(n) / SR
    # band-limited-ish via tanh
    return np.tanh(6 * np.sin(2*np.pi*freq*t))

def noise(n):
    return np.random.default_rng(7).standard_normal(n)

def karplus(freq, n, decay=0.996, brightness=0.5):
    per = max(2, int(SR / freq))
    rng = np.random.default_rng(int(freq*10) & 0xFFFF)
    buf = rng.standard_normal(per)
    buf = np.convolve(buf, [brightness, 1-brightness], mode="same")
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % per]
        nxt = 0.5 * (buf[i % per] + buf[(i+1) % per]) * decay
        buf[i % per] = nxt
    # soft high cut
    if n > 4:
        k = np.array([0.25, 0.5, 0.25])
        out = np.convolve(out, k, mode="same")
    return out

def lowpass(x, cutoff=800, q=0.7):
    # one-pole
    rc = 1 / (2 * math.pi * cutoff)
    a = 1 / (rc * SR + 1)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += a * (v - acc)
        y[i] = acc
    return y

def highpass(x, cutoff=200):
    rc = 1 / (2 * math.pi * cutoff)
    a = rc / (rc + 1/SR)
    y = np.zeros_like(x)
    prevx = prevy = 0.0
    for i, v in enumerate(x):
        y[i] = a * (prevy + v - prevx)
        prevx, prevy = v, y[i]
    return y

def pad_tone(freq, n, rich=0.4):
    s = 0.5*sine(freq, n) + 0.28*sine(freq*1.002, n, 0.3) + 0.18*sine(freq*0.5, n)
    s += rich * 0.12 * sine(freq*2, n, 0.7)
    s += rich * 0.06 * sine(freq*3, n)
    s *= env_adsr(n, 0.4, 0.3, 0.85, 0.6)
    return s

def flute(freq, n):
    s = 0.7*sine(freq, n) + 0.18*sine(freq*2, n) + 0.08*tri(freq, n)
    breath = 0.04 * lowpass(np.random.default_rng(int(freq)).standard_normal(n), 1800)
    return (s + breath) * env_adsr(n, 0.06, 0.1, 0.75, 0.2)

def harp(freq, n):
    s = karplus(freq, n, decay=0.988, brightness=0.35)
    s += 0.25 * sine(freq, n) * env_adsr(n, 0.002, 0.15, 0.35, 0.4)
    return s * env_adsr(n, 0.002, 0.08, 0.5, 0.5)

def bass(freq, n):
    s = 0.7*sine(freq, n) + 0.25*sine(freq*2, n) + 0.1*tri(freq, n)
    return lowpass(s, 420) * env_adsr(n, 0.01, 0.12, 0.6, 0.2)

def brass(freq, n):
    s = 0.45*square_soft(freq, n) + 0.3*sine(freq, n) + 0.15*sine(freq*2, n) + 0.1*sine(freq*3, n)
    return lowpass(s, 1400) * env_adsr(n, 0.03, 0.08, 0.7, 0.15)

def kick(n):
    t = np.arange(n)/SR
    f = 110 * np.exp(-t*18) + 35
    ph = np.cumsum(2*np.pi*f/SR)
    s = np.sin(ph) * np.exp(-t*8)
    return s

def snare(n):
    t = np.arange(n)/SR
    s = highpass(np.random.default_rng(3).standard_normal(n), 800) * np.exp(-t*14)
    s += 0.3*sine(180, n)*np.exp(-t*20)
    return s * 0.7

def hat(n, open_=False):
    t = np.arange(n)/SR
    decay = 18 if open_ else 40
    return highpass(np.random.default_rng(11).standard_normal(n), 6000) * np.exp(-t*decay) * 0.35

def place(track, start, clip, gain=1.0):
    end = min(len(track), start + len(clip))
    if start < 0 or start >= len(track) or end <= start:
        return
    track[start:end] += clip[:end-start] * gain

def render_loop(seconds, bpm, pattern_fn, fade=0.08):
    n = int(seconds * SR)
    mix = np.zeros(n)
    pattern_fn(mix, bpm)
    # loop-friendly fade
    f = int(fade * SR)
    if f > 0:
        mix[:f] *= np.linspace(0, 1, f)
        mix[-f:] *= np.linspace(1, 0, f)
    # soft normalize
    peak = np.max(np.abs(mix)) or 1.0
    mix = mix / peak * 0.86
    return mix

def write_wav(path, mix):
    pcm = np.clip(mix, -1, 1)
    pcm = (pcm * 32767).astype(np.int16)
    with wave.open(path, "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())

# Musical material
# scales as MIDI numbers
def beat_samples(bpm, beats):
    return int(SR * 60 / bpm * beats)

# ---------- HALL: warm, mid, hopeful (C major) ----------
def hall(mix, bpm):
    # C major: C D E F G A B
    # melody in 4-bar phrases
    # I - vi - IV - V
    chords = [
        [48, 52, 55, 60],  # C
        [45, 48, 52, 57],  # Am
        [41, 45, 48, 53],  # F
        [43, 47, 50, 55],  # G
    ]
    bar = beat_samples(bpm, 4)
    melody = [
        72, 74, 76, 74, 72, 69, 71, 72,
        69, 71, 72, 74, 76, 74, 72, 69,
        65, 67, 69, 72, 71, 69, 67, 65,
        67, 69, 71, 74, 72, 71, 69, 67,
    ]
    # 16th? actually 8th notes, 2 per beat, 8 per bar, 4 bars
    eighth = beat_samples(bpm, 0.5)
    t = 0
    phrase = 0
    while t < len(mix) - bar:
        ch = chords[phrase % 4]
        # pad whole bar
        for m in ch:
            place(mix, t, pad_tone(midi(m), bar, rich=0.25), 0.18)
        # bass on 1 and 3
        place(mix, t, bass(midi(ch[0]), beat_samples(bpm, 1.6)), 0.7)
        place(mix, t + beat_samples(bpm, 2), bass(midi(ch[0]), beat_samples(bpm, 1.6)), 0.55)
        # harp arpeggio quarters
        arp = [ch[0]+12, ch[1]+12, ch[2]+12, ch[1]+12]
        for i, m in enumerate(arp):
            place(mix, t + beat_samples(bpm, i), harp(midi(m), beat_samples(bpm, 1.2)), 0.45)
        # soft kick 1 and 3
        place(mix, t, kick(int(0.25*SR)), 0.25)
        place(mix, t + beat_samples(bpm, 2), kick(int(0.22*SR)), 0.18)
        # hats
        for i in range(8):
            place(mix, t + i*eighth, hat(int(0.06*SR)), 0.22 if i%2==0 else 0.12)
        phrase += 1
        t += bar
    # melody over the top, 8ths
    t = 0
    i = 0
    while t < len(mix) - eighth:
        note = melody[i % len(melody)]
        if note:
            place(mix, t, flute(midi(note), int(eighth*1.6)), 0.42)
        t += eighth
        i += 1

# ---------- EVENING / TAVERN: slightly swing, D dorian warmth ----------
def evening(mix, bpm):
    chords = [
        [50, 53, 57, 60],  # Dm7
        [43, 47, 50, 54],  # G
        [45, 48, 52, 55],  # Am
        [48, 52, 55, 59],  # Cmaj7
    ]
    bar = beat_samples(bpm, 4)
    swing = beat_samples(bpm, 0.66)
    off = beat_samples(bpm, 0.34)
    melody = [69, 72, 74, 72, 69, 67, 65, 67, 69, 72, 74, 77, 74, 72, 69, 67]
    t = 0
    p = 0
    while t < len(mix) - bar:
        ch = chords[p % 4]
        for m in ch:
            place(mix, t, pad_tone(midi(m), bar, rich=0.35), 0.16)
        place(mix, t, bass(midi(ch[0]-12 if ch[0]>=48 else ch[0]), beat_samples(bpm, 1.8)), 0.8)
        place(mix, t + beat_samples(bpm, 2), bass(midi(ch[0]), beat_samples(bpm, 1.5)), 0.6)
        # plucked "lute" swing
        arp = [ch[1]+12, ch[2]+12, ch[3]+12, ch[2]+12]
        tt = t
        for i, m in enumerate(arp*2):
            dur = swing if i%2==0 else off
            place(mix, tt, harp(midi(m), int(dur*1.8)), 0.38)
            tt += dur
        place(mix, t, kick(int(0.22*SR)), 0.3)
        place(mix, t + beat_samples(bpm, 1.5), snare(int(0.18*SR)), 0.18)
        place(mix, t + beat_samples(bpm, 2.5), kick(int(0.18*SR)), 0.16)
        for i in range(8):
            place(mix, t + beat_samples(bpm, i*0.5), hat(int(0.05*SR), open_=(i==3)), 0.2)
        p += 1
        t += bar
    t = 0
    i = 0
    while t < len(mix) - beat_samples(bpm, 1):
        note = melody[i % len(melody)]
        place(mix, t, flute(midi(note), beat_samples(bpm, 1.15)), 0.4)
        t += beat_samples(bpm, 1)
        i += 1

# ---------- BATTLE: D minor, driving ----------
def battle(mix, bpm):
    chords = [
        [38, 41, 45],  # D
        [38, 41, 45],
        [36, 39, 43],  # C
        [41, 45, 48],  # F
        [38, 41, 45],
        [38, 41, 45],
        [43, 47, 50],  # G
        [36, 40, 43],  # C
    ]
    bar = beat_samples(bpm, 2)  # 2-beat cells
    ost = [50, 53, 57, 53, 50, 48, 46, 48]
    t = 0
    p = 0
    eighth = beat_samples(bpm, 0.5)
    while t < len(mix) - bar:
        ch = chords[p % len(chords)]
        for m in ch:
            place(mix, t, brass(midi(m+12), bar), 0.22)
        place(mix, t, bass(midi(ch[0]), bar), 0.9)
        # drums
        place(mix, t, kick(int(0.2*SR)), 0.8)
        place(mix, t + eighth, hat(int(0.04*SR)), 0.3)
        place(mix, t + 2*eighth, snare(int(0.16*SR)), 0.55)
        place(mix, t + 3*eighth, hat(int(0.04*SR)), 0.25)
        if p % 4 == 3:
            place(mix, t + eighth, kick(int(0.15*SR)), 0.4)
        p += 1
        t += bar
    t = 0
    i = 0
    while t < len(mix) - eighth:
        note = ost[i % len(ost)]
        place(mix, t, square_soft(midi(note), eighth) * env_adsr(eighth, 0.005, 0.04, 0.5, 0.08), 0.28)
        t += eighth
        i += 1
    # lead every 4 bars-ish
    lead = [69, 72, 74, 77, 74, 72, 69, 65, 67, 69, 72, 74, 72, 69, 65, 62]
    t = 0
    i = 0
    q = beat_samples(bpm, 1)
    while t < len(mix) - q:
        place(mix, t, brass(midi(lead[i % len(lead)]), int(q*0.95)), 0.38)
        t += q
        i += 1

# ---------- ROMANCE: slow harp + soft pad, A major ----------
def romance(mix, bpm):
    chords = [
        [45, 49, 52, 56],  # A
        [50, 54, 57, 61],  # D
        [47, 51, 54, 57],  # B minor? actually Bm 47 50 54, use E 47 51 54 59
        [47, 51, 54, 59],  # E
        [42, 45, 49, 52],  # F#m
        [45, 49, 52, 56],  # A
        [50, 54, 57, 61],  # D
        [47, 51, 54, 59],  # E
    ]
    bar = beat_samples(bpm, 4)
    melody = [73, 76, 81, 80, 76, 73, 71, 73, 69, 71, 73, 76, 73, 71, 69, 68]
    t = 0
    p = 0
    while t < len(mix) - bar:
        ch = chords[p % len(chords)]
        for m in ch:
            place(mix, t, pad_tone(midi(m), bar, rich=0.2), 0.22)
        place(mix, t, bass(midi(ch[0]), bar), 0.4)
        arp = [ch[0]+12, ch[1]+12, ch[2]+12, ch[3]+12, ch[2]+12, ch[1]+12]
        step = bar // 6
        for i, m in enumerate(arp):
            place(mix, t + i*step, harp(midi(m), int(step*2.2)), 0.5)
        p += 1
        t += bar
    t = 0
    i = 0
    half = beat_samples(bpm, 2)
    while t < len(mix) - half:
        place(mix, t, flute(midi(melody[i % len(melody)]), int(half*1.15)), 0.38)
        t += half
        i += 1

# ---------- ENDING: fuller hopeful, G major anthem ----------
def ending(mix, bpm):
    chords = [
        [43, 47, 50, 55],  # G
        [36, 40, 43, 47],  # C
        [40, 43, 47, 50],  # Em
        [38, 42, 45, 50],  # D
        [43, 47, 50, 55],
        [45, 48, 52, 55],  # Am
        [36, 40, 43, 47],
        [38, 42, 45, 50],
    ]
    bar = beat_samples(bpm, 4)
    melody = [67, 69, 71, 74, 72, 71, 69, 67, 64, 66, 67, 71, 69, 67, 66, 62]
    t = 0
    p = 0
    while t < len(mix) - bar:
        ch = chords[p % len(chords)]
        for m in ch:
            place(mix, t, pad_tone(midi(m), bar, rich=0.45), 0.2)
            place(mix, t, brass(midi(m+12), bar), 0.12)
        place(mix, t, bass(midi(ch[0]), beat_samples(bpm, 2)), 0.7)
        place(mix, t + beat_samples(bpm, 2), bass(midi(ch[0]+7 if p%2 else ch[0]), beat_samples(bpm, 2)), 0.5)
        place(mix, t, kick(int(0.25*SR)), 0.35)
        place(mix, t + beat_samples(bpm, 2), kick(int(0.2*SR)), 0.25)
        place(mix, t + beat_samples(bpm, 2), snare(int(0.16*SR)), 0.2)
        for i, m in enumerate([ch[1]+12, ch[2]+12, ch[3]+12, ch[2]+12]):
            place(mix, t + beat_samples(bpm, i), harp(midi(m), beat_samples(bpm, 1.4)), 0.3)
        p += 1
        t += bar
    t = 0
    i = 0
    q = beat_samples(bpm, 1)
    while t < len(mix) - q:
        place(mix, t, flute(midi(melody[i % len(melody)]), int(q*1.3)), 0.45)
        t += q
        i += 1

# ---------- TITLE: slow hopeful swell (G major) ----------
def title(mix, bpm):
    chords = [
        [43, 47, 50, 55],  # G
        [40, 43, 47, 50],  # Em
        [36, 40, 43, 47],  # C
        [38, 42, 45, 50],  # D
        [43, 47, 50, 55],
        [45, 48, 52, 55],  # Am
        [40, 43, 47, 50],
        [38, 42, 45, 50],
    ]
    bar = beat_samples(bpm, 4)
    melody = [67, 71, 74, 79, 76, 74, 71, 67, 64, 66, 67, 71, 69, 67, 66, 62]
    t = 0
    p = 0
    while t < len(mix) - bar:
        ch = chords[p % len(chords)]
        swell = 0.12 + 0.14 * min(1.0, t / (SR * 8.0))  # bloom toward the logo
        for m in ch:
            place(mix, t, pad_tone(midi(m), bar, rich=0.3 + 0.2 * swell), 0.16 + swell)
        place(mix, t, bass(midi(ch[0]), bar), 0.32 + 0.2 * swell)
        arp = [ch[0]+12, ch[1]+12, ch[2]+12, ch[3]+12, ch[2]+12, ch[1]+12]
        step = bar // 6
        for i, m in enumerate(arp):
            place(mix, t + i*step, harp(midi(m), int(step*2.4)), 0.42)
        if p % 2 == 0:
            place(mix, t, kick(int(0.22*SR)), 0.12)
        p += 1
        t += bar
    t = beat_samples(bpm, 4)  # melody enters after a bar
    i = 0
    half = beat_samples(bpm, 2)
    while t < len(mix) - half:
        place(mix, t, flute(midi(melody[i % len(melody)]), int(half*1.2)), 0.40)
        t += half
        i += 1

# ---------- SFX ----------
def sfx_tap():
    n = int(0.06*SR)
    t = np.arange(n)/SR
    s = 0.5*sine(880, n) * np.exp(-t*60) + 0.3*sine(1320, n)*np.exp(-t*80)
    s += 0.15*highpass(np.random.default_rng(2).standard_normal(n), 4000)*np.exp(-t*70)
    return s * 0.7

def sfx_hit():
    n = int(0.28*SR)
    t = np.arange(n)/SR
    s = lowpass(np.random.default_rng(5).standard_normal(n), 2000) * np.exp(-t*18)
    s += 0.6*sine(90, n)*np.exp(-t*14)
    s += 0.3*sine(180, n)*np.exp(-t*20)
    # body thud + click
    s += 0.25 * np.sin(2*np.pi*(220*np.exp(-t*30))*t) * np.exp(-t*16)
    return np.clip(s*0.9, -1, 1)

def sfx_heal():
    n = int(0.45*SR)
    t = np.arange(n)/SR
    s = np.zeros(n)
    for i, f in enumerate([523, 659, 784, 1046]):
        start = int(i*0.06*SR)
        nn = n-start
        if nn>0:
            s[start:] += 0.35 * sine(f, nn) * np.exp(-np.arange(nn)/SR*6)
    s += 0.08 * sine(1568, n) * np.exp(-t*8)
    return np.clip(s, -1, 1)

def sfx_level():
    n = int(0.85*SR)
    t = np.arange(n)/SR
    notes = [392, 523, 659, 784, 1046]
    s = np.zeros(n)
    for i, f in enumerate(notes):
        start = int(i*0.11*SR)
        nn = n-start
        if nn>0:
            s[start:] += 0.4 * sine(f, nn) * np.exp(-np.arange(nn)/SR*4)
            s[start:] += 0.12 * sine(f*2, nn) * np.exp(-np.arange(nn)/SR*6)
    # shimmer
    s += 0.08 * sine(2093, n) * np.exp(-t*5)
    return np.clip(s, -1, 1)

def sfx_gift():
    n = int(0.5*SR)
    t = np.arange(n)/SR
    s = np.zeros(n)
    for i, f in enumerate([659, 784, 988]):
        start = int(i*0.07*SR)
        nn = n-start
        if nn>0:
            s[start:] += 0.38 * harp(f, nn)[:nn]
            s[start:] += 0.2 * sine(f, nn) * np.exp(-np.arange(nn)/SR*5)
    return np.clip(s*0.9, -1, 1)

def sfx_ach():
    n = int(1.05*SR)
    t = np.arange(n)/SR
    notes = [523, 659, 784, 1046, 1318]
    s = np.zeros(n)
    for i, f in enumerate(notes):
        start = int(i*0.12*SR)
        nn = n-start
        if nn>0:
            s[start:] += 0.35 * sine(f, nn) * env_adsr(nn, 0.01, 0.08, 0.6, 0.4)
            s[start:] += 0.12 * sine(f*2, nn) * env_adsr(nn, 0.01, 0.1, 0.4, 0.4)
    s += 0.1 * pad_tone(523, n, 0.4)
    return np.clip(s, -1, 1)

def encode_ogg(wav, ogg, br="64k"):
    os.system(f'ffmpeg -y -hide_banner -loglevel error -i "{wav}" -c:a libopus -b:a {br} -ac 1 -ar 48000 "{ogg}"')

def main():
    os.makedirs(OUT, exist_ok=True)
    tracks = {
        "mus_hall": (26, 84, hall),
        "mus_evening": (24, 92, evening),
        "mus_battle": (22, 132, battle),
        "mus_romance": (28, 68, romance),
        "mus_ending": (28, 88, ending),
        "mus_title": (28, 70, title),
    }
    for name, (sec, bpm, fn) in tracks.items():
        print("render", name)
        mix = render_loop(sec, bpm, fn, fade=0.12)
        wav = f"{OUT}/{name}.wav"
        write_wav(wav, mix)
        encode_ogg(wav, f"{OUT}/{name}.ogg", "56k")
    sfx = {
        "sfx_tap": sfx_tap(),
        "sfx_hit": sfx_hit(),
        "sfx_heal": sfx_heal(),
        "sfx_level": sfx_level(),
        "sfx_gift": sfx_gift(),
        "sfx_ach": sfx_ach(),
    }
    for name, mix in sfx.items():
        f = int(0.008*SR)
        if len(mix) > 2*f:
            mix = mix.copy()
            mix[:f] *= np.linspace(0,1,f)
            mix[-f:] *= np.linspace(1,0,f)
        peak = np.max(np.abs(mix)) or 1
        mix = mix/peak*0.88
        wav = f"{OUT}/{name}.wav"
        write_wav(wav, mix)
        encode_ogg(wav, f"{OUT}/{name}.ogg", "32k")
        print("sfx", name, os.path.getsize(f"{OUT}/{name}.ogg"))
    print("done")

if __name__ == "__main__":
    main()
