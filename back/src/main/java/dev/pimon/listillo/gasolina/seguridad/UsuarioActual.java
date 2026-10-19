package dev.pimon.listillo.gasolina.seguridad;

import dev.pimon.security.model.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Lee el usuario autenticado SIN exigir que lo haya.
 *
 * La anotación {@code @CurrentUser} de la librería lanza un 401 cuando no hay sesión,
 * que es justo lo que se quiere en los favoritos. Pero el buscador es público y a la vez
 * quiere reconocer al usuario si viene identificado, para poner sus favoritas primero.
 * Con {@code @CurrentUser}, buscar sin haber entrado devolvería 401.
 *
 * Esto funciona porque el filtro JWT de la librería se ejecuta en TODAS las peticiones,
 * también en las rutas públicas: si llega un token válido, deja el usuario puesto en el
 * contexto aunque la ruta no exija autenticación.
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** El usuario de la petición, o vacío si es anónimo o el token no vale. */
    public static Optional<AuthUser> opcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Para un anónimo, Spring Security deja un AnonymousAuthenticationToken cuyo
        // principal es la cadena "anonymousUser": de ahí la comprobación de tipo.
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthUser usuario)) {
            return Optional.empty();
        }
        return Optional.of(usuario);
    }
}
