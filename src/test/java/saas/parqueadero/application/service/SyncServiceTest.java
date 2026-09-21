package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.parqueadero.application.dto.SnapshotUploadRequest;
import saas.parqueadero.domain.exception.UnauthorizedException;
import saas.parqueadero.domain.model.ClienteInstalacion;
import saas.parqueadero.domain.model.SnapshotOperativo;
import saas.parqueadero.domain.port.out.ClienteInstalacionRepositoryPort;
import saas.parqueadero.domain.port.out.SnapshotOperativoRepositoryPort;
import saas.parqueadero.infrastructure.configuration.sync.SyncProperties;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock ClienteInstalacionRepositoryPort clienteRepository;
    @Mock SnapshotOperativoRepositoryPort snapshotRepository;
    final ObjectMapper objectMapper = new ObjectMapper();

    SyncService service;

    private static final String TENANT = "tenant-1";
    private static final String TOKEN = "token-secreto";

    @BeforeEach
    void setUp() {
        service = new SyncService(clienteRepository, snapshotRepository, objectMapper,
            new SyncProperties("", 90, 600L));
    }

    private ClienteInstalacion clienteActivo() {
        return ClienteInstalacion.builder()
            .id(1L).tenantId(TENANT).nombre("Parqueadero X")
            .tokenHash(SyncToken.hash(TOKEN)).activa(true)
            .creadaEn(Instant.now())
            .build();
    }

    private SnapshotUploadRequest request() throws Exception {
        return SnapshotUploadRequest.builder()
            .generadoEn(Instant.now())
            .appVersion("1.1.3")
            .licencia(new SnapshotUploadRequest.Licencia("ACTIVA", LocalDate.now().plusMonths(6)))
            .resumen(new SnapshotUploadRequest.Resumen(4, 12, 8, new BigDecimal("53000")))
            .datos(objectMapper.readTree("{\"registros\":[{\"placa\":\"ABC123\"}]}"))
            .build();
    }

    @Test
    void rechazaTenantDesconocido() throws Exception {
        when(clienteRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recibirSnapshot(TENANT, TOKEN, request()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rechazaInstalacionDesactivada() throws Exception {
        ClienteInstalacion c = clienteActivo();
        c.setActiva(false);
        when(clienteRepository.findByTenantId(TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.recibirSnapshot(TENANT, TOKEN, request()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rechazaTokenInvalido() throws Exception {
        when(clienteRepository.findByTenantId(TENANT)).thenReturn(Optional.of(clienteActivo()));
        assertThatThrownBy(() -> service.recibirSnapshot(TENANT, "otro-token", request()))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void guardaSnapshotYActualizaCliente() throws Exception {
        when(clienteRepository.findByTenantId(TENANT)).thenReturn(Optional.of(clienteActivo()));

        var response = service.recibirSnapshot(TENANT, TOKEN, request());

        assertThat(response.isRecibido()).isTrue();
        assertThat(response.getProximaSyncSugeridaSegundos()).isEqualTo(600L);

        ArgumentCaptor<SnapshotOperativo> snap = ArgumentCaptor.forClass(SnapshotOperativo.class);
        verify(snapshotRepository).save(snap.capture());
        assertThat(snap.getValue().getPayloadJson()).contains("ABC123");
        assertThat(snap.getValue().getResumen().getVehiculosDentro()).isEqualTo(4);

        ArgumentCaptor<ClienteInstalacion> cli = ArgumentCaptor.forClass(ClienteInstalacion.class);
        verify(clienteRepository).save(cli.capture());
        assertThat(cli.getValue().getUltimaSyncEn()).isNotNull();
        assertThat(cli.getValue().getEstadoLicencia()).isEqualTo("ACTIVA");
        assertThat(cli.getValue().getAppVersion()).isEqualTo("1.1.3");
    }
}
