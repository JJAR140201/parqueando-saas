package saas.parqueadero.domain.port.out;

/** Cifrado reversible de secretos (tokens de proveedores). */
public interface TokenCipherPort {

    /** @throws saas.parqueadero.domain.exception.BusinessException si el cifrado no esta configurado */
    String cifrar(String textoPlano);

    String descifrar(String textoCifrado);

    boolean estaConfigurado();
}
