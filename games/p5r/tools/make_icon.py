# Create the P5RP launcher icon from the supplied full-colour artwork:
# python games/p5r/tools/make_icon.py <picture>
# Writes games/p5r/icon/icon.png (1024 px) and Android density/adaptive resources.
import os
import sys

from PIL import Image, ImageOps

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "icon")


def fit_artwork(artwork, size, width_share=1.0):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    width = max(1, round(size * width_share))
    height = max(1, round(size * width_share))
    image = ImageOps.fit(artwork, (width, height), method=Image.Resampling.LANCZOS,
                         centering=(0.5, 0.5))
    canvas.alpha_composite(image, ((size - width) // 2, (size - height) // 2))
    return canvas


def main():
    artwork = Image.open(sys.argv[1]).convert("RGBA")
    os.makedirs(OUT, exist_ok=True)
    icon = fit_artwork(artwork, 1024)
    icon.save(os.path.join(OUT, "icon.png"))

    res = os.path.join(OUT, "res")
    for folder, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        directory = os.path.join(res, "mipmap-" + folder)
        os.makedirs(directory, exist_ok=True)
        fit_artwork(artwork, px).save(os.path.join(directory, "ic_launcher.png"))
        adaptive_size = round(px * 108 / 48)
        fit_artwork(artwork, adaptive_size, 0.88).save(
            os.path.join(directory, "ic_launcher_layer.png"))

    directory = os.path.join(res, "mipmap-anydpi-v26")
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, "ic_launcher.xml"), "w", encoding="utf-8") as icon_xml:
        icon_xml.write('<?xml version="1.0" encoding="utf-8"?>\n'
                       '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                       '    <background android:drawable="@android:color/black" />\n'
                       '    <foreground android:drawable="@mipmap/ic_launcher_layer" />\n'
                       '</adaptive-icon>\n')
    print("P5RP icon: %s, launcher resources: %s" % (os.path.join(OUT, "icon.png"), res))


if __name__ == "__main__":
    main()
