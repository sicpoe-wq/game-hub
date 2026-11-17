#!/usr/bin/env python3
"""Tight corner flood-fill chromakey, drop floor shadows, despill, trim, WebP."""
from pathlib import Path
import sys
from collections import deque
import numpy as np
from PIL import Image

def dilate(mask, n=1):
    out = mask.copy()
    for _ in range(n):
        m = out
        shifted = (
            m
            | np.roll(m, 1, 0) | np.roll(m, -1, 0)
            | np.roll(m, 1, 1) | np.roll(m, -1, 1)
        )
        shifted[0] &= m[0] | m[1]
        shifted[-1] &= m[-1] | m[-2]
        shifted[:, 0] &= m[:, 0] | m[:, 1]
        shifted[:, -1] &= m[:, -1] | m[:, -2]
        # fix wrap: restore edges properly
        out = m.copy()
        out[1:-1, 1:-1] = shifted[1:-1, 1:-1]
        out[0, 1:-1] = m[0, 1:-1] | m[1, 1:-1] | m[0, 0:-2] | m[0, 2:]
        out[-1, 1:-1] = m[-1, 1:-1] | m[-2, 1:-1] | m[-1, 0:-2] | m[-1, 2:]
        out[1:-1, 0] = m[1:-1, 0] | m[1:-1, 1] | m[0:-2, 0] | m[2:, 0]
        out[1:-1, -1] = m[1:-1, -1] | m[1:-1, -2] | m[0:-2, -1] | m[2:, -1]
    return out

def process(src, dest, max_h=512, thresh=14.0):
    im = Image.open(src).convert("RGBA")
    a = np.array(im)
    h, w = a.shape[:2]
    rgb = a[..., :3].astype(np.int16)
    corners = np.stack([
        rgb[0, 0], rgb[0, -1], rgb[-1, 0], rgb[-1, -1],
        rgb[0, w // 2], rgb[-1, w // 2], rgb[h // 2, 0], rgb[h // 2, -1],
        rgb[2, 2], rgb[2, -3], rgb[-3, 2], rgb[-3, -3],
    ])
    key = np.median(corners, axis=0)
    dist = np.linalg.norm(rgb - key, axis=2)

    # HSV-ish hue distance to key (degrees). Used for shadow / spill.
    rf, gf, bf = rgb[..., 0].astype(np.float32), rgb[..., 1].astype(np.float32), rgb[..., 2].astype(np.float32)
    mx = np.maximum(np.maximum(rf, gf), bf)
    mn = np.minimum(np.minimum(rf, gf), bf)
    chroma = np.maximum(mx - mn, 1e-6)
    hue = np.zeros((h, w), dtype=np.float32)
    mask_r = (mx == rf) & (mx > mn)
    mask_g = (mx == gf) & (mx > mn) & ~mask_r
    mask_b = (mx == bf) & (mx > mn) & ~mask_r & ~mask_g
    hue[mask_r] = (60.0 * ((gf[mask_r] - bf[mask_r]) / chroma[mask_r])) % 360
    hue[mask_g] = (60.0 * ((bf[mask_g] - rf[mask_g]) / chroma[mask_g] + 2.0)) % 360
    hue[mask_b] = (60.0 * ((rf[mask_b] - gf[mask_b]) / chroma[mask_b] + 4.0)) % 360
    kr, kg, kb = [float(x) for x in key]
    kmx = max(kr, kg, kb); kmn = min(kr, kg, kb); kc = max(kmx - kmn, 1e-6)
    if kmx == kr:
        kh = (60.0 * ((kg - kb) / kc)) % 360
    elif kmx == kg:
        kh = (60.0 * ((kb - kr) / kc + 2.0)) % 360
    else:
        kh = (60.0 * ((kr - kg) / kc + 4.0)) % 360
    dh = np.abs(hue - kh)
    dh = np.minimum(dh, 360.0 - dh)
    sat = chroma / np.maximum(mx, 1e-6)

    # --- tight border flood ---
    vis = np.zeros((h, w), dtype=bool)
    seed_ok = dist <= (thresh + 6)
    q = deque()
    def try_seed(y, x):
        if seed_ok[y, x] and not vis[y, x]:
            vis[y, x] = True
            q.append((y, x))
    for x in range(w):
        try_seed(0, x); try_seed(h - 1, x)
    for y in range(h):
        try_seed(y, 0); try_seed(y, w - 1)
    close = dist <= thresh
    while q:
        y, x = q.popleft()
        if y > 0 and not vis[y - 1, x] and close[y - 1, x]:
            vis[y - 1, x] = True; q.append((y - 1, x))
        if y + 1 < h and not vis[y + 1, x] and close[y + 1, x]:
            vis[y + 1, x] = True; q.append((y + 1, x))
        if x > 0 and not vis[y, x - 1] and close[y, x - 1]:
            vis[y, x - 1] = True; q.append((y, x - 1))
        if x + 1 < w and not vis[y, x + 1] and close[y, x + 1]:
            vis[y, x + 1] = True; q.append((y, x + 1))

    # interior leftover bg pockets (between legs / under arms)
    vis |= (~vis) & (dist <= thresh)

    # pink-like (bg or a darkened contact-shadow of the bg), not wood/skin/navy
    pinkish = ((dh < 24) | ((rf > gf + 40) & (bf > gf))) & (rf > gf + 15) & (rf > 70) & (gf < 130)

    # drop floor contact-shadow: side blobs live *beside* the pedestal, not only below it
    solid = (~vis) & (~pinkish)
    ys = np.arange(h)[:, None]
    xs = np.arange(w)[None, :]
    solid_y = np.where(solid, ys, -1)
    solid_x = np.where(solid, xs, -1)
    cutoff = solid_y.max(axis=0)  # lowest solid y per column, or -1
    if solid.any():
        sx0 = int(np.where(solid, xs, w).min())
        sx1 = int(solid_x.max())
        sy1 = int(solid_y.max())
    else:
        sx0, sx1, sy1 = 0, w - 1, h - 1
    # columns with no solid figure at all (typical left/right shadow)
    no_solid_col = cutoff < 0
    # below the pedestal lip, or outside the solid x-span, or hanging off the base
    below = (ys > (cutoff[None, :] + 2)) & (cutoff[None, :] >= 0)
    beside = (xs < sx0 - 1) | (xs > sx1 + 1)
    lip = ys > (sy1 - 6)
    vis |= pinkish & (~vis) & (below | no_solid_col[None, :] | beside | lip)

    # also drop small leftover pink blobs not part of the main figure
    opaque = ~vis
    # label connected components of opaque (simple flood)
    labels = np.zeros((h, w), dtype=np.int32)
    lab = 0
    sizes = {}
    yy, xx = np.where(opaque)
    # only if there are multiple components worth checking — skip if cheap path
    # Use a light scan
    seen = np.zeros((h, w), dtype=bool)
    comps = []
    for y, x in zip(yy, xx):
        if seen[y, x]:
            continue
        lab += 1
        q = deque([(y, x)])
        seen[y, x] = True
        cells = []
        while q:
            cy, cx = q.popleft()
            cells.append((cy, cx))
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                ny, nx = cy + dy, cx + dx
                if 0 <= ny < h and 0 <= nx < w and opaque[ny, nx] and not seen[ny, nx]:
                    seen[ny, nx] = True
                    q.append((ny, nx))
        comps.append(cells)
    if comps:
        comps.sort(key=len, reverse=True)
        # keep largest; drop other components that are mostly pinkish (shadows/specks)
        for cells in comps[1:]:
            ys_, xs_ = zip(*cells)
            arr = np.array(cells)
            frac = pinkish[arr[:, 0], arr[:, 1]].mean()
            if frac > 0.4 or len(cells) < 800:
                vis[arr[:, 0], arr[:, 1]] = True

    alpha = a[..., 3].astype(np.float32)
    alpha[vis] = 0.0

    # Feather + despill on the rim
    rim = dilate(vis, 3) & ~vis
    fade = np.clip((dist - thresh) / 28.0, 0.0, 1.0)
    alpha[rim] *= fade[rim]
    # extra: pixels still very close to key become more transparent
    near = (~vis) & (dist < 22)
    alpha[near] *= np.clip((dist[near] - 6) / 16.0, 0.0, 1.0)

    # despill: pull rim toward luminance to remove pink fringe
    lum = 0.299 * rf + 0.587 * gf + 0.114 * bf
    spill_w = np.clip(1.0 - (dist / 50.0), 0.0, 0.85)
    spill_w = np.where(rim, spill_w, 0.0)
    for c in range(3):
        ch = a[..., c].astype(np.float32)
        a[..., c] = np.clip(ch * (1.0 - spill_w) + lum * spill_w, 0, 255).astype(np.uint8)

    a[..., 3] = np.clip(alpha, 0, 255).astype(np.uint8)

    ys, xs = np.where(a[..., 3] > 10)
    if len(ys) == 0:
        print("EMPTY", src)
        return
    pad = 2
    y0, y1 = max(0, int(ys.min()) - pad), min(h, int(ys.max()) + pad + 1)
    x0, x1 = max(0, int(xs.min()) - pad), min(w, int(xs.max()) + pad + 1)
    cropped = Image.fromarray(a[y0:y1, x0:x1])
    if cropped.height > max_h:
        nw = max(1, int(round(cropped.width * max_h / cropped.height)))
        cropped = cropped.resize((nw, max_h), Image.Resampling.LANCZOS)
    dest = Path(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    cropped.save(dest, "WEBP", quality=82, method=6)
    print(f"{dest.name} {cropped.size} {dest.stat().st_size}")

def main():
    if len(sys.argv) < 3:
        print("usage: chromakey.py SRC_DIR DEST_DIR")
        sys.exit(2)
    src, dest = Path(sys.argv[1]), Path(sys.argv[2])
    paths = sorted(p for p in src.iterdir() if p.suffix.lower() in {".jpg", ".jpeg", ".png"})
    n = 0
    for p in paths:
        process(p, dest / (p.stem + ".webp"))
        n += 1
    print("done", n)

if __name__ == "__main__":
    main()
