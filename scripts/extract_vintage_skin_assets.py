"""Extract the component background samples from the approved design reference."""

from pathlib import Path
import sys

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps


CROPS = {
    "home-banner.png": ((22, 890, 265, 979), (750, 360)),
    "action-card.png": ((112, 254, 296, 322), (335, 180)),
    "section-banner.png": ((112, 329, 296, 390), (686, 144)),
    "product-card.png": ((282, 890, 494, 980), (686, 320)),
    "spec-panel.png": ((511, 890, 682, 980), (686, 720)),
    "empty-cart.png": ((696, 890, 886, 980), (686, 560)),
    "cart-panel.png": ((897, 890, 1152, 958), (686, 180)),
    "checkout-bar.png": ((34, 1013, 270, 1068), (750, 124)),
    "member-card.png": ((282, 1011, 571, 1075), (686, 224)),
    # Use the text-free ornamental card sample. Tab labels and icons remain real UI nodes.
    "tab-bar.png": ((282, 890, 494, 980), (750, 112)),
}


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: extract_vintage_skin_assets.py SOURCE OUTPUT_DIR")
    source = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    output_dir.mkdir(parents=True, exist_ok=True)

    with Image.open(source) as image:
        image = image.convert("RGB")
        for filename, (box, size) in CROPS.items():
            sample = image.crop(box)
            sample = ImageEnhance.Contrast(sample).enhance(1.03)
            sample = ImageOps.fit(sample, size, method=Image.Resampling.LANCZOS)
            sample = sample.filter(ImageFilter.UnsharpMask(radius=1.2, percent=65, threshold=3))
            if filename == "cart-panel.png":
                clean_texture = sample.crop((430, 25, 540, 155)).resize(
                    (320, 130), Image.Resampling.BICUBIC
                )
                cleaned = sample.copy()
                cleaned.paste(clean_texture, (115, 25))
                mask = Image.new("L", sample.size, 0)
                ImageDraw.Draw(mask).rectangle((128, 30, 425, 150), fill=255)
                mask = mask.filter(ImageFilter.GaussianBlur(radius=7))
                sample = Image.composite(cleaned, sample, mask)
            if filename == "checkout-bar.png":
                # The reference contains example CTA copy. Clone a clean paper area
                # into the button interior so runtime text remains a real UI node.
                clean_texture = sample.crop((220, 24, 440, 100))
                cleaned = sample.copy()
                cleaned.paste(clean_texture, (490, 24))
                mask = Image.new("L", sample.size, 0)
                ImageDraw.Draw(mask).rectangle((505, 26, 700, 98), fill=255)
                mask = mask.filter(ImageFilter.GaussianBlur(radius=7))
                sample = Image.composite(cleaned, sample, mask)
                wash = Image.new("RGBA", sample.size, (0, 0, 0, 0))
                wash_draw = ImageDraw.Draw(wash)
                for x in range(350):
                    alpha = int(190 * (1 - x / 350))
                    wash_draw.line((x, 0, x, sample.height), fill=(255, 247, 231, alpha))
                sample = Image.alpha_composite(sample.convert("RGBA"), wash).convert("RGB")
            sample.save(output_dir / filename, format="PNG", optimize=True)


if __name__ == "__main__":
    main()
