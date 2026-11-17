#!/usr/bin/env python3
"""Variance-aware chromakey, rim despill, wood-base crop. Replaces v2.1 pedestals."""
from __future__ import annotations
from collections import deque
from pathlib import Path
import numpy as np
from PIL import Image
from numpy.lib.stride_tricks import sliding_window_view

ROOT = Path("/workspace")
REPO = ROOT / "gamehub"
DRAW = REPO / "app/src/main/res/drawable"
PRIV = REPO / "app/src/private/res/drawable"
TMP = ROOT / "v22-tmp"
TMP.mkdir(exist_ok=True)

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

def local_var(luma, k=5):
    pad = k // 2
    lp = np.pad(luma, pad, mode="edge")
    win = sliding_window_view(lp, (k, k))
    return win.var(axis=(-1, -2))

def chroma_var(src, dth=16.0, vth=30.0):
    im = np.array(Image.open(src).convert("RGB"))
    h, w = im.shape[:2]
    rgb = im.astype(np.float32)
    border = np.concatenate([rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]])
    key = np.median(border, axis=0)
    dist = np.linalg.norm(rgb - key, axis=2)
    luma = 0.3 * rgb[..., 0] + 0.59 * rgb[..., 1] + 0.11 * rgb[..., 2]
    var = local_var(luma, 5)
    close = (dist <= dth) & (var <= vth)
    # also allow slightly looser color if almost zero variance (flat studio bg)
    close |= (dist <= dth + 8) & (var <= 8)
    vis = np.zeros((h, w), dtype=bool)
    q = deque()
    def seed(y, x):
        if close[y, x] and not vis[y, x]:
            vis[y, x] = True
            q.append((y, x))
    for x in range(w):
        seed(0, x); seed(h - 1, x)
    for y in range(h):
        seed(y, 0); seed(y, w - 1)
    while q:
        y, x = q.popleft()
        for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not vis[ny, nx] and close[ny, nx]:
                vis[ny, nx] = True
                q.append((ny, nx))
    alpha = np.where(vis, 0, 255).astype(np.uint8)
    # soft edge: dist near threshold on the subject boundary
    # pixels just inside, mix alpha down if close to key
    edge_near = (~vis) & (dist < dth + 10)
    # only the rim: a neighbor is background
    rim = np.zeros((h, w), dtype=bool)
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        rim |= np.roll(vis, (dy, dx), (0, 1))
    rim &= ~vis
    fade = np.clip((dth + 6 - dist) / 12.0, 0, 1)
    alpha = np.where(rim & (dist < dth + 10), (alpha * (1 - fade * 0.7)).astype(np.uint8), alpha)
    out = np.dstack([im, alpha])
    return out, key

def rim_despill(a, key):
    """Pull residual key-color fringe on the rim only; do not touch interior clothing."""
    out = a.copy()
    rgb = out[..., :3].astype(np.float32)
    alpha = out[..., 3].astype(np.float32)
    vis = alpha < 8
    rim = np.zeros(alpha.shape, dtype=bool)
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (-1, 1), (1, -1), (1, 1)):
        rim |= np.roll(vis, (dy, dx), (0, 1))
    rim &= alpha >= 8
    keyv = np.array(key, dtype=np.float32)
    dist = np.linalg.norm(rgb - keyv, axis=2)
    # also classic magenta
    mag = np.array([255, 0, 255], dtype=np.float32)
    dmag = np.linalg.norm(rgb - mag, axis=2)
    pull = rim & ((dist < 40) | (dmag < 50))
    # move rgb toward luminance, away from key
    luma = (0.3 * rgb[..., 0] + 0.59 * rgb[..., 1] + 0.11 * rgb[..., 2])[..., None]
    amt = np.clip(1.0 - np.minimum(dist, dmag) / 50.0, 0, 0.85)[..., None]
    amt = np.where(pull[..., None], amt, 0)
    rgb = rgb * (1 - amt) + luma * amt
    out[..., :3] = np.clip(rgb, 0, 255).astype(np.uint8)
    # 1px alpha shrink on pulled rim
    a2 = alpha.copy()
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        a2 = np.minimum(a2, np.roll(alpha, (dy, dx), (0, 1)))
    out[..., 3] = np.where(pull, (alpha * 0.4 + a2 * 0.6).astype(np.uint8), out[..., 3])
    return out

def crop_pedestal(a):
    h, w = a.shape[:2]
    rgb = a[..., :3].astype(np.float32)
    alpha = a[..., 3]
    hue, sat, val = hsv(rgb)
    solid = alpha > 20
    wood = solid & (hue >= 10) & (hue <= 55) & (sat >= 0.18) & (val >= 0.28) & (val <= 0.92)
    red = solid & ((hue <= 12) | (hue >= 345)) & (sat >= 0.35) & (val >= 0.25)
    blue = solid & (hue >= 185) & (hue <= 255) & (sat >= 0.28) & (val >= 0.18)
    walk = wood | red | blue
    y_mid = int(h * 0.55)
    if wood[y_mid:, :].sum() < 400:
        return a  # no painted pedestal (Hawley-style)
    vis = np.zeros((h, w), dtype=bool)
    q = deque()
    y_seed = int(h * 0.90)
    ys, xs = np.where(walk[y_seed:, :])
    for i in range(len(ys)):
        y = ys[i] + y_seed
        x = xs[i]
        vis[y, x] = True
        q.append((y, x))
    if not q:
        ys, xs = np.where(walk)
        # take lowest 8% of wood rows as seeds
        if len(ys) == 0:
            return a
        cuty = np.percentile(ys, 92)
        for i in range(len(ys)):
            if ys[i] >= cuty:
                vis[ys[i], xs[i]] = True
                q.append((int(ys[i]), int(xs[i])))
    while q:
        y, x = q.popleft()
        for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not vis[ny, nx] and walk[ny, nx]:
                vis[ny, nx] = True
                q.append((ny, nx))
    if vis.sum() < 400:
        return a
    top = int(np.where(vis)[0].min())
    # keep 2px of the platform lip under the shoes so feet aren't clipped
    cut = min(h, top + 2)
    if cut < int(h * 0.45):  # safety: don't cut the body
        return a
    if (h - cut) < 8:
        return a
    return a[:cut].copy()

def trim_alpha(a, pad=2):
    ys, xs = np.where(a[..., 3] > 8)
    if len(ys) == 0:
        return a
    y0, y1 = max(0, ys.min() - pad), min(a.shape[0] - 1, ys.max() + pad)
    x0, x1 = max(0, xs.min() - pad), min(a.shape[1] - 1, xs.max() + pad)
    return a[y0:y1 + 1, x0:x1 + 1]

def fit_height(im, max_h=512):
    if im.height <= max_h:
        return im
    nw = max(1, int(im.width * (max_h / im.height)))
    return im.resize((nw, max_h), Image.Resampling.LANCZOS)

def process_one(src, dest):
    a, key = chroma_var(src)
    a = rim_despill(a, key)
    a = crop_pedestal(a)
    a = trim_alpha(a, 2)
    im = fit_height(Image.fromarray(a, "RGBA"), 512)
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, "WEBP", quality=82, method=6)
    return im

def sheet(items, path, cell_w=120, cell_h=280, cols=8):
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (40, 40, 48, 255))
    for i, (name, im) in enumerate(items):
        r, c = divmod(i, cols)
        th = im.copy()
        th.thumbnail((cell_w - 8, cell_h - 24), Image.Resampling.LANCZOS)
        x = c * cell_w + (cell_w - th.width) // 2
        y = r * cell_h + (cell_h - 18 - th.height)
        sheet.paste(th, (x, y), th)
    sheet.save(path)

def main():
    items = []
    for src in sorted((ROOT / "pol-raw").glob("pp_*.jpg")):
        dest = DRAW / (src.stem + ".webp")
        im = process_one(src, dest)
        items.append((src.stem, im))
        print("fig", dest.name, im.size, dest.stat().st_size, flush=True)
    sheet(items, TMP / "parody_sheet.png")
    items = []
    for src in sorted((ROOT / "pol-raw-likeness").glob("pl_*.jpg")):
        dest = PRIV / (src.stem + ".webp")
        im = process_one(src, dest)
        items.append((src.stem, im))
        print("fig-priv", dest.name, im.size, dest.stat().st_size, flush=True)
    if items:
        sheet(items, TMP / "likeness_sheet.png")
    print("done")

if __name__ == "__main__":
    main()
