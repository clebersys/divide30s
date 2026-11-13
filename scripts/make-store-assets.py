#!/usr/bin/env python3
"""Gera os recursos gráficos da Play Store (ícone 512 e feature graphic).

Uso: python3 scripts/make-store-assets.py
Requer Pillow.
"""
from __future__ import annotations

import os
from glob import glob

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "store")

BG_TOP = (11, 18, 32)
BG_BOTTOM = (16, 27, 46)
SURFACE = (22, 33, 58)
ACCENT = (56, 189, 248)
TEXT_PRIMARY = (232, 238, 248)
TEXT_MUTED = (148, 163, 184)
DARK = (15, 23, 42)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def vertical_gradient(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", (1, height))
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / max(1, height - 1)
        color = tuple(
            round(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * t) for i in range(3)
        )
        draw.point((0, y), fill=color)
    return image.resize((width, height))


def draw_logo(draw: ImageDraw.ImageDraw, x: float, y: float, size: float) -> None:
    """Desenha o símbolo do app (mesmo desenho do adaptive icon)."""
    scale = size / 108.0

    def pt(px: float, py: float) -> tuple[float, float]:
        return (x + px * scale, y + py * scale)

    draw.rounded_rectangle(
        [pt(28, 30), pt(80, 78)],
        radius=4 * scale,
        fill=ACCENT,
    )
    draw.polygon([pt(44, 46), pt(62, 54), pt(44, 62)], fill=DARK)
    bar_w = 3 * scale
    for bar_x in (26.5, 79.5):
        top_left = pt(bar_x, 40)
        draw.rectangle(
            [top_left, (top_left[0] + bar_w, top_left[1] + 28 * scale)],
            fill=TEXT_PRIMARY,
        )


def build_icon() -> None:
    size = 512
    # 32 bits (RGBA) com alfa totalmente opaco: exigência da Play Store.
    image = Image.new("RGBA", (size, size), (*DARK, 255))
    draw = ImageDraw.Draw(image)
    # O ícone da Play Store não tem cantos arredondados: o launcher aplica a máscara.
    # O símbolo ocupa ~72% da largura, como em ícones de loja bem resolvidos.
    unit = (0.72 * size) / 52.0          # 52 unidades = largura do retângulo do logo
    logo_size = unit * 108.0
    offset = size / 2.0 - 54.0 * unit    # (54, 54) é o centro do logo no viewport
    draw_logo(draw, offset, offset, logo_size)
    image.save(os.path.join(STORE, "icon-512.png"), "PNG", optimize=True)
    print("store/icon-512.png", image.size, image.mode)


def normalize_screenshots() -> None:
    """Screenshots precisam ser PNG 24 bits (sem canal alfa)."""
    for path in sorted(glob(os.path.join(STORE, "screenshot-*.png"))):
        with Image.open(path) as image:
            if image.mode != "RGB":
                image.convert("RGB").save(path, "PNG", optimize=True)
                print("normalizado (24 bits):", os.path.basename(path))


def build_feature_graphic() -> None:
    width, height = 1024, 500
    image = vertical_gradient((width, height))
    draw = ImageDraw.Draw(image)

    # brilho suave atrás do logo
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse([-40, 40, 460, 540], fill=(*ACCENT, 26))
    image = Image.alpha_composite(image.convert("RGBA"), glow).convert("RGB")
    draw = ImageDraw.Draw(image)

    draw_logo(draw, 96, 118, 264)

    title_font = ImageFont.truetype(FONT_BOLD, 84)
    subtitle_font = ImageFont.truetype(FONT_REGULAR, 34)
    badge_font = ImageFont.truetype(FONT_BOLD, 26)

    title = "Divide 30s"
    draw.text((432, 140), title, font=title_font, fill=TEXT_PRIMARY)

    lines = ["Vídeos em partes de 30s", "sem perder qualidade"]
    y = 250
    for line in lines:
        draw.text((434, y), line, font=subtitle_font, fill=TEXT_MUTED)
        y += 48

    badge = "SEM REENCODE"
    text_box = draw.textbbox((0, 0), badge, font=badge_font)
    badge_w = text_box[2] - text_box[0]
    badge_h = text_box[3] - text_box[1]
    pad_x, pad_y = 18, 10
    bx, by = 434, 372
    draw.rounded_rectangle(
        [bx, by, bx + badge_w + pad_x * 2, by + badge_h + pad_y * 2],
        radius=14,
        fill=SURFACE,
        outline=ACCENT,
        width=2,
    )
    draw.text(
        (bx + pad_x, by + pad_y - text_box[1] // 2),
        badge,
        font=badge_font,
        fill=ACCENT,
    )

    image.save(os.path.join(STORE, "feature-graphic-1024x500.png"), "PNG", optimize=True)
    print("store/feature-graphic-1024x500.png", image.size)


def main() -> None:
    os.makedirs(STORE, exist_ok=True)
    build_icon()
    build_feature_graphic()
    normalize_screenshots()


if __name__ == "__main__":
    main()
