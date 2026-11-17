#!/usr/bin/env python3
"""v2.2 assets: backdrop WebPs, procedural board textures, recropped figures."""
from __future__ import annotations
import sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageFilter, ImageDraw
# reuse chromakey
sys.path.insert(0, str(Path(__file__).resolve().parent))
from chromakey import process as chroma_process, dilate

ROOT = Path("/workspace")
REPO = ROOT / "gamehub"
DRAW = REPO / "app/src/main/res/drawable"
PRIV = REPO / "app/src/private/res/drawable"
RAW_BG = ROOT / "bg-raw"
RAW_PP = ROOT / "pol-raw"
RAW_PL = ROOT / "pol-raw-likeness"
TMP = ROOT / "v22-tmp"
TMP.mkdir(exist_ok=True)

def save_webp(im: Image.Image, dest: Path, q=82):
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, "WEBP", quality=q, method=6)

def convert_backdrops():
    # 1024x576 keeps APK modest
    for src in sorted(RAW_BG.glob("bg_*.jpg")):
        im = Image.open(src).convert("RGB")
        im = im.resize((1024, 576), Image.Resampling.LANCZOS)
        dest = DRAW / (src.stem + ".webp")
        save_webp(im, dest, q=80)
        print("bg", dest.name, dest.stat().st_size)

def _fbm(h, w, octaves=5, seed=1, scale=8.0):
    rng = np.random.RandomState(seed)
    acc = np.zeros((h, w), dtype=np.float32)
    amp = 1.0
    total = 0.0
    for o in range(octaves):
        sh, sw = max(2, h // (scale * (2 ** o))), max(2, w // (scale * (2 ** o)))
        sh, sw = int(max(2, sh)), int(max(2, sw))
        grid = rng.rand(sh, sw).astype(np.float32)
        small = Image.fromarray((grid * 255).astype(np.uint8), "L").resize((w, h), Image.Resampling.BICUBIC)
        acc += np.array(small, dtype=np.float32) / 255.0 * amp
        total += amp
        amp *= 0.5
    return acc / total

def _norm(a):
    a = a - a.min()
    return a / (a.max() + 1e-6)

def wood_tex(h, w, base, dark, seed, rings=14.0):
    n = _fbm(h, w, 6, seed, 10)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    # wavy grain along x
    grain = np.sin((xx / w * rings + n * 3.5 + yy / h * 0.4) * np.pi * 2)
    grain = 0.5 + 0.5 * grain
    pores = _fbm(h, w, 4, seed + 9, 3)
    t = np.clip(grain * 0.75 + pores * 0.25, 0, 1)
    # slight vertical streaks
    streaks = _fbm(h, w, 3, seed + 3, 20)
    t = np.clip(t * 0.88 + streaks * 0.12, 0, 1)
    col = np.zeros((h, w, 3), dtype=np.float32)
    for i in range(3):
        col[..., i] = base[i] + (dark[i] - base[i]) * (1.0 - t)
    # scratch highlights
    col = np.clip(col + (t[..., None] - 0.5) * 12, 0, 255)
    return Image.fromarray(col.astype(np.uint8), "RGB")

def marble_tex(h, w, base, vein, seed):
    n = _fbm(h, w, 6, seed, 8)
    n2 = _fbm(h, w, 5, seed + 4, 5)
    v = np.abs(np.sin((n * 6.5 + n2 * 2.0) * np.pi))
    v = np.power(v, 8.0)  # thin veins
    v2 = np.power(np.abs(np.sin((n2 * 4.0 + n * 1.2) * np.pi)), 12)
    t = np.clip(v * 0.85 + v2 * 0.4, 0, 1)
    col = np.zeros((h, w, 3), dtype=np.float32)
    for i in range(3):
        col[..., i] = base[i] * (1 - t * 0.85) + vein[i] * t
    # soft blotches
    blot = _fbm(h, w, 3, seed + 7, 12)
    col += (blot[..., None] - 0.5) * 14
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), "RGB")

def metal_tex(h, w, base, seed):
    n = _fbm(h, w, 5, seed, 16)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    brush = 0.5 + 0.5 * np.sin((yy / 2.2 + n * 8) * 0.7)
    t = np.clip(brush * 0.7 + n * 0.3, 0, 1)
    col = np.zeros((h, w, 3), dtype=np.float32)
    hi = np.array([min(255, c + 50) for c in base], dtype=np.float32)
    lo = np.array([max(0, c - 40) for c in base], dtype=np.float32)
    for i in range(3):
        col[..., i] = lo[i] + (hi[i] - lo[i]) * t
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), "RGB")

def felt_tex(h, w, base, seed):
    n = _fbm(h, w, 4, seed, 4)
    col = np.zeros((h, w, 3), dtype=np.float32)
    for i in range(3):
        col[..., i] = np.clip(base[i] + (n - 0.5) * 28, 0, 255)
    return Image.fromarray(col.astype(np.uint8), "RGB")

def neon_tex(h, w, seed):
    n = _fbm(h, w, 4, seed, 6)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    grid = ((xx % 32) < 1.2) | ((yy % 32) < 1.2)
    col = np.zeros((h, w, 3), dtype=np.float32)
    col[..., 0] = 8 + n * 10
    col[..., 1] = 14 + n * 16
    col[..., 2] = 28 + n * 22
    col[grid, 0] = 0
    col[grid, 1] = 180
    col[grid, 2] = 220
    # dim the grid
    col[grid] *= 0.35
    return Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), "RGB")

def gold_tex(h, w, seed):
    return metal_tex(h, w, (196, 154, 58), seed)

def make_textures():
    S = 256
    specs = {
        "tex_wood_lt": wood_tex(S, S, (232, 201, 150), (176, 122, 64), 11),
        "tex_wood_dk": wood_tex(S, S, (176, 118, 58), (96, 54, 22), 17),
        "tex_wood_fr": wood_tex(S, S, (96, 54, 22), (48, 24, 10), 23),
        "tex_marble_lt": marble_tex(S, S, (236, 232, 224), (150, 148, 155), 31),
        "tex_marble_dk": marble_tex(S, S, (125, 134, 150), (60, 66, 80), 37),
        "tex_stone_fr": marble_tex(S, S, (62, 68, 80), (30, 32, 40), 41),
        "tex_metal": metal_tex(S, S, (160, 166, 176), 43),
        "tex_gold": gold_tex(S, S, 47),
        "tex_felt_lt": felt_tex(S, S, (238, 238, 210), 53),
        "tex_felt_dk": felt_tex(S, S, (118, 150, 86), 59),
        "tex_felt_fr": felt_tex(S, S, (48, 46, 42), 61),
        "tex_neon_lt": neon_tex(S, S, 19),
        "tex_neon_dk": Image.fromarray(np.clip(np.array(neon_tex(S, S, 29)) * 0.45, 0, 255).astype(np.uint8)),
        "tex_enchant_lt": marble_tex(S, S, (212, 196, 240), (140, 100, 190), 71),
        "tex_enchant_dk": marble_tex(S, S, (90, 58, 138), (40, 24, 70), 73),
        "tex_sakura_lt": wood_tex(S, S, (255, 232, 240), (230, 180, 196), 79),
        "tex_sakura_dk": wood_tex(S, S, (224, 144, 168), (160, 80, 100), 83),
        "tex_lumen_lt": marble_tex(S, S, (61, 74, 114), (90, 110, 160), 89),
        "tex_lumen_dk": marble_tex(S, S, (28, 36, 72), (16, 20, 40), 91),
        "tex_particle": particle_dot(64),
        "tex_letter": letter_atlas(),
    }
    for name, im in specs.items():
        dest = DRAW / f"{name}.webp"
        save_webp(im.convert("RGBA") if im.mode == "RGBA" else im.convert("RGB"), dest, q=85)
        print("tex", dest.name, dest.stat().st_size)

def particle_dot(s=64):
    a = np.zeros((s, s, 4), dtype=np.uint8)
    cy = cx = (s - 1) / 2
    for y in range(s):
        for x in range(s):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / (s * 0.48)
            v = int(np.clip(1.0 - d, 0, 1) ** 2 * 255)
            a[y, x] = (255, 240, 210, v)
    return Image.fromarray(a, "RGBA")

def letter_atlas():
    # 16 cells: 0-7 = A-H, 8-15 = 1-8. 64x64 each, 256x256 sheet, white glyph + alpha.
    cell = 64
    im = Image.new("RGBA", (cell * 4, cell * 4), (0, 0, 0, 0))
    dr = ImageDraw.Draw(im)
    chars = list("ABCDEFGH") + list("12345678")
    try:
        from PIL import ImageFont
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", 42)
    except Exception:
        font = ImageFont.load_default()
    for i, ch in enumerate(chars):
        r, c = divmod(i, 4)
        x0, y0 = c * cell, r * cell
        # measure
        bbox = dr.textbbox((0, 0), ch, font=font)
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
        x = x0 + (cell - tw) / 2 - bbox[0]
        y = y0 + (cell - th) / 2 - bbox[1]
        # slight shadow to read as engraved depth
        dr.text((x + 1, y + 1), ch, font=font, fill=(0, 0, 0, 180))
        dr.text((x, y), ch, font=font, fill=(255, 236, 200, 230))
    return im

# ---------- figure recrop ----------
def hsv(rgb):
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    mx = np.maximum(np.maximum(r, g), b)
    mn = np.minimum(np.minimum(r, g), b)
    ch = np.maximum(mx - mn, 1e-6)
    hue = np.zeros(r.shape, dtype=np.float32)
    mask_r = (mx == r) & (mx > mn)
    mask_g = (mx == g) & (mx > mn) & ~mask_r
    mask_b = (mx == b) & (mx > mn) & ~mask_r & ~mask_g
    hue[mask_r] = (60.0 * ((g[mask_r] - b[mask_r]) / ch[mask_r])) % 360
    hue[mask_g] = (60.0 * ((b[mask_g] - r[mask_g]) / ch[mask_g] + 2.0)) % 360
    hue[mask_b] = (60.0 * ((r[mask_b] - g[mask_b]) / ch[mask_b] + 4.0)) % 360
    sat = ch / np.maximum(mx, 1e-6)
    val = mx / 255.0
    return hue, sat, val

def stronger_despill(a: np.ndarray) -> np.ndarray:
    """Pull magenta/pink fringe toward luminance; 1px alpha erode."""
    out = a.copy()
    rgb = out[..., :3].astype(np.float32)
    alpha = out[..., 3].astype(np.float32)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    # magenta excess: R and B above G
    mag = np.maximum(r, b) - g
    # edge: alpha between 1 and 250 or neighbor transparent
    solid = alpha > 8
    # neighbor min alpha
    amin = alpha.copy()
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        rolled = np.roll(alpha, (dy, dx), (0, 1))
        amin = np.minimum(amin, rolled)
    edge = solid & ((alpha < 250) | (amin < 40))
    # also pixels that are pinkish even if interior-ish near crop
    hue, sat, val = hsv(rgb)
    # magenta / hot pink hue ~ 300 ± 30, or 330-360 / 0-10 with high sat
    pink_hue = ((hue >= 280) & (hue <= 340) & (sat > 0.18) & (val > 0.25))
    spill = edge & ((mag > 12) | pink_hue)
    # pull R and B toward G, amount based on magenta and how edgy
    amt = np.clip((mag / 40.0) * 0.85, 0, 0.9)
    amt = np.where(spill, np.maximum(amt, 0.45), 0)
    r2 = r * (1 - amt) + g * amt
    b2 = b * (1 - amt) + g * amt
    rgb[..., 0] = r2
    rgb[..., 2] = b2
    out[..., :3] = np.clip(rgb, 0, 255).astype(np.uint8)
    # 1px erode: shrink coverage slightly to drop leftover fringe
    a2 = alpha.copy()
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (-1, 1), (1, -1), (1, 1)):
        a2 = np.minimum(a2, np.roll(alpha, (dy, dx), (0, 1)))
    # only erode true edges, keep interior
    out[..., 3] = np.where(alpha > 0, np.clip(a2 * 0.55 + alpha * 0.45, 0, 255), 0).astype(np.uint8)
    # hard cut leftover near-transparent pink
    weak = (out[..., 3] < 28) & spill
    out[..., 3] = np.where(weak, 0, out[..., 3])
    return out

def is_pedestal_row(rgb, alpha, y, min_w):
    row_a = alpha[y] > 20
    if row_a.sum() < min_w * 0.15:
        return False, 0.0
    pix = rgb[y][row_a].astype(np.float32)
    hue, sat, val = hsv(pix[None, ...])
    hue, sat, val = hue[0], sat[0], val[0]
    # wood / tan / brown
    wood = ((hue >= 12) & (hue <= 52) & (sat >= 0.18) & (val >= 0.12) & (val <= 0.85))
    # dark ring / black plastic base
    dark = (val < 0.28) & (sat < 0.35)
    # ivory/staugton cream stem
    ivory = (val > 0.62) & (sat < 0.22) & (hue < 60)
    frac = (wood | dark | ivory).mean()
    width = row_a.sum()
    return frac > 0.55, frac

def crop_pedestal(a: np.ndarray) -> np.ndarray:
    h, w = a.shape[:2]
    alpha = a[..., 3]
    rgb = a[..., :3]
    rows = np.where(alpha.max(axis=1) > 20)[0]
    if len(rows) == 0:
        return a
    y0, y1 = int(rows[0]), int(rows[-1])
    # walk up from bottom while rows look like pedestal
    cut = y1 + 1
    consecutive = 0
    started = False
    min_w = w * 0.2
    for y in range(y1, max(y0, y1 - int(h * 0.55)), -1):
        ok, frac = is_pedestal_row(rgb, alpha, y, min_w)
        if ok:
            started = True
            consecutive += 1
            cut = y
        else:
            if started and consecutive >= 6:
                break
            if started and consecutive < 6:
                # short blip, keep going a little
                if consecutive > 0:
                    consecutive -= 1
                if frac < 0.25 and consecutive == 0:
                    # definitely not pedestal
                    break
    if cut >= y1 or (y1 - cut) < 4:
        return a  # nothing reliable to crop (e.g. new Hawley)
    # leave 2px below the feet
    cut = min(y1, cut + 2)
    # only crop if we are removing a meaningful base (at least ~4% height)
    if (y1 - cut) < max(8, int(h * 0.04)):
        return a
    out = a[:cut + 1].copy()
    return out

def trim_alpha(a: np.ndarray, pad=2) -> np.ndarray:
    alpha = a[..., 3]
    ys, xs = np.where(alpha > 8)
    if len(ys) == 0:
        return a
    y0, y1 = max(0, ys.min() - pad), min(a.shape[0] - 1, ys.max() + pad)
    x0, x1 = max(0, xs.min() - pad), min(a.shape[1] - 1, xs.max() + pad)
    return a[y0:y1 + 1, x0:x1 + 1]

def fit_height(im: Image.Image, max_h=512) -> Image.Image:
    if im.height <= max_h:
        return im
    w = max(1, int(im.width * (max_h / im.height)))
    return im.resize((w, max_h), Image.Resampling.LANCZOS)

def process_figure(src: Path, dest: Path, already_webp=False):
    tmp = TMP / (src.stem + "_key.png")
    if already_webp:
        im = Image.open(src).convert("RGBA")
        im.save(tmp)
        # still run a light re-key? skip chroma, just despill+crop
        a = np.array(im)
    else:
        chroma_process(src, tmp, max_h=720, thresh=14.0)
        a = np.array(Image.open(tmp).convert("RGBA"))
    a = stronger_despill(a)
    a = crop_pedestal(a)
    a = stronger_despill(a)
    a = trim_alpha(a, pad=2)
    im = Image.fromarray(a, "RGBA")
    im = fit_height(im, 512)
    save_webp(im, dest, q=82)
    return im

def process_all_figures():
    sheet_pp = []
    for src in sorted(RAW_PP.glob("pp_*.jpg")):
        dest = DRAW / (src.stem + ".webp")
        im = process_figure(src, dest)
        sheet_pp.append((src.stem, im))
        print("fig", dest.name, im.size, dest.stat().st_size)
    if PRIV.exists() or RAW_PL.exists():
        PRIV.mkdir(parents=True, exist_ok=True)
        for src in sorted(RAW_PL.glob("pl_*.jpg")):
            dest = PRIV / (src.stem + ".webp")
            im = process_figure(src, dest)
            print("fig-priv", dest.name, im.size, dest.stat().st_size)
    # contact sheet of parody
    if sheet_pp:
        cell_w, cell_h = 120, 280
        cols = 8
        rows = (len(sheet_pp) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (40, 40, 48, 255))
        for i, (name, im) in enumerate(sheet_pp):
            r, c = divmod(i, cols)
            thumb = im.copy()
            thumb.thumbnail((cell_w - 8, cell_h - 24), Image.Resampling.LANCZOS)
            x = c * cell_w + (cell_w - thumb.width) // 2
            y = r * cell_h + (cell_h - 18 - thumb.height)
            sheet.paste(thumb, (x, y), thumb)
        sheet.save(TMP / "parody_sheet.png")
        print("wrote", TMP / "parody_sheet.png")

def main():
    convert_backdrops()
    make_textures()
    process_all_figures()

if __name__ == "__main__":
    main()
