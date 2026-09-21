package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.parqueadero.application.dto.ResumenDiaResponse;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.domain.model.TipoVehiculo;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ReporteParqueoServiceTest {

    @Mock RegistroParqueoRepositoryPort registroParqueoRepositoryPort;
    @Mock AuthenticatedUserProviderPort authenticatedUserProviderPort;
    @Mock SedeRepositoryPort sedeRepositoryPort;
    @Mock UsuarioRepositoryPort usuarioRepositoryPort;

    ReporteParqueoService service;

    @BeforeEach
    void setUp() {
        service = new ReporteParqueoService(registroParqueoRepositoryPort, authenticatedUserProviderPort,
            sedeRepositoryPort, usuarioRepositoryPort);
    }

    private void conRol(String rol, Long empresaId, Long sedeId) {
        when(authenticatedUserProviderPort.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).username("op").roles(List.of(rol)).empresaId(empresaId).sedeId(sedeId).build());
    }

    @Test
    void calculaDentroEntradasYSalidasPorTipo() {
        conRol("ADMIN", 5L, 50L);

        LocalDateTime hoy8am = java.time.LocalDate.now().atTime(8, 0);
        LocalDateTime hoy9am = java.time.LocalDate.now().atTime(9, 0);
        LocalDateTime ayer = java.time.LocalDate.now().minusDays(1).atTime(10, 0);

        List<RegistroParqueo> registros = List.of(
            // sigue activo, entro hoy -> cuenta en dentro Y en entradasHoy
            RegistroParqueo.builder().id(1L).tipoVehiculo(TipoVehiculo.CARRO).estado(EstadoRegistroParqueo.ACTIVO)
                .fechaEntrada(hoy8am).empresaId(5L).sedeId(50L).build(),
            // sigue activo, entro ayer -> cuenta solo en dentro
            RegistroParqueo.builder().id(2L).tipoVehiculo(TipoVehiculo.MOTO).estado(EstadoRegistroParqueo.ACTIVO)
                .fechaEntrada(ayer).empresaId(5L).sedeId(50L).build(),
            // finalizo hoy (entro ayer, salio hoy) -> cuenta en salidasHoy, no en dentro ni entradasHoy
            RegistroParqueo.builder().id(3L).tipoVehiculo(TipoVehiculo.CARRO).estado(EstadoRegistroParqueo.FINALIZADO)
                .fechaEntrada(ayer).fechaSalida(hoy9am).empresaId(5L).sedeId(50L).build(),
            // entro y salio hoy -> cuenta en entradasHoy Y salidasHoy
            RegistroParqueo.builder().id(4L).tipoVehiculo(TipoVehiculo.MOTO).estado(EstadoRegistroParqueo.FINALIZADO)
                .fechaEntrada(hoy8am).fechaSalida(hoy9am).empresaId(5L).sedeId(50L).build()
        );

        when(registroParqueoRepositoryPort.findActividadDelDia(anyLong(), anyLong(), any(), any())).thenReturn(registros);

        ResumenDiaResponse resumen = service.getResumenDia(null, null);

        assertThat(resumen.getDentro().getCarros()).isEqualTo(1);
        assertThat(resumen.getDentro().getMotos()).isEqualTo(1);
        assertThat(resumen.getDentro().getTotal()).isEqualTo(2);

        assertThat(resumen.getEntradasHoy().getCarros()).isEqualTo(1);
        assertThat(resumen.getEntradasHoy().getMotos()).isEqualTo(1);
        assertThat(resumen.getEntradasHoy().getTotal()).isEqualTo(2);

        assertThat(resumen.getSalidasHoy().getCarros()).isEqualTo(1);
        assertThat(resumen.getSalidasHoy().getMotos()).isEqualTo(1);
        assertThat(resumen.getSalidasHoy().getTotal()).isEqualTo(2);
    }

    @Test
    void adminNoPuedeConsultarOtraEmpresa() {
        conRol("ADMIN", 5L, 50L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getResumenDia(99L, null))
            .isInstanceOf(saas.parqueadero.domain.exception.BusinessException.class);
    }
}
