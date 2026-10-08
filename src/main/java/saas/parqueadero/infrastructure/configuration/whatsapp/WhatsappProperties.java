package saas.parqueadero.infrastructure.configuration.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion global de la integracion con WhatsApp Cloud API (Meta). Las credenciales de cada
 * empresa (WABA, numero, token) viven en la tabla {@code whatsapp_account}, no aqui.
 *
 * @param graphBaseUrl         URL base de Graph API.
 * @param apiVersion           version de Graph API (por ejemplo v23.0).
 * @param templateLanguage     codigo de idioma con el que se aprobaron las plantillas en Meta.
 * @param appSecret            secreto de la app de Meta; firma los webhooks (X-Hub-Signature-256).
 * @param webhookVerifyToken   token que se configura en Meta para verificar la URL del webhook.
 * @param encryptionKey        clave AES-256 en Base64 para cifrar los tokens de acceso guardados.
 * @param connectTimeoutMillis tiempo maximo para conectar con Meta.
 * @param readTimeoutMillis    tiempo maximo de espera de la respuesta de Meta.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsappProperties(
    String graphBaseUrl,
    String apiVersion,
    String templateLanguage,
    String appSecret,
    String webhookVerifyToken,
    String encryptionKey,
    Integer connectTimeoutMillis,
    Integer readTimeoutMillis
) {

    public WhatsappProperties {
        if (graphBaseUrl == null || graphBaseUrl.isBlank()) {
            graphBaseUrl = "https://graph.facebook.com";
        }
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = "v23.0";
        }
        if (templateLanguage == null || templateLanguage.isBlank()) {
            templateLanguage = "es";
        }
        if (connectTimeoutMillis == null || connectTimeoutMillis < 1) {
            connectTimeoutMillis = 5000;
        }
        if (readTimeoutMillis == null || readTimeoutMillis < 1) {
            readTimeoutMillis = 10000;
        }
    }

    public boolean hasAppSecret() {
        return appSecret != null && !appSecret.isBlank();
    }

    public boolean hasWebhookVerifyToken() {
        return webhookVerifyToken != null && !webhookVerifyToken.isBlank();
    }
}
