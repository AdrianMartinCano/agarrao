package dev.pimon.listillo.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cierra {@code POST /api/auth/register}, que trae spring-users sin interruptor para
 * apagarlo. En Listillo se entra solo con Google: sin este filtro,
 * cualquiera podría crearse una cuenta con contraseña llamando al API a mano.
 *
 * Responde 404, como si no existiera. {@code ListilloLocal} lo abre para poder crear
 * usuarios de prueba.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistroCerrado extends OncePerRequestFilter {

    static final String RUTA = "/api/auth/register";

    private final boolean abierto;

    public RegistroCerrado(@Value("${listillo.registro-con-contrasena:false}") boolean abierto) {
        this.abierto = abierto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return abierto || !RUTA.equals(peticion.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        respuesta.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
