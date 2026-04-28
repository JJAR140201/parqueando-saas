package saas.parqueadero.infrastructure.configuration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Usuario;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String getUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long getUsuarioId(String token) {
        return getLongClaim(token, "usuarioId");
    }

    public Long getEmpresaId(String token) {
        return getLongClaim(token, "empresaId");
    }

    public Long getSedeId(String token) {
        return getLongClaim(token, "sedeId");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = extractClaims(token).get("roles");
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String generateToken(Usuario usuario) {
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();
        claims.put("usuarioId", usuario.getId());
        claims.put("roles", List.of(usuario.getRol().name()));
        if (usuario.getEmpresaId() != null) {
            claims.put("empresaId", usuario.getEmpresaId());
        }
        if (usuario.getSedeId() != null) {
            claims.put("sedeId", usuario.getSedeId());
        }

        return Jwts.builder()
            .subject(usuario.getUsername())
            .claims(claims)
            .issuedAt(now)
            .expiration(buildExpirationDate())
            .signWith(resolveSigningKey())
            .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(resolveSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey resolveSigningKey() {
        String secret = jwtProperties.secret();
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException ex) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Long getLongClaim(String token, String claim) {
        Object value = extractClaims(token).get(claim);
        if (value instanceof Integer integer) {
            return integer.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        return null;
    }

    public Date buildExpirationDate() {
        long expiration = jwtProperties.expirationMillis() != null ? jwtProperties.expirationMillis() : 86400000L;
        return new Date(System.currentTimeMillis() + expiration);
    }
}
