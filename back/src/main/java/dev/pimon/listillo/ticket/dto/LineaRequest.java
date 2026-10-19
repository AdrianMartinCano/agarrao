package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Unidad;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Una línea como la deja la persona tras repasarla. Sin {@code nombre} = sin casar. */
public record LineaRequest(
        @NotBlank(message = "Cada línea necesita su texto")
        @Size(max = 120, message = "El texto de una línea no puede pasar de 120 caracteres")
        String texto,

        @NotNull(message = "Falta la cantidad")
        @DecimalMin(value = "0.001", message = "La cantidad tiene que ser mayor que cero")
        @DecimalMax(value = "9999", message = "Esa cantidad es demasiado grande")
        BigDecimal cantidad,

        @NotNull(message = "Falta la unidad")
        Unidad unidad,

        BigDecimal precioUnitario,

        @NotNull(message = "Falta el importe")
        @DecimalMin(value = "-9999.99", message = "Importe fuera de rango")
        @DecimalMax(value = "9999.99", message = "Importe fuera de rango")
        BigDecimal importe,

        @Pattern(regexp = "[0-9]{8,14}", message = "Ese código de barras no es válido")
        String ean,

        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
        String nombre
) {
}
