"""Dibuja los retratos de categoría de Listillo con la misma cara que los personajes a mano."""
import sys
from pathlib import Path

TINTA = "#2A2F5A"


def cara(cx, cy, rubor="#FFB3CC", sep=14):
    return f"""  <!-- Cara -->
  <ellipse cx="{cx - sep}" cy="{cy}" rx="4.5" ry="5.5" fill="{TINTA}"/>
  <ellipse cx="{cx + sep}" cy="{cy}" rx="4.5" ry="5.5" fill="{TINTA}"/>
  <circle cx="{cx - sep + 1.5}" cy="{cy - 2}" r="1.6" fill="#FFFFFF"/>
  <circle cx="{cx + sep + 1.5}" cy="{cy - 2}" r="1.6" fill="#FFFFFF"/>
  <ellipse cx="{cx - sep - 11}" cy="{cy + 12}" rx="7" ry="4.5" fill="{rubor}"/>
  <ellipse cx="{cx + sep + 11}" cy="{cy + 12}" rx="7" ry="4.5" fill="{rubor}"/>
  <path d="M{cx - 7} {cy + 11} Q{cx} {cy + 18} {cx + 7} {cy + 11}" stroke="{TINTA}" stroke-width="4" fill="none" stroke-linecap="round"/>
"""


def svg(etiqueta, cuerpo, sombra=40):
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 160" role="img" aria-label="{etiqueta}">
  <ellipse cx="80" cy="148" rx="{sombra}" ry="7" fill="{TINTA}" opacity=".12"/>
{cuerpo}</svg>
"""


PERSONAJES = {
    # El pasillo, no un producto: un frutero con varias frutas y la cara en el cuenco.
    "fruta": svg("Frutero con cara", f"""  <!-- Las frutas asomando: uvas, plátano, naranja y manzana -->
  <circle cx="50" cy="66" r="9" fill="#B28BFF"/><circle cx="60" cy="56" r="9" fill="#9E73F2"/>
  <circle cx="44" cy="54" r="8" fill="#9E73F2"/><circle cx="54" cy="44" r="8" fill="#B28BFF"/>
  <path d="M60 64 Q78 18 120 34 Q96 40 84 66 Z" fill="#FFD84D"/>
  <path d="M118 30 L126 26" stroke="#8A5A3C" stroke-width="5" stroke-linecap="round"/>
  <circle cx="110" cy="66" r="20" fill="#FFA43B"/>
  <path d="M100 58 Q102 52 108 50" stroke="#FFC98A" stroke-width="5" fill="none" stroke-linecap="round"/>
  <circle cx="78" cy="68" r="18" fill="#FF6B6B"/>
  <path d="M78 50 Q80 42 86 40" stroke="#8A5A3C" stroke-width="4" fill="none" stroke-linecap="round"/>
  <!-- El cuenco -->
  <path d="M24 78 H136 Q134 128 92 138 H68 Q26 128 24 78 Z" fill="#FFE1EC"/>
  <rect x="20" y="72" width="120" height="12" rx="6" fill="#FFCFE0"/>
  <path d="M38 92 Q40 106 48 114" stroke="#FFFFFF" stroke-width="6" fill="none" stroke-linecap="round"/>
  <rect x="62" y="136" width="36" height="8" rx="4" fill="#FFCFE0"/>
{cara(80, 98, "#FF9FBF")}""", 46),

    # El pasillo, no un producto: una cesta con verduras y la cara en la cesta.
    "verdura": svg("Cesta de verduras con cara", f"""  <!-- Las verduras asomando: puerro, zanahoria, tomate y lechuga -->
  <path d="M46 70 L40 22" stroke="#7FD6A0" stroke-width="12" stroke-linecap="round"/>
  <path d="M40 22 L32 8 M40 22 L46 8" stroke="#3FB777" stroke-width="6" stroke-linecap="round"/>
  <path d="M76 70 L92 24" stroke="#FF9F43" stroke-width="14" stroke-linecap="round"/>
  <path d="M92 24 L88 10 M92 24 L102 14" stroke="#3FB777" stroke-width="6" stroke-linecap="round"/>
  <circle cx="118" cy="62" r="18" fill="#FF6B6B"/>
  <path d="M112 46 L118 50 L124 46" stroke="#3FB777" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="62" cy="64" r="16" fill="#4FD18B"/><circle cx="52" cy="70" r="12" fill="#7FD6A0"/>
  <!-- El asa y la cesta de mimbre -->
  <path d="M30 78 Q80 10 130 78" stroke="#C98B52" stroke-width="7" fill="none" stroke-linecap="round"/>
  <path d="M22 76 H138 L128 132 Q126 140 118 140 H42 Q34 140 32 132 Z" fill="#E7B47E"/>
  <rect x="18" y="70" width="124" height="14" rx="7" fill="#D9A36A"/>
  <path d="M36 130 H124" stroke="#D9A36A" stroke-width="4" stroke-linecap="round"/>
{cara(80, 100, "#FFB3A8")}""", 50),

    # Hogar y bazar: un cubo de fregar con su fregona y la cara en el cubo.
    "hogar": svg("Cubo con fregona y cara", f"""  <!-- El palo y las tiras de la fregona -->
  <path d="M104 16 L92 82" stroke="#C98B52" stroke-width="8" stroke-linecap="round"/>
  <path d="M74 80 Q90 70 110 80 L112 100 Q92 106 72 100 Z" fill="#F2EDFF"/>
  <path d="M78 98 L74 112 M86 100 L84 114 M94 100 L94 115 M102 100 L104 113" stroke="#D9CDFF" stroke-width="5" stroke-linecap="round"/>
  <!-- El cubo con su asa y el escurridor -->
  <path d="M38 64 Q80 30 122 64" stroke="#2A2F5A" stroke-width="4" fill="none" stroke-linecap="round" opacity=".55"/>
  <path d="M30 70 H130 L120 136 Q118 144 110 144 H50 Q42 144 40 136 Z" fill="#5CB2FF"/>
  <rect x="26" y="64" width="108" height="14" rx="7" fill="#8EC5FF"/>
  <path d="M44 90 Q46 82 54 80" stroke="#C4E1FF" stroke-width="6" fill="none" stroke-linecap="round"/>
{cara(80, 102, "#FFB3CC")}""", 46),

    "carne": svg("Muslito de pollo con cara", f"""  <!-- El hueso -->
  <rect x="96" y="96" width="34" height="14" rx="7" transform="rotate(40 113 103)" fill="#FFF8EC"/>
  <circle cx="128" cy="128" r="10" fill="#FFF8EC"/>
  <circle cx="136" cy="116" r="10" fill="#FFF8EC"/>
  <!-- La carne -->
  <path d="M38 70 C38 38 70 22 98 34 C126 46 124 86 106 104 C90 120 60 124 46 108 C38 98 38 84 38 70 Z" fill="#E8925A"/>
  <path d="M50 58 Q56 44 70 40" stroke="#F4B07E" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 72, "#FFB3A8")}"""),

    "pescado": svg("Pez con cara", f"""  <!-- La cola -->
  <path d="M114 80 L146 54 Q150 80 146 106 Z" fill="#5CB2FF"/>
  <!-- El cuerpo -->
  <ellipse cx="74" cy="80" rx="56" ry="40" fill="#8EC5FF"/>
  <path d="M74 40 Q90 26 104 34 Q96 44 92 46 Z" fill="#5CB2FF"/>
  <path d="M30 88 Q74 112 118 88 Q100 118 74 120 Q46 118 30 88 Z" fill="#DFEEFF"/>
  <path d="M38 68 Q42 54 56 48" stroke="#C4E1FF" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(66, 76, "#FFB3CC")}""", 46),

    "pan": svg("Barra de pan con cara", f"""  <!-- La barra, tumbada y un poco inclinada -->
  <g transform="rotate(-14 80 88)">
    <rect x="18" y="58" width="124" height="58" rx="29" fill="#E0A562"/>
    <path d="M42 64 Q50 74 46 86 M70 60 Q78 70 74 82 M98 60 Q106 70 102 82 M124 64 Q130 72 128 82" stroke="#F3CC94" stroke-width="6" fill="none" stroke-linecap="round"/>
  </g>
{cara(80, 98, "#FFB3A8")}""", 54),

    "despensa": svg("Bote de garbanzos con cara", f"""  <!-- La tapa -->
  <rect x="44" y="16" width="72" height="20" rx="8" fill="#FFB23E"/>
  <rect x="44" y="16" width="72" height="8" rx="4" fill="#FFCB73"/>
  <!-- El tarro de cristal, con garbanzos dentro -->
  <path d="M48 38 H112 Q124 38 124 52 V130 Q124 142 112 142 H48 Q36 142 36 130 V52 Q36 38 48 38 Z" fill="#E4F2FF"/>
  <circle cx="54" cy="120" r="9" fill="#F2D29B"/><circle cx="72" cy="126" r="9" fill="#EBC584"/>
  <circle cx="90" cy="122" r="9" fill="#F2D29B"/><circle cx="108" cy="126" r="9" fill="#EBC584"/>
  <circle cx="62" cy="106" r="9" fill="#EBC584"/><circle cx="82" cy="108" r="9" fill="#F2D29B"/>
  <circle cx="102" cy="108" r="9" fill="#EBC584"/>
  <path d="M46 56 Q46 48 54 46" stroke="#FFFFFF" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 66, "#FFB3CC")}"""),

    "desayuno": svg("Taza de cacao con cara", f"""  <!-- El vapor -->
  <path d="M66 30 Q58 20 66 10 M88 30 Q80 20 88 10" stroke="#D9CDFF" stroke-width="6" fill="none" stroke-linecap="round"/>
  <!-- El asa -->
  <path d="M118 66 Q146 66 144 92 Q142 116 116 114" stroke="#FF6BA7" stroke-width="12" fill="none" stroke-linecap="round"/>
  <!-- La taza -->
  <path d="M30 42 H126 V108 Q126 142 92 142 H64 Q30 142 30 108 Z" fill="#FF8DBE"/>
  <ellipse cx="78" cy="44" rx="48" ry="9" fill="#8A5A3C"/>
  <path d="M40 60 V78" stroke="#FFC2DC" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(78, 84, "#FFE1EC")}""", 46),

    "bebidas": svg("Botella de agua con cara", f"""  <!-- El tapón -->
  <rect x="64" y="6" width="32" height="16" rx="6" fill="#5CB2FF"/>
  <!-- La botella -->
  <path d="M68 22 H92 V34 Q118 44 118 70 V128 Q118 142 104 142 H56 Q42 142 42 128 V70 Q42 44 68 34 Z" fill="#DFEEFF"/>
  <rect x="42" y="92" width="76" height="30" fill="#8EC5FF"/>
  <path d="M42 92 Q60 86 80 92 T118 92" fill="#B9DBFF"/>
  <path d="M52 66 Q52 54 62 48" stroke="#FFFFFF" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 66, "#FFB3CC", 13)}""", 38),

    "congelados": svg("Cubito de hielo con cara", f"""  <!-- El cubito, con su cara de arriba -->
  <path d="M40 54 L80 34 L120 54 L80 74 Z" fill="#F2F9FF"/>
  <path d="M40 54 L80 74 V140 L40 120 Z" fill="#CBE6FF"/>
  <path d="M120 54 L80 74 V140 L120 120 Z" fill="#B0D8FF"/>
  <path d="M48 70 V92" stroke="#FFFFFF" stroke-width="6" fill="none" stroke-linecap="round"/>
  <!-- Escarcha -->
  <path d="M132 30 V46 M124 38 H140 M28 92 V104 M22 98 H34" stroke="#8EC5FF" stroke-width="4" stroke-linecap="round"/>
{cara(80, 96, "#FFB3CC", 11)}""", 42),

    "limpieza": svg("Botella de lejía con cara", f"""  <!-- El tapón -->
  <rect x="58" y="10" width="30" height="18" rx="6" fill="#B28BFF"/>
  <!-- La botella, con su asa hueca -->
  <path d="M60 28 H88 V36 Q124 40 124 74 V130 Q124 142 112 142 H48 Q36 142 36 130 V64 Q36 40 60 36 Z" fill="#F2EDFF"/>
  <path d="M96 48 Q112 50 112 66 Q112 78 100 78" stroke="#D9CDFF" stroke-width="8" fill="none" stroke-linecap="round"/>
  <rect x="44" y="96" width="72" height="30" rx="10" fill="#B28BFF"/>
  <path d="M80 100 Q88 110 88 115 A8 8 0 0 1 72 115 Q72 110 80 100 Z" fill="#F2EDFF"/>
  <path d="M46 62 Q48 50 58 46" stroke="#FFFFFF" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(72, 70, "#FFCFE0", 13)}"""),

    "higiene": svg("Rollo de papel higiénico con cara", f"""  <!-- El rollo -->
  <rect x="34" y="34" width="92" height="104" rx="18" fill="#FFFFFF"/>
  <ellipse cx="80" cy="36" rx="46" ry="14" fill="#F4F0FF"/>
  <ellipse cx="80" cy="36" rx="14" ry="5" fill="#D9CDFF"/>
  <!-- El papel que cuelga -->
  <path d="M126 70 H140 V128 Q140 134 134 132 L126 128 Z" fill="#FFFFFF"/>
  <path d="M130 84 H138 M130 100 H138 M130 116 H138" stroke="#E4DCFF" stroke-width="3" stroke-linecap="round"/>
  <path d="M34 112 H126" stroke="#E4DCFF" stroke-width="4"/>
  <path d="M44 62 V82" stroke="#F4F0FF" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 78, "#FFB3CC")}"""),
}

if __name__ == "__main__":
    DESTINO = Path(sys.argv[1])
    for nombre, contenido in PERSONAJES.items():
        (DESTINO / f"{nombre}.svg").write_text(contenido, encoding="utf8", newline="\n")
    print(len(PERSONAJES), "personajes")
