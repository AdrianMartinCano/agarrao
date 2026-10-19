package dev.pimon.listillo.producto;

/**
 * Códigos de barras de producto: EAN-8, UPC-A (12), EAN-13 y GTIN-14, con el dígito de
 * control comprobado. Una lectura mala de la cámara casi nunca cuadra el dígito de control,
 * así que esto también filtra lecturas falsas antes de preguntar a nadie.
 */
public final class Ean {

    private Ean() {
    }

    public static boolean esValido(String codigo) {
        if (codigo == null) {
            return false;
        }
        int largo = codigo.length();
        if (largo != 8 && largo != 12 && largo != 13 && largo != 14) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < largo - 1; i++) {
            char c = codigo.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            // Desde la derecha (sin contar el de control): pesos 3, 1, 3, 1…
            int posicionDesdeLaDerecha = largo - 1 - i;
            suma += (c - '0') * (posicionDesdeLaDerecha % 2 == 1 ? 3 : 1);
        }
        char control = codigo.charAt(largo - 1);
        return control >= '0' && control <= '9' && (10 - suma % 10) % 10 == control - '0';
    }
}
