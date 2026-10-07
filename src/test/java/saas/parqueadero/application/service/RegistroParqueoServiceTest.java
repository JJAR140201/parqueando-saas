package saas.parqueadero.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import saas.parqueadero.application.dto.RegistrarEntradaRequest;
import saas.parqueadero.application.dto.RegistrarSalidaRequest;
import saas.parqueadero.application.dto.RegistroParqueoResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.CapacityUnavailableException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.domain.model.TipoVehiculo;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;
import saas.parqueadero.domain.port.out.TarifaRepositoryPort;

class RegistroParqueoServiceTest {

    private static final long EMPRESA = 7L;
    private static final long SEDE = 70L;

    private final SedeRepositoryPort sedeRepository = mock(SedeRepositoryPort.class);
    private final EmpresaRepositoryPort empresaRepository = mock(EmpresaRepositoryPort.class);
    private final TarifaRepositoryPort tarifaRepository = mock(TarifaRepositoryPort.class);
    private final RegistroParqueoRepositoryPort registroRepository = mock(RegistroParqueoRepositoryPort.class);
    private final AuthenticatedUserProviderPort userProvider = mock(AuthenticatedUserProviderPort.class);
    private final SuscripcionMensualRepositoryPort suscripcionRepository = mock(SuscripcionMensualRepositoryPort.class);

    private RegistroParqueoService service;
    private Sede sede;

    @BeforeEach
    void setUp() {
        service = new RegistroParqueoService(sedeRepository, empresaRepository, tarifaRepository,
            registroRepository, userProvider, suscripcionRepository);
        when(userProvider.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).empresaId(EMPRESA).sedeId(SEDE).roles(List.of("OPERARIO")).build());
        sede = Sede.builder().id(SEDE).empresaId(EMPRESA).capacidadTotal(10).capacidadActual(5).build();
        when(sedeRepository.findByIdAndEmpresaIdForUpdate(SEDE, EMPRESA)).thenReturn(Optional.of(sede));
        when(registroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private RegistrarEntradaRequest entrada(String placa) {
        RegistrarEntradaRequest request = new RegistrarEntradaRequest();
        request.setPlaca(placa);
        request.setTipoVehiculo(TipoVehiculo.CARRO);
        return request;
    }

    @Test
    void entradaBloqueaLaSedeYDescuentaCupo() {
        when(registroRepository.findActivoByPlacaAndSedeIdAndEmpresaId(" abc123 ", SEDE, EMPRESA))
            .thenReturn(Optional.empty());

        RegistroParqueoResponse response = service.registrarEntrada(entrada(" abc123 "));

        assertEquals("ABC123", response.getPlaca());
        assertEquals(4, sede.getCapacidadActual());
        verify(sedeRepository).findByIdAndEmpresaIdForUpdate(SEDE, EMPRESA);
    }

    @Test
    void entradaDuplicadaEnLaMismaSedeSeRechazaSinTocarCupo() {
        when(registroRepository.findActivoByPlacaAndSedeIdAndEmpresaId("ABC123", SEDE, EMPRESA))
            .thenReturn(Optional.of(RegistroParqueo.builder().placa("ABC123").build()));

        assertThrows(BusinessException.class, () -> service.registrarEntrada(entrada("ABC123")));
        assertEquals(5, sede.getCapacidadActual());
        verify(registroRepository, never()).save(any());
    }

    @Test
    void entradaSinCupoSeRechaza() {
        sede.setCapacidadActual(0);
        assertThrows(CapacityUnavailableException.class, () -> service.registrarEntrada(entrada("XYZ987")));
    }

    @Test
    void salidaCobraPorFraccionYLiberaCupo() {
        RegistroParqueo activo = RegistroParqueo.builder()
            .placa("ABC123").tipoVehiculo(TipoVehiculo.CARRO).estado(EstadoRegistroParqueo.ACTIVO)
            .fechaEntrada(LocalDateTime.now().minusMinutes(120)).sedeId(SEDE).empresaId(EMPRESA).build();
        when(registroRepository.findActivoByPlacaAndSedeIdAndEmpresaId("ABC123", SEDE, EMPRESA))
            .thenReturn(Optional.of(activo));
        when(suscripcionRepository.findVigenteByPlacaAndSedeIdAndEmpresaId(any(), any(), any(), any()))
            .thenReturn(Optional.empty());
        when(tarifaRepository.findBySedeIdAndEmpresaIdAndTipoVehiculo(SEDE, EMPRESA, TipoVehiculo.CARRO))
            .thenReturn(Optional.of(Tarifa.builder()
                .valorFraccion(new BigDecimal("1000")).minutosFraccion(60).build()));

        RegistrarSalidaRequest request = new RegistrarSalidaRequest();
        request.setPlaca("ABC123");
        RegistroParqueoResponse response = service.registrarSalida(request);

        assertEquals(EstadoRegistroParqueo.FINALIZADO, response.getEstado());
        assertEquals(0, new BigDecimal("2000").compareTo(response.getTotalPagado()));
        assertEquals(6, sede.getCapacidadActual());
    }
}
