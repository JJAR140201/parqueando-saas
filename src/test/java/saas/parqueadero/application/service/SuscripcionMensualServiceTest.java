package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@ExtendWith(MockitoExtension.class)
class SuscripcionMensualServiceTest {

    @Mock SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;
    @Mock AuthenticatedUserProviderPort authenticatedUserProviderPort;
    @Mock SedeRepositoryPort sedeRepositoryPort;

    SuscripcionMensualService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SuscripcionMensualService(suscripcionMensualRepositoryPort, authenticatedUserProviderPort, sedeRepositoryPort);
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
}
