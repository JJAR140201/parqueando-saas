package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;

class WhatsappMensajeFormatterTest {

    private final WhatsappMensajeFormatter formatter = new WhatsappMensajeFormatter();

    private final Cliente cliente = Cliente.builder().id(1L).nombre("Juan").build();
    private final Empresa empresa = Empresa.builder().id(5L).nombre("Parqueadero Central").build();
    private final SuscripcionMensual suscripcion = SuscripcionMensual.builder()
        .id(9L).valorMensual(new BigDecimal("120000.00"))
        .fechaInicio(LocalDate.of(2026, 10, 1)).fechaFin(LocalDate.of(2026, 10, 10)).build();

    @Test
    void mensualidadGeneradaUsaPeriodoValorYFechaDeVencimiento() {
        List<String> variables = formatter.variables(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, cliente, suscripcion,
            empresa, LocalDate.of(2026, 10, 3));

        assertThat(variables).containsExactly("Juan", "octubre de 2026", "$120.000", "10 de octubre de 2026", "Parqueadero Central");
    }

    @Test
    void confirmacionDePagoUsaLaFechaDelPago() {
        List<String> variables = formatter.variables(TipoNotificacionWhatsapp.CONFIRMACION_PAGO, cliente, suscripcion,
            empresa, LocalDate.of(2026, 10, 7));

        assertThat(variables.get(3)).isEqualTo("7 de octubre de 2026");
    }

    @Test
    void periodoQueCruzaDeMesSeDescribeConRango() {
        assertThat(WhatsappMensajeFormatter.periodo(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 14)))
            .isEqualTo("del 15 de octubre al 14 de noviembre de 2026");
    }

    @Test
    void formateaMontosConSeparadorDeMiles() {
        assertThat(WhatsappMensajeFormatter.valor(new BigDecimal("1500000"))).isEqualTo("$1.500.000");
        assertThat(WhatsappMensajeFormatter.valor(new BigDecimal("950"))).isEqualTo("$950");
    }

    @Test
    void limpiaSaltosDeLineaYEspaciosQueMetaRechaza() {
        assertThat(WhatsappMensajeFormatter.limpiar("  Juan\n\tPerez     Lopez  ")).isEqualTo("Juan Perez Lopez");
        assertThat(WhatsappMensajeFormatter.limpiar("   ")).isEqualTo("-");
        assertThat(WhatsappMensajeFormatter.limpiar(null)).isEqualTo("-");
        assertThat(WhatsappMensajeFormatter.limpiar("x".repeat(500))).hasSize(120);
    }
}
