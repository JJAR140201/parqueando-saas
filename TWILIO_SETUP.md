# Integración Twilio para Envío de SMS de Recibos de Parqueo

## Descripción
Esta integración permite enviar los recibos de parqueo por SMS a través de Twilio. El contenido del SMS incluye toda la información relevante del parqueo como placa, tipo de vehículo, horarios, duración y total pagado.

## Configuración

### 1. Obtener Credenciales de Twilio

1. Ve a [Twilio Console](https://www.twilio.com/console)
2. Inicia sesión con tu cuenta
3. En el Dashboard, encontrarás:
   - **Account SID**: Tu identificador de cuenta
   - **Auth Token**: Tu token de autenticación
   - **Phone Number**: El número de Twilio desde el cual se enviarán los SMS

### 2. Configurar Variables de Entorno

Agrega las siguientes variables en tu archivo `.env` o en el servicio de deployment:

```bash
TWILIO_ACCOUNT_SID=tu_account_sid_aqui
TWILIO_AUTH_TOKEN=tu_auth_token_aqui
TWILIO_PHONE_NUMBER=+1234567890
```

Para Railway u otro servicio de hosting:
- En la consola de Railway, ve a Variables
- Agrega las tres variables anterior con los valores correspondientes

### 3. Verificar la Aplicación

Asegúrate de que `pom.xml` incluya la dependencia de Twilio:

```xml
<!-- Twilio SDK for SMS -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.3.0</version>
</dependency>
```

## Uso de la API

### Endpoint para Enviar SMS

**POST** `/api/v1/operaciones/parqueadero/salidas/enviar-sms`

#### Cuerpo de la Solicitud:

```json
{
    "numeroTelefono": "+573001234567",
    "placa": "ABC123"
}
```

- `numeroTelefono`: Número en formato E.164 (ej: +[código país][número])
  - Ejemplos:
    - Colombia: +573001234567
    - USA: +12125551234
    - España: +34912345678

- `placa`: Placa del vehículo para el cual se busca el recibo

#### Respuesta Exitosa (200):

```json
{
    "exitoso": true,
    "mensaje": "SMS enviado exitosamente",
    "placa": "ABC123"
}
```

#### Respuesta con Error (400):

```json
{
    "exitoso": false,
    "mensaje": "Número de teléfono inválido. Debe estar en formato E.164 (ej: +573001234567)",
    "placa": "ABC123"
}
```

## Estructura del SMS

El SMS enviado tiene el siguiente formato:

```
[Nombre Empresa]
===== RECIBO DE PARQUEO =====
Placa: ABC123
Tipo: SEDAN
Entrada: 01/06/2024 10:30:45
Salida: 01/06/2024 14:45:30
Duración: 4.25 horas
=============================
TOTAL: $ 12500.00
=============================
GRACIAS POR SU VISITA
MANEJE CON PRECAUCION
```

## Notas Importantes

### Seguridad
- **NUNCA** compartas tu Account SID o Auth Token en chats, código o repositorios públicos
- Si comprometiste tus credenciales, regeneralas inmediatamente en Twilio Console
- Usa variables de entorno para almacenar las credenciales

### Límites de Twilio
- **SMS Entrante y Saliente**: Varía según tu plan
- **Costo**: Twilio cobra por SMS enviado
- **Longitud**: Los SMS se fragmentan automáticamente en múltiples SMS si exceden 160 caracteres

### Validación de Números
- El formato **E.164** es obligatorio
- Ejemplos de formato correcto: `+573001234567`, `+12025551234`
- Ejemplos de formato incorrecto: `3001234567`, `573001234567`, `+57 300 123 4567`

## Troubleshooting

### Error: "Invalid 'To' parameter"
- Verifica que el número esté en formato E.164
- Asegúrate de incluir el símbolo `+` al inicio
- Incluye el código de país correcto

### Error: "Invalid 'From' parameter"
- El número de Twilio configurado no es válido
- Verifica que sea un número Twilio válido en tu cuenta
- Debe estar en formato E.164

### Error: "Invalid credentials"
- Verifica que `TWILIO_ACCOUNT_SID` y `TWILIO_AUTH_TOKEN` sean correctos
- Consulta la consola de Twilio para obtener los valores actuales

### SMS no se envía pero no hay error
- Revisa los logs de la aplicación: `[TwilioService]`
- Verifica que tu cuenta de Twilio tenga saldo o esté en un plan activo
- Comprueba que el número 'To' esté verificado (en modo trial de Twilio)

## Integración Futura

Para integración automática al registrar salida:

```java
// En RegistroParqueoService.registrarSalida()
if (cliente.getNumeroTelefono() != null) {
    twilioService.enviarReciboPorSMS(cliente.getNumeroTelefono(), precio);
}
```

Esto requeriría agregar el campo `numeroTelefono` al modelo de `Usuario` o `Cliente`.

## Referencias
- [Documentación Oficial de Twilio Java SDK](https://www.twilio.com/docs/libraries/java)
- [Twilio SMS API](https://www.twilio.com/docs/sms)
- [Formato E.164](https://en.wikipedia.org/wiki/E.164)
