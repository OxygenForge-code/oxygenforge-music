"""Generates OxygenForge Music launcher icon rasters.

Run from the repository root:  python tools/generate-icons.py

Everything is drawn into a 1024x1024 "design space" that maps onto the 108dp
adaptive-icon canvas, so the mark lands inside the 72dp safe zone. One master
render per variant is downscaled with LANCZOS to every density.

Outputs (app/src/main/res):
  mipmap-<density>/ic_glass_*_background.png       one tile per icon variant
  mipmap-<density>/ic_launcher_background.png      default adaptive background
  mipmap-<density>/ic_glass_mark.png               white mark (dark tiles)
  mipmap-<density>/ic_glass_mark_ink.png           ink mark (clear tiles)
  mipmap-<density>/ic_glass_monochrome.png         mark alpha mask
  mipmap-<density>/ic_<variant>_foreground.png     finished icon, used as picker preview
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

RES = Path("app/src/main/res")
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
SS = 2  # supersampling factor on top of the 1024 design space
D = 1024 * SS


# ---------------------------------------------------------------- primitives


def new_canvas() -> Image.Image:
    return Image.new("RGBA", (D, D), (0, 0, 0, 0))


def squircle_points(inset: float, n: float = 4.2, steps: int = 720):
    half = (D / 2) - inset
    cx = cy = D / 2
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        cos_t, sin_t = math.cos(t), math.sin(t)
        x = cx + half * math.copysign(abs(cos_t) ** (2 / n), cos_t)
        y = cy + half * math.copysign(abs(sin_t) ** (2 / n), sin_t)
        pts.append((x, y))
    return pts


def circle_points(inset: float, steps: int = 720):
    r = (D / 2) - inset
    c = D / 2
    return [
        (c + r * math.cos(2 * math.pi * i / steps), c + r * math.sin(2 * math.pi * i / steps))
        for i in range(steps)
    ]


def shape_mask(points) -> Image.Image:
    mask = Image.new("L", (D, D), 0)
    ImageDraw.Draw(mask).polygon(points, fill=255)
    return mask


GRAD_RES = 128


def linear_gradient(stops, angle: float = 90.0) -> Image.Image:
    """Multi-stop linear gradient; `angle` in degrees, 90 = top to bottom.

    Rendered small and upscaled: a linear ramp survives resampling untouched
    and this keeps the generator fast enough to re-run on a whim.
    """
    rad = math.radians(angle)
    dx, dy = math.cos(rad), math.sin(rad)
    n = GRAD_RES
    img = Image.new("RGBA", (n, n))
    px = img.load()
    half = n / 2
    proj = [(x * dx + y * dy) for x, y in [(-half, -half), (half, -half), (-half, half), (half, half)]]
    lo, hi = min(proj), max(proj)
    span = hi - lo
    for y in range(n):
        for x in range(n):
            t = ((x - half) * dx + (y - half) * dy - lo) / span
            px[x, y] = sample(stops, t)
    return img.resize((D, D), Image.BICUBIC)


def sample(stops, t: float):
    t = max(0.0, min(1.0, t))
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        if p0 <= t <= p1:
            k = 0.0 if p1 == p0 else (t - p0) / (p1 - p0)
            return tuple(int(round(c0[j] + (c1[j] - c0[j]) * k)) for j in range(4))
    return stops[-1][1]


def radial_glow(center, radius, color, alpha: int, steps: int = 90) -> Image.Image:
    """Soft radial falloff; drawn at quarter scale, then upscaled."""
    scale = 4
    small = D // scale
    size = small
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    cx, cy = center[0] / scale, center[1] / scale
    radius = radius / scale
    for i in range(steps, 0, -1):
        r = radius * i / steps
        a = int(alpha * (1 - i / steps) ** 1.6)
        if a <= 0:
            continue
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*color, a))
    layer = layer.resize((D, D), Image.BICUBIC)
    return layer.filter(ImageFilter.GaussianBlur(radius * scale * 0.10))



def paste_through(base: Image.Image, art: Image.Image, mask: Image.Image):
    base.paste(art, (0, 0), mask)


# ---------------------------------------------------------------- the mark


def _bezier(p0, p1, p2, steps=40):
    out = []
    for s in range(steps + 1):
        t = s / steps
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t**2 * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t**2 * p2[1]
        out.append((x, y))
    return out


def sparkle(draw, cx, cy, radius, waist=0.16, points=4, steps=24, fill=255):
    """A four-point sparkle: curved (concave) sides rather than straight star arms."""
    verts = []
    for i in range(points * 2):
        angle = -math.pi / 2 + i * math.pi / points
        r = radius if i % 2 == 0 else radius * waist
        verts.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    path = []
    for i in range(len(verts)):
        p0 = verts[i]
        p2 = verts[(i + 1) % len(verts)]
        mid = ((p0[0] + p2[0]) / 2, (p0[1] + p2[1]) / 2)
        # Pull the midpoint toward the centre so the arms read as tapered.
        ctrl = (cx + (mid[0] - cx) * 0.42, cy + (mid[1] - cy) * 0.42)
        for s in range(steps):
            t = s / steps
            x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * ctrl[0] + t**2 * p2[0]
            y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * ctrl[1] + t**2 * p2[1]
            path.append((x, y))
    draw.polygon(path, fill=fill)


def mark_layers(tint_top, tint_bottom, glow_color, glow_alpha=120):
    """Returns (glow, mark, mask) for the OxygenForge mark in design space.

    A forge ring (the "O") around a single eighth note, with a spark off the
    ring's shoulder. Ring: centre 512, radius 236, stroke 30.
    """
    ring = Image.new("L", (D, D), 0)
    rd = ImageDraw.Draw(ring)
    stroke = 34 * SS
    r = 236 * SS
    c = D / 2
    rd.ellipse([c - r, c - r, c + r, c + r], outline=255, width=int(stroke))

    note = Image.new("L", (D, D), 0)
    nd = ImageDraw.Draw(note)

    # Note head: a rotated ellipse sitting clear of the ring.
    hrx, hry = 66 * SS, 50 * SS
    head = Image.new("L", (int(2 * hrx) + 8, int(2 * hry) + 8), 0)
    ImageDraw.Draw(head).ellipse([4, 4, 2 * hrx + 4, 2 * hry + 4], fill=255)
    head = head.rotate(-22, resample=Image.BICUBIC, expand=True)
    hx, hy = 424 * SS, 612 * SS
    note.paste(head, (int(hx - head.width / 2), int(hy - head.height / 2)), head)

    # Stem: rounded rectangle rising from the head's right edge.
    stem_w = 42 * SS
    stem_x = 476 * SS
    nd.rounded_rectangle(
        [stem_x, 344 * SS, stem_x + stem_w, 622 * SS],
        radius=stem_w / 2,
        fill=255,
    )

    # Flag: a crescent sweeping right off the stem's top, thick at the tip.
    outer = [(stem_x + stem_w * 0.5, 344 * SS), (612 * SS, 352 * SS), (676 * SS, 438 * SS)]
    inner = [(676 * SS, 438 * SS), (582 * SS, 404 * SS), (stem_x + stem_w * 0.5, 392 * SS)]
    pts = _bezier(outer[0], outer[1], outer[2]) + _bezier(inner[0], inner[1], inner[2])
    nd.polygon(pts, fill=255)

    # Spark on the ring's shoulder.
    spark = Image.new("L", (D, D), 0)
    sd = ImageDraw.Draw(spark)
    sparkle(sd, 704 * SS, 330 * SS, 36 * SS)

    shapes = Image.new("L", (D, D), 0)
    shapes.paste(ring, (0, 0), ring)
    shapes.paste(note, (0, 0), note)
    shapes.paste(spark, (0, 0), spark)

    mark_art = linear_gradient(
        [(0.0, (*tint_top, 255)), (1.0, (*tint_bottom, 255))],
        angle=115,
    )
    mark = Image.new("RGBA", (D, D), (0, 0, 0, 0))
    paste_through(mark, mark_art, shapes)

    glow = Image.new("RGBA", (D, D), (0, 0, 0, 0))
    glow_art = Image.new("RGBA", (D, D), (*glow_color, glow_alpha))
    paste_through(glow, glow_art, shapes)
    glow = glow.filter(ImageFilter.GaussianBlur(26 * SS))
    return glow, mark, shapes


# ---------------------------------------------------------------- the tile


PALETTES = {
    # family: (top, bottom, accent A, accent B) — richer than they look once the
    # glass sheen and the shadow settle on top of them.
    "liquid": ((14, 96, 116), (3, 26, 40), (118, 255, 233), (58, 172, 255)),
    "samsung": ((62, 38, 138), (10, 7, 34), (200, 160, 255), (98, 232, 255)),
    "pixel": ((112, 54, 14), (18, 9, 4), (255, 214, 142), (255, 118, 52)),
    "default": ((16, 92, 120), (5, 24, 38), (128, 240, 255), (150, 122, 255)),
}

DARK_BASE = ((12, 15, 20), (2, 3, 5))


def tile(kind: str, family: str, variant: str) -> Image.Image:
    """kind: 'solid' | 'clear' | 'dark' | 'clear_dark'."""
    top, bottom, accent_a, accent_b = PALETTES[family][0], PALETTES[family][1], PALETTES[family][2], PALETTES[family][3]
    clear = kind.startswith("clear")
    dark = kind.endswith("dark")
    mid = tuple(int((top[i] + bottom[i]) / 2) for i in range(3))
    if dark:
        top, mid, bottom = DARK_BASE[0], tuple(int((DARK_BASE[0][i] + DARK_BASE[1][i]) / 2) for i in range(3)), DARK_BASE[1]

    points = circle_points(158 * SS) if family == "pixel" else squircle_points(150 * SS)
    body_mask = shape_mask(points)

    canvas = new_canvas()

    # Soft drop shadow so the tile sits on any wallpaper.
    shadow = Image.new("RGBA", (D, D), (0, 0, 0, 0))
    paste_through(shadow, Image.new("RGBA", (D, D), (0, 0, 0, 150)), body_mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(26 * SS))
    canvas.alpha_composite(shadow.transform(canvas.size, Image.AFFINE, (1, 0, 0, 0, 1, 14 * SS)))

    if clear:
        # Frosted pane: a light veil so the ink mark keeps its contrast on any
        # wallpaper, with the family's colour blooming through it.
        tint = (255, 255, 255, 128) if not dark else (140, 164, 190, 84)
        body = Image.new("RGBA", (D, D), tint)
        body.alpha_composite(radial_glow((D * 0.32, D * 0.24), D * 0.60, accent_a, 80))
        body.alpha_composite(radial_glow((D * 0.74, D * 0.82), D * 0.58, accent_b, 74))
    else:
        # Dark tiles keep the accent tight to the edges so they stay black.
        bloom = 0.45 if dark else 1.0
        body = linear_gradient(
            [(0.0, (*top, 255)), (0.48, (*mid, 255)), (1.0, (*bottom, 255))],
            angle=105,
        )
        body.alpha_composite(radial_glow((D * 0.30, D * 0.16), D * 0.66, accent_a, int(130 * bloom)))
        body.alpha_composite(radial_glow((D * 0.78, D * 0.88), D * 0.62, accent_b, int(120 * bloom)))
        # Forge ember: a warm core behind the mark.
        body.alpha_composite(radial_glow((D * 0.5, D * 0.54), D * 0.40, accent_a, int(40 * bloom)))

    paste_through(canvas, body, body_mask)

    # Glass sheen across the top third.
    sheen_peak = 96 if not clear else 60
    sheen = linear_gradient(
        [
            (0.0, (255, 255, 255, sheen_peak)),
            (0.22, (255, 255, 255, int(sheen_peak * 0.35))),
            (0.55, (255, 255, 255, 0)),
        ],
        angle=95,
    )
    # Composited rather than pasted: the sheen's own alpha must add light, not
    # replace the body underneath it.
    canvas.alpha_composite(
        Image.composite(sheen, Image.new("RGBA", (D, D), (0, 0, 0, 0)), body_mask)
    )

    # Rim: bright at the top, fading to nothing at the bottom.
    rim = Image.new("RGBA", (D, D), (0, 0, 0, 0))
    rim_mask = Image.new("L", (D, D), 0)
    rm = ImageDraw.Draw(rim_mask)
    rm.line(points + [points[0]], fill=255, width=int(5 * SS), joint="curve")
    rim_art = linear_gradient(
        [(0.0, (255, 255, 255, 150)), (0.45, (255, 255, 255, 54)), (1.0, (255, 255, 255, 10))],
        angle=95,
    )
    paste_through(rim, rim_art, rim_mask)
    canvas.alpha_composite(rim)

    return canvas, body_mask


def render_variant(family: str, kind: str) -> Image.Image:
    canvas, body_mask = tile(kind, family, kind)
    dark = kind.endswith("dark") or kind == "dark"
    clear = kind.startswith("clear")
    if clear and not dark:
        tint_top, tint_bottom = (12, 26, 38), (24, 52, 70)
        glow = (90, 200, 230)
    else:
        tint_top, tint_bottom = (255, 255, 255), (186, 246, 255)
        glow = PALETTES[family][2]
    glow_img, mark, shapes = mark_layers(tint_top, tint_bottom, glow, 150 if not clear else 90)
    canvas.alpha_composite(Image.composite(glow_img, Image.new("RGBA", (D, D), (0, 0, 0, 0)), body_mask))
    canvas.alpha_composite(mark)
    return canvas


def render_background(family: str, kind: str) -> Image.Image:
    canvas, _ = tile(kind, family, kind)
    return canvas


def render_mark(tint_top, tint_bottom, glow_color, glow_alpha=150) -> Image.Image:
    glow_img, mark, _ = mark_layers(tint_top, tint_bottom, glow_color, glow_alpha)
    out = new_canvas()
    out.alpha_composite(glow_img)
    out.alpha_composite(mark)
    return out


def save(img: Image.Image, density: str, name: str, folder: str = "mipmap"):
    size = DENSITIES[density]
    target = RES / f"{folder}-{density}"
    target.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(target / f"{name}.png", optimize=True)


VARIANTS = [
    # (preview file stem, family, tile kind, background name)
    ("ic_liquid_glass", "liquid", "solid", "ic_glass_background"),
    ("ic_liquid_glass_dark", "liquid", "dark", "ic_glass_dark_background"),
    ("ic_liquid_glass_clear", "liquid", "clear", "ic_glass_clear_background"),
    ("ic_liquid_glass_clear_dark", "liquid", "clear_dark", "ic_glass_clear_dark_background"),
    ("ic_samsung_glass", "samsung", "solid", "ic_glass_samsung_background"),
    ("ic_samsung_glass_dark", "samsung", "dark", "ic_glass_samsung_dark_background"),
    ("ic_samsung_glass_clear", "samsung", "clear", "ic_glass_samsung_clear_background"),
    ("ic_samsung_glass_clear_dark", "samsung", "clear_dark", "ic_glass_samsung_clear_dark_background"),
    ("ic_pixel_glass", "pixel", "solid", "ic_glass_pixel_background"),
    ("ic_pixel_glass_dark", "pixel", "dark", "ic_glass_pixel_dark_background"),
    ("ic_pixel_glass_clear", "pixel", "clear", "ic_glass_pixel_clear_background"),
    ("ic_pixel_glass_clear_dark", "pixel", "clear_dark", "ic_glass_pixel_clear_dark_background"),
]


def main():
    # Per-variant tiles and picker previews.
    for stem, family, kind, bg_name in VARIANTS:
        bg = render_background(family, kind)
        preview = render_variant(family, kind)
        for density in DENSITIES:
            save(bg, density, bg_name)
            save(preview, density, f"{stem}_foreground")

    # Default icon (the richest tile) and the shared mark layers.
    default_bg = render_background("default", "solid")
    default_preview = render_variant("default", "solid")
    mark_white = render_mark((255, 255, 255), (186, 246, 255), (111, 231, 255), 150)
    mark_ink = render_mark((10, 24, 34), (26, 58, 78), (60, 170, 200), 90)
    mono = render_mark((255, 255, 255), (255, 255, 255), (255, 255, 255), 0)
    for density in DENSITIES:
        save(default_bg, density, "ic_launcher_background")
        save(default_preview, density, "ic_launcher_preview")
        save(mark_white, density, "ic_glass_mark")
        save(mark_ink, density, "ic_glass_mark_ink")
        save(mono, density, "ic_glass_monochrome")

    # Contact sheet for a quick visual check.
    sheet = Image.new("RGBA", (4 * 320, 3 * 320), (24, 24, 28, 255))
    for i, (stem, family, kind, _) in enumerate(VARIANTS):
        art = render_variant(family, kind).resize((300, 300), Image.LANCZOS)
        sheet.alpha_composite(art, ((i % 4) * 320 + 10, (i // 4) * 320 + 10))
    default_icon = new_canvas()
    default_icon.alpha_composite(default_bg)
    default_icon.alpha_composite(mark_white)
    sheet.alpha_composite(default_icon.resize((300, 300), Image.LANCZOS), (3 * 320 + 10, 2 * 320 + 10))
    Path(".tmp_icons").mkdir(exist_ok=True)
    sheet.save(".tmp_icons/contact-sheet.png")
    print("done; contact sheet at .tmp_icons/contact-sheet.png")


if __name__ == "__main__":
    main()
