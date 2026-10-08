# Notificaciones por WhatsApp (Meta Cloud API)

Parqueando envia mensajes de **plantilla** por la Cloud API de WhatsApp: mensualidad generada,
recordatorio de vencimiento, confirmacion de pago y mensualidad vencida. Cada empresa usa **su propia**
cuenta de WhatsApp Business; una empresa nunca usa las credenciales de otra.

```
Mensualidad (crear / renovar / vencer / recordatorio)
   -> evento tras confirmar la transaccion  (un fallo de WhatsApp no la afecta)
   -> WhatsappNotificationService  (consentimiento, preferencias, telefono, cuenta de la empresa)
   -> WhatsappGateway -> MetaWhatsappAdapter -> Graph API
   -> bitacora whatsapp_message (REQUESTED -> ACCEPTED)
Meta -> POST /api/webhooks/meta/whatsapp (firmado) -> SENT / DELIVERED / READ / FAILED
```

## 1. Variables de entorno del backend

| Variable | Obligatoria | Descripcion |
|---|---|---|
| `WHATSAPP_ENCRYPTION_KEY` | para guardar tokens | Clave AES-256 en Base64 (32 bytes): `openssl rand -base64 32`. **No la pierdas ni la cambies**: con otra clave los tokens guardados no se pueden descifrar y hay que volver a cargarlos |
| `WHATSAPP_APP_SECRET` | para el webhook | "App Secret" de la app de Meta. Con el se valida la firma `X-Hub-Signature-256`. Sin el, **todos** los webhooks se rechazan |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | para el webhook | Texto cualquiera que tu eliges; se escribe igual en Meta al configurar el webhook |
| `WHATSAPP_API_VERSION` | no | Version de Graph API (por defecto `v23.0`). Revisa en Meta cual esta vigente |
| `WHATSAPP_TEMPLATE_LANGUAGE` | no | Idioma con el que aprobaste las plantillas (por defecto `es`; si usas espanol de Colombia, `es_CO`) |
| `WHATSAPP_RECORDATORIO_CRON` | no | Hora del recordatorio diario (por defecto `0 0 9 * * *`, 9:00 hora de Colombia) |
| `WHATSAPP_CONNECT_TIMEOUT_MS` / `WHATSAPP_READ_TIMEOUT_MS` | no | Tiempos maximos con Meta (5 s / 10 s) |

El token de acceso de cada empresa **no** va en variables: se registra por la API o la pantalla y se guarda cifrado.

## 2. Configurar Meta

1. En [Meta for Developers](https://developers.facebook.com) crea (o usa) una app de tipo *Business* con el producto **WhatsApp**.
2. Agrega y verifica el numero de WhatsApp Business (piloto: `+57 302 608 8215`). Anota el **Phone Number ID** y el **WABA ID**.
3. Crea un **token permanente**: Business Settings -> Users -> System users -> genera un token con los permisos `whatsapp_business_messaging` y `whatsapp_business_management`. Los tokens temporales de 24 h solo sirven para probar.
4. **Webhook**: en WhatsApp -> Configuration, URL de devolucion de llamada `https://<tu-backend>/api/webhooks/meta/whatsapp`, token de verificacion = `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, y suscribete al campo **`messages`**. Un solo webhook sirve a todas las empresas: el `phone_number_id` del evento identifica a cual pertenece.
5. Copia el **App Secret** (Settings -> Basic) a `WHATSAPP_APP_SECRET`.

## 3. Plantillas

Crealas en WhatsApp Manager -> Message templates, categoria **Utility**, idioma Espanol, con **estos nombres exactos**.
Los cinco valores son siempre `{{1}}` cliente, `{{2}}` periodo, `{{3}}` valor, `{{4}}` fecha y `{{5}}` parqueadero
(en `payment_confirmation` la fecha es la del pago; en las demas, la de vencimiento).

| Nombre | Cuerpo |
|---|---|
| `monthly_invoice_created` | `Hola {{1}}. Se ha generado tu mensualidad correspondiente a {{2}}. Valor: {{3}}. Fecha limite de pago: {{4}}. Atentamente, {{5}}.` |
| `monthly_payment_reminder` | `Hola {{1}}. Te recordamos que tu mensualidad de {{2}} por {{3}} vence el {{4}}. Atentamente, {{5}}.` |
| `payment_confirmation` | `Hola {{1}}. Hemos recibido correctamente tu pago. Periodo: {{2}}. Valor: {{3}}. Fecha del pago: {{4}}. Estado: PAGADO. Gracias por utilizar {{5}}.` |
| `monthly_payment_overdue` | `Hola {{1}}. Tu mensualidad de {{2}} por {{3}} vencio el {{4}} y tiene un pago pendiente. Atentamente, {{5}}.` |

Valores de ejemplo para la revision de Meta: `{{1}}` Juan Perez, `{{2}}` octubre de 2026, `{{3}}` $120.000, `{{4}}` 10 de octubre de 2026, `{{5}}` Parqueadero Central.

Los mensajes son **solo informativos**: no incluyas promociones ni ofertas, o Meta puede recategorizar la plantilla como Marketing.

## 4. Configurar una empresa

Desde la pantalla **WhatsApp** (ADMIN de la empresa o SUPER_ADMIN), o por la API:

```bash
curl -X PUT "$API/api/v1/whatsapp/cuenta" -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{
  "wabaId": "123456789", "businessId": "987654321", "phoneNumberId": "109876543210",
  "displayPhoneNumber": "+57 302 608 8215", "accessToken": "EAAG...", "enabled": true }'
# SUPER_ADMIN: agregar ?empresaId=<id>

# Mensaje de prueba (usa una plantilla con datos de ejemplo)
curl -X POST "$API/api/v1/whatsapp/mensajes/prueba" -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" -d '{"telefono":"+573026088215"}'
```

El token nunca vuelve a mostrarse: la API solo informa `tokenConfigurado: true`. Si dejas `accessToken` vacio al
actualizar, se conserva el anterior. Estado de la cuenta: `PENDIENTE` hasta el primer envio exitoso, `ACTIVA`,
y `ERROR` si Meta rechaza el token (codigo 190).

## 5. Reglas de envio

- **Consentimiento**: un cliente solo recibe WhatsApp si `whatsappHabilitado = true`. Los clientes que ya existian
  al activar la funcion quedaron en `false`; habilitalos solo si tienes su autorizacion (WhatsApp lo exige).
- **Preferencias por tipo**: generada -> *facturacion*; recordatorio y vencida -> *mensualidades*; pago -> *confirmaciones de pago*.
- **Telefono**: formato internacional E.164 (`+573001234567`). Si no es valido, no se intenta y queda `NOT_SENT / TELEFONO_INVALIDO`.
- **Renovar = pagar**: extender la fecha de fin de una mensualidad (o reactivar una inactiva) envia la confirmacion de pago.
- **Recordatorio**: una vez por mensualidad, `app.mensualidad.alerta.dias-anticipacion` dias antes del vencimiento. Si la empresa
  aun no tiene WhatsApp configurado, no se marca como enviado y se enviara cuando lo configure.
- **Vencida**: se envia al cancelarse automaticamente una mensualidad vencida.
- Un fallo de WhatsApp (Meta caido, token vencido, telefono invalido) **nunca** afecta a la mensualidad ni al pago.

## 6. Bitacora, estados y latencias

`GET /api/v1/whatsapp/mensajes` (paginada, total en `X-Total-Count`) y `GET /api/v1/whatsapp/metricas`.

| Estado | Significado |
|---|---|
| `REQUESTED` | Solicitado; aun no responde Meta |
| `ACCEPTED` | Meta acepto el mensaje (tiene `providerMessageId`) |
| `SENT` / `DELIVERED` / `READ` | Informados por el webhook |
| `FAILED` | Meta lo rechazo o no pudo entregarlo (`errorCode`, `errorMessage`) |
| `NOT_SENT` | No se intento: sin consentimiento, preferencia desactivada o telefono invalido |

Latencias (ms): `latenciaApi = accepted - requested`, `latenciaEnvio = sent - requested`, `latenciaEntrega = delivered - requested`.

## 7. Limitaciones y notas operativas

- Una cuenta de WhatsApp por empresa y un `phone_number_id` solo puede pertenecer a una empresa.
- Los envios salen por una cola en memoria (2-4 hilos). Con mas de una instancia del backend hay que revisar el
  recordatorio diario (se ejecutaria en cada instancia) y mover la cola a un broker.
- Para **apagar WhatsApp** de una empresa basta con deshabilitar su cuenta (`enabled: false`); el resto del sistema no cambia.
- Para **cambiar de proveedor** se implementa otro `WhatsappGateway` (hoy `MetaWhatsappAdapter`) sin tocar la logica de mensualidades.
