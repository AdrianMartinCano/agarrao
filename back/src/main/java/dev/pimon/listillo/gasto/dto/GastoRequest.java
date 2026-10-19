package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.gasto.TipoGasto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Anotar o cambiar un gasto. {@code cadaMes}: además, que se apunte solo cada mes ese mismo
 * día (hasta el 28). Al cambiar uno ya anotado no cuenta.
 */
public record GastoRequest(
        @NotBlank(message = "Escribe en qué ha sido")
        @Size(max = 80, message = "No puede pasar de 80 caracteres")
        String concepto,

        @NotNull(message = "Falta cuánto ha sido")
        @DecimalMin(value = "0.01", message = "El importe tiene que ser mayor que 0")
        @DecimalMax(value = "99999.99", message = "Ese importe es demasiado grande")
        @Digits(integer = 5, fraction = 2, message = "Como mucho, dos decimales")
        BigDecimal importe,

        @NotNull(message = "Falta la fecha")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @NotNull(message = "Elige de qué tipo es")
        TipoGasto tipo,

        boolean cadaMes
) {
}
