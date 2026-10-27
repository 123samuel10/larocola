#!/usr/bin/env python3
"""
Genera el código QR de La Rocola para un establecimiento.
Uso: python generar_qr.py <url> [nombre_salida]
Ejemplo: python generar_qr.py "https://miusuario.github.io/e/la-esquina" la-esquina
Requiere: pip install qrcode[pil] Pillow
"""
import sys
import qrcode
import qrcode.image.svg

def generar_qr(url: str, nombre: str = "qr-larocola") -> None:
    # PNG
    qr = qrcode.QRCode(
        error_correction=qrcode.constants.ERROR_CORRECT_H,
        box_size=12,
        border=4,
    )
    qr.add_data(url)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    png_path = f"{nombre}.png"
    img.save(png_path)
    print(f"PNG generado: {png_path} ({img.size[0]}x{img.size[1]}px)")

    # SVG
    factory = qrcode.image.svg.SvgImage
    qr_svg = qrcode.QRCode(
        error_correction=qrcode.constants.ERROR_CORRECT_H,
        image_factory=factory,
        box_size=12,
        border=4,
    )
    qr_svg.add_data(url)
    qr_svg.make(fit=True)
    svg_img = qr_svg.make_image()
    svg_path = f"{nombre}.svg"
    svg_img.save(svg_path)
    print(f"SVG generado: {svg_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    url_arg = sys.argv[1]
    nombre_arg = sys.argv[2] if len(sys.argv) > 2 else "qr-larocola"
    generar_qr(url_arg, nombre_arg)
