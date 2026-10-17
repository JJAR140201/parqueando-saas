package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.parqueadero.application.dto.SuscripcionMensualResponse;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.MensualidadEventosPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@ExtendWith(MockitoExtension.class)
class SuscripcionMensualServiceTest {

    @Mock SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;
    @Mock AuthenticatedUserProviderPort authenticatedUserProviderPort;
    @Mock SedeRepositoryPort sedeRepositoryPort;
    @Mock ClienteService clienteService;
    @Mock MensualidadEventosPort eventosPort;

    SuscripcionMensualService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SuscripcionMensualService(suscripcionMensualRepositoryPort, authenticatedUserProviderPort, sedeRepositoryPort,
            clienteService, eventosPort);
        Field diasAnticipacion = SuscripcionMensualService.class.getDeclaredField("diasAnticipacion");
        diasAnticipacion.setAccessible(true);
        diasAnticipacion.set(service, 5);
    }

    private void conRol(String rol, Long empresaId, Long sedeId) {
        when(authenticatedUserProviderPort.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).username("admin").roles(List.of(rol)).empresaId(empresaId).sedeId(sedeId).build());
    }

    @Test
    void soloIncluyeActivasQueVencenDentroDeLaVentana() {
        conRol("ADMIN", 5L, 50L);

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusMonths(1);
        List<SuscripcionMensual> todas = List.of(
            // vence en 3 dias, activa -> deberia aparecer
            SuscripcionMensual.builder().id(1L).placa("AAA111").activa(true).fechaInicio(inicio).fechaFin(hoy.plusDays(3)).empresaId(5L).sedeId(50L).build(),
            // vence en 10 dias (fuera de la ventana de 5) -> no deberia aparecer
            SuscripcionMensual.builder().id(2L).placa("BBB222").activa(true).fechaInicio(inicio).fechaFin(hoy.plusDays(10)).empresaId(5L).sedeId(50L).build(),
            // ya vencida -> no deberia aparecer (eso lo maneja la cancelacion automatica)
            SuscripcionMensual.builder().id(3L).placa("CCC333").activa(true).fechaInicio(inicio).fechaFin(hoy.minusDays(1)).empresaId(5L).sedeId(50L).build(),
            // vence en 2 dias pero inactiva (cancelada) -> no deberia aparecer
            SuscripcionMensual.builder().id(4L).placa("DDD444").activa(false).fechaInicio(inicio).fechaFin(hoy.plusDays(2)).empresaId(5L).sedeId(50L).build()
        );

        when(sedeRepositoryPort.findByIdAndEmpresaId(50L, 5L)).thenReturn(java.util.Optional.of(Sede.builder().id(50L).empresaId(5L).nombre("Principal").build()));
        when(suscripcionMensualRepositoryPort.findByEmpresaIdAndSedeId(5L, 50L)).thenReturn(todas);

        List<SuscripcionMensualResponse> resultado = service.listProximasAVencer(null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPlaca()).isEqualTo("AAA111");
    }

    // ---------------------------------------------------------------- notificaciones por WhatsApp

    private void preparaGuardado() {
        when(sedeRepositoryPort.findByIdAndEmpresaId(50L, 5L))
            .thenReturn(java.util.Optional.of(Sede.builder().id(50L).empresaId(5L).nombre("Principal").build()));
        when(clienteService.resolverOCrear(any(), any(), any(), any(), any()))
            .thenReturn(Cliente.builder().id(3L).empresaId(5L).nombre("Juan").whatsappHabilitado(true).build());
        when(suscripcionMensualRepositoryPort.save(any())).thenAnswer(inv -> {
            SuscripcionMensual s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(9L);
            }
            return s;
        });
    }

    private SuscripcionMensual existente(LocalDate fin, boolean activa) {
        return SuscripcionMensual.builder().id(9L).placa("ABC123").activa(activa).alertaVencimientoEnviada(false)
            .tipoVehiculo(saas.parqueadero.domain.model.TipoVehiculo.CARRO).valorMensual(new java.math.BigDecimal("100000"))
            .fechaInicio(LocalDate.now().minusDays(20)).fechaFin(fin).telefono("+573001234567").empresaId(5L).sedeId(50L).build();
    }

    private saas.parqueadero.application.dto.UpdateSuscripcionMensualRequest actualizacion(LocalDate fin, Boolean activa) {
        return saas.parqueadero.application.dto.UpdateSuscripcionMensualRequest.builder()
            .tipoVehiculo(saas.parqueadero.domain.model.TipoVehiculo.CARRO).valorMensual(new java.math.BigDecimal("100000"))
            .fechaInicio(LocalDate.now().minusDays(20)).fechaFin(fin).activa(activa).telefono("+573001234567").build();
    }

    @Test
    void crearUnaMensualidadPublicaElEventoDeMensualidadGeneradaYEnlazaAlCliente() {
        conRol("ADMIN", 5L, 50L);
        preparaGuardado();

        SuscripcionMensualResponse respuesta = service.createSuscripcion(
            saas.parqueadero.application.dto.CreateSuscripcionMensualRequest.builder()
                .placa("abc123").tipoVehiculo(saas.parqueadero.domain.model.TipoVehiculo.CARRO)
                .valorMensual(new java.math.BigDecimal("120000")).fechaInicio(LocalDate.now()).fechaFin(LocalDate.now().plusDays(30))
                .telefono("+573001234567").nombreCliente("Juan").whatsappHabilitado(true).build());

        verify(clienteService).resolverOCrear(5L, "+573001234567", "Juan", true, "Cliente ABC123");
        verify(eventosPort).publicar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);
        assertThat(respuesta.getClienteId()).isEqualTo(3L);
    }

    @Test
    void extenderLaFechaDeFinEsUnaRenovacionYPublicaLaConfirmacionDePago() {
        conRol("ADMIN", 5L, 50L);
        preparaGuardado();
        LocalDate fin = LocalDate.now().plusDays(3);
        when(suscripcionMensualRepositoryPort.findById(9L)).thenReturn(java.util.Optional.of(existente(fin, true)));

        service.updateSuscripcion(9L, actualizacion(fin.plusDays(30), true));

        verify(eventosPort).publicar(TipoNotificacionWhatsapp.CONFIRMACION_PAGO, 9L);
    }

    @Test
    void reactivarUnaMensualidadInactivaTambienEsUnPago() {
        conRol("ADMIN", 5L, 50L);
        preparaGuardado();
        LocalDate fin = LocalDate.now().plusDays(10);
        when(suscripcionMensualRepositoryPort.findById(9L)).thenReturn(java.util.Optional.of(existente(fin, false)));

        service.updateSuscripcion(9L, actualizacion(fin, true));

        verify(eventosPort).publicar(TipoNotificacionWhatsapp.CONFIRMACION_PAGO, 9L);
    }

    @Test
    void editarSinRenovarNoNotifica() {
        conRol("ADMIN", 5L, 50L);
        preparaGuardado();
        LocalDate fin = LocalDate.now().plusDays(10);
        when(suscripcionMensualRepositoryPort.findById(9L)).thenReturn(java.util.Optional.of(existente(fin, true)));

        service.updateSuscripcion(9L, actualizacion(fin, true));
        service.updateSuscripcion(9L, actualizacion(fin.minusDays(2), true));

        verify(eventosPort, never()).publicar(any(), anyLong());
    }

    @Test
    void cancelarOExtenderUnaMensualidadInactivaNoNotifica() {
        conRol("ADMIN", 5L, 50L);
        preparaGuardado();
        LocalDate fin = LocalDate.now().plusDays(10);
        when(suscripcionMensualRepositoryPort.findById(9L)).thenReturn(java.util.Optional.of(existente(fin, true)));

        service.updateSuscripcion(9L, actualizacion(fin.plusDays(30), false));

        verify(eventosPort, never()).publicar(any(), anyLong());
    }
}
