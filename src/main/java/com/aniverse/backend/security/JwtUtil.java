package com.aniverse.backend.security;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secretKey}") // Clave secreta para firmar tokens
    private String secretKey;

    @Value("${jwt.expiration}") // Tiempo de expiración del token
    private long expirationMillis;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * Método para obtener la llave HMAC basada en la clave secreta.
     */
    private HmacKey getKey() {
        return new HmacKey(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT con los datos proporcionados.
     *
     * @param username Nombre de usuario (subjeto del token).
     * @param role     Rol del usuario.
     * @return Token firmado.
     */
    public String generateToken(String username, String role) {
        try {
            // Obtener tiempo actual en milisegundos
            long currentTimeMillis = System.currentTimeMillis();

            JwtClaims claims = new JwtClaims();
            claims.setSubject(username);

            // Establecer tiempo de emisión explícitamente
            claims.setIssuedAt(NumericDate.fromMilliseconds(currentTimeMillis));

            // Establecer tiempo de expiración basado en la configuración
            long expirationTimeMillis = currentTimeMillis + expirationMillis;
            claims.setExpirationTime(NumericDate.fromMilliseconds(expirationTimeMillis));

            // Asegurar prefijo ROLE_
            String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            claims.setClaim("role", formattedRole);

            // Establecer tiempo de no validez antes de la emisión
            claims.setNotBefore(NumericDate.fromMilliseconds(currentTimeMillis));

            // Añadir ID único al token
            claims.setJwtId(UUID.randomUUID().toString());

            // Logging para diagnóstico
            System.out.println("Token Generation Details:");
            System.out.println("Subject: " + username);
            System.out.println("Role: " + formattedRole);
            System.out.println("Issued At: " + currentTimeMillis);
            System.out.println("Expiration: " + expirationTimeMillis);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(claims.toJson());
            jws.setKey(getKey());
            jws.setAlgorithmHeaderValue("HS256");

            return jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new RuntimeException("Error al generar el token JWT", e);
        }
    }
    /**
     * Valida el token JWT verificando la firma y la expiración.
     *
     * @param token Token a validar.
     * @return `true` si el token es válido.
     */
// Método de validación más robusto
    public boolean validateToken(String token) {
        try {
            JwtConsumer consumer = new JwtConsumerBuilder()
                    .setVerificationKey(getKey())
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireExpirationTime()
                    .setRequireIssuedAt()
                    .setRequireNotBefore()
                    .build();

            JwtClaims claims = consumer.processToClaims(token);

            // Logging detallado
            System.out.println("Token Validation Details:");
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpirationTime());
            System.out.println("Role: " + claims.getClaimValue("role"));

            return true;
        } catch (Exception e) {
            System.err.println("Token Validation Error:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            return false;
        }
    }
    /**
     * Extrae los datos (claims) de un token JWT.
     *
     * @param token Token a procesar.
     * @return Claims como un mapa clave-valor.
     */
    public Map<String, Object> extractClaims(String token) {
        try {
            JwtConsumer consumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setVerificationKey(getKey())
                    .build();

            JwtClaims claims = consumer.processToClaims(token);
            return new HashMap<>(claims.getClaimsMap());
        } catch (Exception e) {
            throw new RuntimeException("Error al extraer claims del token JWT", e);
        }
    }


}
