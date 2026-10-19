package dev.pimon.listillo.tienda;

import java.time.Duration;

/** Propiedades de los lectores web para los tests: sin pausas y contra localhost. */
final class PropiedadesDePrueba {

    private PropiedadesDePrueba() {
    }

    static PreciosWebProperties props() {
        return new PreciosWebProperties(true, "-", Duration.ZERO, Duration.ofSeconds(1),
                new PreciosWebProperties.Mercadona("http://localhost", "mad1"),
                new PreciosWebProperties.Lidl("http://localhost"),
                new PreciosWebProperties.Consum("http://localhost"),
                new PreciosWebProperties.Ahorramas("http://localhost"),
                new PreciosWebProperties.Eroski("http://localhost"),
                new PreciosWebProperties.Aldi("http://localhost"),
                new PreciosWebProperties.Caprabo("http://localhost"));
    }
}
