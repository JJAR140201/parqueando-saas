package saas.parqueadero.infrastructure.configuration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import saas.parqueadero.domain.model.AuthenticatedUser;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.isValid(token)) {
                String username = jwtTokenProvider.getUsername(token);
                Long usuarioId = jwtTokenProvider.getUsuarioId(token);
                Long empresaId = jwtTokenProvider.getEmpresaId(token);
                Long sedeId = jwtTokenProvider.getSedeId(token);
                var authorities = jwtTokenProvider.getRoles(token).stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

                AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                    .username(username)
                    .usuarioId(usuarioId)
                    .empresaId(empresaId)
                    .sedeId(sedeId)
                    .roles(jwtTokenProvider.getRoles(token))
                    .build();

                var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
