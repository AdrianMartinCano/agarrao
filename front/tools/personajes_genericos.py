"""Dibuja los retratos propios de los genéricos más comprados, con la misma cara que los demás.

Uso: python tools/personajes_genericos.py public/personajes
Los de pasillo (fruta, carne…) los dibuja personajes.py; qué genérico lleva cuál lo dice la
migración V21 del back.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from personajes import cara, svg  # noqa: E402

DESTINO = Path(sys.argv[1])
BRILLO = 'stroke="#FFFFFF" stroke-width="7" fill="none" stroke-linecap="round" opacity=".8"'

PERSONAJES = {
    # ── Fruta y verdura ──
    "platano": svg("Plátano con cara", f"""  <path d="M62 22 L66 10" stroke="#8A5A3C" stroke-width="8" stroke-linecap="round"/>
  <path d="M62 22 Q40 70 52 120 Q58 142 84 140 Q122 136 124 110 Q96 112 86 80 Q80 50 82 26 Q72 14 62 22 Z" fill="#FFD84D"/>
  <path d="M58 48 Q52 76 56 98" {BRILLO}/>
  <path d="M122 112 L130 108" stroke="#5C4030" stroke-width="6" stroke-linecap="round"/>
{cara(80, 108, "#FFB3A8", 11)}""", 40),

    "manzana": svg("Manzana con cara", f"""  <path d="M80 46 Q78 30 86 18" stroke="#8A5A3C" stroke-width="6" fill="none" stroke-linecap="round"/>
  <path d="M86 30 Q104 12 120 24 Q104 40 86 30 Z" fill="#4FD18B"/>
  <path d="M80 46 C60 30 26 40 28 82 C30 124 58 144 80 134 C102 144 130 124 132 82 C134 40 100 30 80 46 Z" fill="#FF6B6B"/>
  <path d="M42 72 Q44 58 56 52" {BRILLO}/>
{cara(80, 90, "#FFC2C2")}""", 46),

    "naranja": svg("Naranja con cara", f"""  <circle cx="80" cy="88" r="54" fill="#FFA43B"/>
  <path d="M80 36 Q96 18 114 26 Q100 42 80 36 Z" fill="#4FD18B"/>
  <circle cx="80" cy="36" r="4" fill="#8A5A3C"/>
  <path d="M44 70 Q48 56 60 50" {BRILLO}/>
  <circle cx="110" cy="62" r="2" fill="#FF8A1F"/><circle cx="118" cy="80" r="2" fill="#FF8A1F"/><circle cx="46" cy="104" r="2" fill="#FF8A1F"/>
{cara(80, 94, "#FF8FA3")}""", 50),

    "fresa": svg("Fresa con cara", f"""  <path d="M80 140 C40 130 24 92 30 66 C36 44 60 40 80 46 C100 40 124 44 130 66 C136 92 120 130 80 140 Z" fill="#FF5C6E"/>
  <path d="M52 44 L60 28 L72 40 L80 22 L88 40 L100 28 L108 44 Q80 56 52 44 Z" fill="#3FB777"/>
  <g fill="#FFE08A"><circle cx="50" cy="74" r="2.5"/><circle cx="112" cy="74" r="2.5"/><circle cx="58" cy="120" r="2.5"/><circle cx="102" cy="120" r="2.5"/><circle cx="80" cy="130" r="2.5"/><circle cx="44" cy="96" r="2.5"/><circle cx="116" cy="96" r="2.5"/></g>
{cara(80, 88, "#FFC2CC")}""", 42),

    "tomate": svg("Tomate con cara", f"""  <ellipse cx="80" cy="92" rx="60" ry="50" fill="#FF6B6B"/>
  <path d="M80 50 L66 38 L74 50 L54 48 L72 58 L80 50 L88 58 L106 48 L86 50 L94 38 Z" fill="#3FB777" stroke="#3FB777" stroke-width="4" stroke-linejoin="round"/>
  <path d="M80 48 V34" stroke="#3FB777" stroke-width="6" stroke-linecap="round"/>
  <path d="M38 80 Q42 66 56 60" {BRILLO}/>
{cara(80, 96, "#FFC2C2")}""", 54),

    "patata": svg("Patata con cara", f"""  <path d="M40 54 C58 30 112 28 128 56 C142 82 132 124 102 136 C74 146 34 136 26 108 C20 86 28 68 40 54 Z" fill="#E7B47E"/>
  <g fill="#C98B52"><circle cx="54" cy="58" r="3"/><circle cx="116" cy="70" r="3"/><circle cx="48" cy="116" r="3"/><circle cx="110" cy="122" r="3"/><circle cx="92" cy="46" r="2.5"/></g>
  <path d="M48 72 Q52 60 64 54" stroke="#F3CC94" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 90, "#FFB3A8")}""", 52),

    "zanahoria": svg("Zanahoria con cara", f"""  <path d="M70 34 Q60 14 48 12 M80 32 V8 M90 34 Q100 14 112 12" stroke="#3FB777" stroke-width="8" fill="none" stroke-linecap="round"/>
  <path d="M42 38 Q80 26 118 38 Q104 104 84 144 Q80 150 76 144 Q56 104 42 38 Z" fill="#FF9F43"/>
  <path d="M60 104 H72 M92 116 H100 M66 126 H74" stroke="#F0812A" stroke-width="4" stroke-linecap="round"/>
  <path d="M54 50 Q56 64 60 74" stroke="#FFC48A" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 66, "#FFC2C2", 12)}""", 30),

    "lechuga": svg("Lechuga con cara", f"""  <circle cx="44" cy="88" r="28" fill="#7FD6A0"/><circle cx="116" cy="88" r="28" fill="#7FD6A0"/>
  <circle cx="60" cy="56" r="28" fill="#4FD18B"/><circle cx="100" cy="56" r="28" fill="#4FD18B"/>
  <circle cx="80" cy="94" r="48" fill="#A8E8BE"/>
  <path d="M80 140 V104 M80 116 L64 100 M80 116 L96 100" stroke="#7FD6A0" stroke-width="4" fill="none" stroke-linecap="round"/>
{cara(80, 84, "#FFB3CC")}""", 52),

    "cebolla": svg("Cebolla con cara", f"""  <path d="M80 18 Q84 40 110 56 Q138 76 130 108 Q120 140 80 142 Q40 140 30 108 Q22 76 50 56 Q76 40 80 18 Z" fill="#E7B8E8"/>
  <path d="M64 56 Q48 90 60 136 M96 56 Q112 90 100 136" stroke="#D49AD8" stroke-width="4" fill="none" stroke-linecap="round"/>
  <path d="M72 144 L68 152 M80 144 V153 M88 144 L92 152" stroke="#C98B52" stroke-width="3" stroke-linecap="round"/>
  <path d="M44 84 Q46 70 56 62" {BRILLO}/>
{cara(80, 98, "#FFB3CC", 12)}""", 46),

    # ── Despensa ──
    "aceite": svg("Botella de aceite con cara", f"""  <rect x="68" y="4" width="24" height="14" rx="5" fill="#3FB777"/>
  <path d="M70 18 H90 V44 Q116 54 116 80 V130 Q116 142 104 142 H56 Q44 142 44 130 V80 Q44 54 70 44 Z" fill="#F4F8D8"/>
  <path d="M44 86 H116 V130 Q116 142 104 142 H56 Q44 142 44 130 Z" fill="#D7E05A"/>
  <path d="M44 86 Q62 80 80 86 T116 86" fill="#E4EA8A"/>
  <path d="M54 70 Q56 60 64 54" {BRILLO}/>
{cara(80, 108, "#FFB3CC", 12)}""", 40),

    "arroz": svg("Saco de arroz con cara", f"""  <path d="M58 30 Q80 20 102 30 L96 42 H64 Z" fill="#EADFCB"/>
  <path d="M60 42 H100" stroke="#C98B52" stroke-width="7" stroke-linecap="round"/>
  <path d="M62 44 Q30 60 32 104 Q34 142 80 142 Q126 142 128 104 Q130 60 98 44 Z" fill="#FBF5EA"/>
  <g fill="#FFFFFF" stroke="#E5DACB" stroke-width="2"><ellipse cx="58" cy="124" rx="5" ry="3" transform="rotate(30 58 124)"/><ellipse cx="100" cy="126" rx="5" ry="3" transform="rotate(-20 100 126)"/><ellipse cx="80" cy="132" rx="5" ry="3"/></g>
  <path d="M44 84 Q46 68 58 60" stroke="#FFFFFF" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 92, "#FFB3CC")}""", 46),

    "pasta": svg("Plato de espaguetis con cara", f"""  <!-- Los espaguetis con tomate y una albóndiga -->
  <path d="M34 74 Q36 34 80 30 Q124 34 126 74 Z" fill="#FFD27A"/>
  <path d="M44 66 Q56 46 70 62 Q80 40 92 60 Q104 44 116 66 M52 72 Q66 56 78 70 Q92 54 106 72" stroke="#F5B954" stroke-width="4" fill="none" stroke-linecap="round"/>
  <path d="M58 46 Q80 30 104 46 Q96 60 80 56 Q64 60 58 46 Z" fill="#FF6B6B"/>
  <circle cx="84" cy="40" r="10" fill="#B5603A"/>
  <path d="M100 30 L108 22 M104 34 L114 30" stroke="#3FB777" stroke-width="4" stroke-linecap="round"/>
  <!-- El plato hondo -->
  <path d="M22 76 H138 Q136 126 94 136 H66 Q24 126 22 76 Z" fill="#D9CDFF"/>
  <rect x="18" y="70" width="124" height="12" rx="6" fill="#C7B8FF"/>
  <path d="M36 92 Q38 106 46 114" stroke="#F2EDFF" stroke-width="6" fill="none" stroke-linecap="round"/>
{cara(80, 100, "#FFB3CC")}""", 50),

    "atun": svg("Lata de atún con cara", f"""  <path d="M26 76 V116 Q26 134 80 134 Q134 134 134 116 V76 Z" fill="#5CB2FF"/>
  <ellipse cx="80" cy="76" rx="54" ry="16" fill="#E3E8F2"/>
  <ellipse cx="80" cy="76" rx="44" ry="11" fill="#C9D1E2"/>
  <path d="M92 66 Q104 60 112 66" stroke="#9AA6BF" stroke-width="5" fill="none" stroke-linecap="round"/>
  <path d="M104 118 L116 110 V126 Z" fill="#8EC5FF"/>
{cara(76, 104, "#FFB3CC", 12)}""", 54),

    # ── Lácteos ──
    "yogur": svg("Yogur con cara", f"""  <path d="M40 46 H120 L110 138 Q108 144 102 144 H58 Q52 144 50 138 Z" fill="#E9E2FF"/>
  <path d="M44 84 H116 L112 112 H48 Z" fill="#FFB3CC"/>
  <path d="M34 38 H126 V50 H34 Z" fill="#B28BFF"/>
  <path d="M118 38 Q134 22 144 34 Q136 44 126 44" fill="#C7B0FF"/>
  <path d="M52 58 V74" stroke="#FFFFFF" stroke-width="6" stroke-linecap="round"/>
{cara(80, 70, "#FFB3CC", 12)}""", 40),

    "queso": svg("Cuña de queso con cara", f"""  <path d="M22 80 L130 46 L138 80 Z" fill="#FFE88A"/>
  <path d="M22 80 H138 V132 H22 Z" fill="#FFD84D"/>
  <circle cx="42" cy="118" r="7" fill="#F2C230"/><circle cx="122" cy="98" r="8" fill="#F2C230"/><circle cx="108" cy="124" r="5" fill="#F2C230"/><circle cx="100" cy="66" r="5" fill="#F5D24F"/>
{cara(76, 100, "#FFB3A8")}""", 58),

    "mantequilla": svg("Mantequilla con cara", f"""  <ellipse cx="80" cy="128" rx="66" ry="14" fill="#DFEEFF"/>
  <path d="M34 68 L58 52 H126 V112 L102 128 H34 Z" fill="#FFE9A0"/>
  <path d="M34 68 L58 52 H126 L102 68 Z" fill="#FFF3C4"/>
  <path d="M102 68 H34 V128 H102 Z" fill="#FFE07A"/>
  <path d="M102 68 L126 52 V112 L102 128 Z" fill="#F5D060"/>
{cara(68, 92, "#FFB3A8", 12)}""", 56),

    # ── Carne ──
    "pollo": svg("Pollo asado con cara", f"""  <!-- Los dos muslos hacia arriba, con su hueso -->
  <path d="M48 70 L30 34" stroke="#FFF8EC" stroke-width="9" stroke-linecap="round"/>
  <circle cx="26" cy="30" r="7" fill="#FFF8EC"/><circle cx="34" cy="26" r="6" fill="#FFF8EC"/>
  <path d="M112 70 L130 34" stroke="#FFF8EC" stroke-width="9" stroke-linecap="round"/>
  <circle cx="134" cy="30" r="7" fill="#FFF8EC"/><circle cx="126" cy="26" r="6" fill="#FFF8EC"/>
  <ellipse cx="50" cy="72" rx="18" ry="22" transform="rotate(-25 50 72)" fill="#D9853E"/>
  <ellipse cx="110" cy="72" rx="18" ry="22" transform="rotate(25 110 72)" fill="#D9853E"/>
  <!-- El cuerpo dorado, en su bandeja -->
  <ellipse cx="80" cy="136" rx="62" ry="10" fill="#DFEEFF"/>
  <path d="M26 100 C26 70 52 58 80 58 C108 58 134 70 134 100 C134 124 110 136 80 136 C50 136 26 124 26 100 Z" fill="#E59A52"/>
  <path d="M44 88 Q54 74 70 70" stroke="#F4B97E" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(80, 102, "#FFB3A8")}""", 56),

    "jamon": svg("Jamón con cara", f"""  <path d="M112 34 L128 20" stroke="#FFF8EC" stroke-width="12" stroke-linecap="round"/>
  <circle cx="132" cy="16" r="8" fill="#FFF8EC"/>
  <path d="M114 32 C144 58 124 120 90 138 C56 154 18 132 24 100 C30 70 76 50 114 32 Z" fill="#E5737A"/>
  <path d="M24 100 C18 132 56 154 90 138 C64 140 32 128 24 100 Z" fill="#FFF0E6"/>
  <path d="M48 84 Q60 70 78 64" stroke="#F2A0A5" stroke-width="7" fill="none" stroke-linecap="round"/>
{cara(74, 100, "#FFD0D6", 12)}""", 52),

    "salchicha": svg("Salchicha con cara", f"""  <rect x="18" y="66" width="124" height="52" rx="26" transform="rotate(-10 80 92)" fill="#E8925A"/>
  <path d="M18 98 L8 102 M142 82 L152 78" stroke="#C97A47" stroke-width="6" stroke-linecap="round"/>
  <path d="M38 80 Q60 70 90 68" stroke="#F4B07E" stroke-width="7" fill="none" stroke-linecap="round" transform="rotate(-10 80 92)"/>
{cara(80, 92, "#FFB3A8", 12)}""", 58),

    # ── Panadería y desayuno ──
    "pan-molde": svg("Rebanada de pan de molde con cara", f"""  <path d="M36 140 V78 Q20 74 22 54 Q26 26 80 24 Q134 26 138 54 Q140 74 124 78 V140 Z" fill="#E0A562"/>
  <path d="M46 132 V74 Q32 70 34 56 Q38 36 80 34 Q122 36 126 56 Q128 70 114 74 V132 Z" fill="#FBE3B6"/>
  <path d="M56 50 Q64 44 74 42" stroke="#FFF4DD" stroke-width="6" fill="none" stroke-linecap="round"/>
{cara(80, 88, "#FFB3A8", 12)}""", 46),

    "galleta": svg("Galleta con cara", f"""  <circle cx="80" cy="86" r="56" fill="#E0A562"/>
  <circle cx="80" cy="86" r="48" fill="#EDBB7A"/>
  <g fill="#8A5A3C"><circle cx="52" cy="62" r="5"/><circle cx="110" cy="60" r="4"/><circle cx="118" cy="104" r="5"/><circle cx="46" cy="108" r="4"/><circle cx="84" cy="128" r="4"/><circle cx="80" cy="46" r="3.5"/></g>
{cara(80, 88, "#FFB3A8")}""", 50),

    "cafe": svg("Paquete de café con cara", f"""  <path d="M44 20 H116 L112 32 H48 Z" fill="#6B4430"/>
  <path d="M48 32 H112 Q126 70 120 140 H40 Q34 70 48 32 Z" fill="#8A5A3C"/>
  <rect x="48" y="64" width="64" height="58" rx="12" fill="#F3CC94"/>
  <ellipse cx="80" cy="50" rx="8" ry="11" fill="#5C3A28" transform="rotate(30 80 50)"/>
  <path d="M77 42 Q82 50 78 58" stroke="#8A5A3C" stroke-width="2" fill="none" transform="rotate(30 80 50)"/>
{cara(80, 88, "#FFB3A8", 11)}""", 42),

    "chocolate": svg("Tableta de chocolate con cara", f"""  <rect x="34" y="20" width="92" height="124" rx="10" fill="#B57A4F"/>
  <path d="M34 50 H126 M34 80 H126 M80 20 V50" stroke="#9A6440" stroke-width="4"/>
  <path d="M30 96 L44 88 L58 98 L72 88 L86 98 L100 88 L114 98 L130 90 V138 Q130 148 120 148 H40 Q30 148 30 138 Z" fill="#B28BFF"/>
  <rect x="52" y="112" width="56" height="18" rx="9" fill="#E4DCFF"/>
{cara(80, 64, "#FFB3CC", 12)}""", 46),

    # ── Bebidas ──
    "zumo": svg("Brik de zumo de naranja con cara", f"""  <path d="M50 46 L64 18 H96 L110 46 Z" fill="#FFE2B8"/>
  <path d="M64 18 H96 V12 Q96 8 92 8 H68 Q64 8 64 12 Z" fill="#FFCB73"/>
  <rect x="44" y="44" width="72" height="98" rx="12" fill="#FFFFFF"/>
  <path d="M44 96 H116 V130 Q116 142 104 142 H56 Q44 142 44 130 Z" fill="#FFA43B"/>
  <circle cx="98" cy="118" r="10" fill="#FFC86E"/><path d="M98 108 V128 M88 118 H108" stroke="#FFA43B" stroke-width="2"/>
{cara(80, 72, "#FFB3CC")}""", 40),

    "refresco": svg("Lata de refresco con cara", f"""  <path d="M50 24 H110 L116 36 V128 L110 140 H50 L44 128 V36 Z" fill="#FF5C6E"/>
  <ellipse cx="80" cy="24" rx="30" ry="7" fill="#E3E8F2"/>
  <rect x="72" y="18" width="18" height="6" rx="3" fill="#C9D1E2"/>
  <path d="M44 96 Q80 84 116 100 V112 Q80 98 44 108 Z" fill="#FFFFFF"/>
  <path d="M54 44 V70" {BRILLO}/>
{cara(80, 66, "#FFD0D6", 12)}""", 38),

    "cerveza": svg("Jarra de cerveza con cara", f"""  <path d="M116 58 Q144 58 142 86 Q140 112 116 110" stroke="#E4F2FF" stroke-width="12" fill="none" stroke-linecap="round"/>
  <rect x="34" y="40" width="86" height="104" rx="14" fill="#FFC93C"/>
  <path d="M30 40 Q30 18 52 22 Q60 8 78 14 Q94 4 106 18 Q126 16 124 40 Q124 52 110 50 H44 Q30 52 30 40 Z" fill="#FFFFFF"/>
  <path d="M46 66 V124 M108 66 V124" stroke="#FFE08A" stroke-width="6" stroke-linecap="round"/>
  <circle cx="64" cy="128" r="3" fill="#FFF3C4"/><circle cx="90" cy="118" r="2.5" fill="#FFF3C4"/>
{cara(77, 88, "#FFB3A8", 12)}""", 46),

    "vino": svg("Botella de vino con cara", f"""  <rect x="70" y="4" width="20" height="16" rx="4" fill="#B28BFF"/>
  <path d="M70 20 H90 V50 Q114 60 114 86 V134 Q114 144 104 144 H56 Q46 144 46 134 V86 Q46 60 70 50 Z" fill="#8E3B5C"/>
  <rect x="50" y="86" width="60" height="44" rx="8" fill="#FFF3C4"/>
  <path d="M56 70 Q60 60 68 56" stroke="#B85C80" stroke-width="6" fill="none" stroke-linecap="round"/>
{cara(80, 104, "#FFB3CC", 11)}""", 36),

    # ── Congelados ──
    "pizza": svg("Porción de pizza con cara", f"""  <path d="M80 148 L22 40 Q80 16 138 40 Z" fill="#FFD84D"/>
  <path d="M18 38 Q80 8 142 38 Q144 52 132 52 Q80 30 28 52 Q16 52 18 38 Z" fill="#E0A562"/>
  <circle cx="58" cy="66" r="9" fill="#FF6B6B"/><circle cx="104" cy="64" r="9" fill="#FF6B6B"/><circle cx="82" cy="118" r="7" fill="#FF6B6B"/>
  <path d="M70 128 Q74 138 70 146" stroke="#FFD84D" stroke-width="5" stroke-linecap="round"/>
{cara(80, 84, "#FFB3A8", 11)}""", 30),

    "helado": svg("Cucurucho de helado con cara", f"""  <path d="M46 84 H114 L84 148 Q80 154 76 148 Z" fill="#E7B47E"/>
  <path d="M56 96 L92 124 M70 88 L100 108 M104 96 L68 124 M90 88 L58 108" stroke="#C98B52" stroke-width="3"/>
  <circle cx="80" cy="58" r="40" fill="#FFB3CC"/>
  <path d="M40 66 Q50 92 62 82 Q70 96 82 86 Q94 98 104 84 Q116 94 120 66 Z" fill="#FFB3CC"/>
  <circle cx="80" cy="18" r="8" fill="#FF5C6E"/>
  <path d="M54 44 Q58 32 68 28" {BRILLO}/>
{cara(80, 60, "#FF8DBE", 12)}""", 30),

    # ── Limpieza ──
    "detergente": svg("Botella de detergente con cara", f"""  <rect x="40" y="12" width="40" height="20" rx="6" fill="#4FD18B"/>
  <path d="M36 32 H124 Q134 32 134 42 V132 Q134 144 122 144 H38 Q26 144 26 132 V42 Q26 32 36 32 Z" fill="#5CB2FF"/>
  <path d="M96 40 H118 Q124 40 124 46 V70 Q124 76 118 76 H96 Z" fill="#8EC5FF"/>
  <rect x="38" y="92" width="84" height="38" rx="12" fill="#FFFFFF"/>
  <circle cx="58" cy="111" r="6" fill="#C4E1FF"/><circle cx="74" cy="104" r="4" fill="#C4E1FF"/>
  <path d="M38 50 V74" stroke="#C4E1FF" stroke-width="7" stroke-linecap="round"/>
{cara(72, 64, "#FFB3CC", 12)}""", 54),
}

for nombre, contenido in PERSONAJES.items():
    (DESTINO / f"{nombre}.svg").write_text(contenido, encoding="utf8", newline="\n")
print(len(PERSONAJES), "personajes")
