#!/usr/bin/env python3
"""Background-remove political figure stills with rembg (not chroma key).

Writes tightly cropped transparent WebPs. Default model: u2net.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from rembg import new_session, remove


def decontaminate(rgba: np.ndarray) -> np.ndarray:
    """Pull RGB of partial-alpha edge pixels toward nearby opaque color
    so a gray studio backdrop does not leave a fringe."""
    a = rgba[:, :, 3]
    rgb = rgba[:, :, :3].astype(np.float32)
    opaque = a >= 250
    if not opaque.any():
        return rgba
    # 3x3 max of opaque RGB (morphological 'propagate')
    from scipy.ndimage import maximum_filter, binary_dilation, binary_erosion

    mask = opaque
    # build a color field from dilated opaque pixels
    color = rgb.copy()
    color[~mask] = 0
    for _ in range(4):
        # dilate mask and copy neighbor color into newly covered pixels
        new_mask = binary_dilation(mask, iterations=1)
        ring = new_mask & ~mask
        if not ring.any():
            break
        # average of 8-neighbors that are already known
        acc = np.zeros_like(color)
        cnt = np.zeros(mask.shape, dtype=np.float32)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dy == 0 and dx == 0:
                    continue
                sh = np.roll(np.roll(color, dy, 0), dx, 1)
                sm = np.roll(np.roll(mask, dy, 0), dx, 1)
                acc[ring] += sh[ring]
                cnt[ring] += sm[ring].astype(np.float32)
        take = ring & (cnt > 0)
        color[take] = acc[take] / cnt[take, None]
        mask = new_mask
    # partial alpha: blend toward propagated opaque color
    partial = (a > 8) & (a < 250)
    if partial.any():
        rgb[partial] = color[partial]
    # crush weak alpha (studio dust)
    a = a.astype(np.float32)
    a[a < 16] = 0
    # slight alpha erode to drop leftover halo
    bin_mask = a >= 16
    er = binary_erosion(bin_mask, iterations=1)
    # keep a 1px soft edge: eroded core stays, ring gets scaled
    ring = bin_mask & ~er
    a[ring] = np.minimum(a[ring], 140)
    a[~bin_mask] = 0
    out = rgba.copy()
    out[:, :, :3] = np.clip(rgb, 0, 255).astype(np.uint8)
    out[:, :, 3] = np.clip(a, 0, 255).astype(np.uint8)
    return out


def crop_tight(im: Image.Image, pad_frac: float = 0.03) -> Image.Image:
    arr = np.array(im)
    a = arr[:, :, 3]
    ys, xs = np.where(a >= 16)
    if xs.size == 0:
        return im
    x0, x1 = int(xs.min()), int(xs.max()) + 1
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    w, h = x1 - x0, y1 - y0
    pad = int(max(w, h) * pad_frac)
    x0 = max(0, x0 - pad)
    y0 = max(0, y0 - pad)
    x1 = min(im.width, x1 + pad)
    y1 = min(im.height, y1 + pad)
    return im.crop((x0, y0, x1, y1))


def resize_max(im: Image.Image, max_h: int = 640) -> Image.Image:
    if im.height <= max_h:
        return im
    w = int(round(im.width * (max_h / im.height)))
    return im.resize((w, max_h), Image.Resampling.LANCZOS)


def process_one(src: Path, dst: Path, session, max_h: int) -> None:
    raw = Image.open(src).convert("RGB")
    cut = remove(raw, session=session)
    if cut.mode != "RGBA":
        cut = cut.convert("RGBA")
    arr = decontaminate(np.array(cut))
    im = Image.fromarray(arr, "RGBA")
    im = crop_tight(im)
    im = resize_max(im, max_h)
    dst.parent.mkdir(parents=True, exist_ok=True)
    im.save(dst, "WEBP", quality=84, method=6, lossless=False)
    print(f"{src.name} -> {dst.name} {im.size} {dst.stat().st_size}B", flush=True)


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--src", type=Path, default=Path("/workspace/pl-raw-v23"))
    p.add_argument("--dst", type=Path, default=Path("/workspace/gamehub/app/src/private/res/drawable"))
    p.add_argument("--max-h", type=int, default=640)
    p.add_argument("--model", default="u2net")
    args = p.parse_args()
    files = sorted(args.src.glob("pl_*.jpg"))
    if not files:
        print("no pl_*.jpg in", args.src, file=sys.stderr)
        return 1
    session = new_session(args.model)
    for f in files:
        process_one(f, args.dst / (f.stem + ".webp"), session, args.max_h)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
