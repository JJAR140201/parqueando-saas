package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.parqueadero.application.dto.RegistrarClienteRequest;
import saas.parqueadero.application.dto.RegistrarClienteResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.ClienteInstalacion;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.ClienteInstalacionRepositoryPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SnapshotOperativoRepositoryPort;
import saas.parqueadero.infrastructure.configuration.sync.SyncProperties;

@ExtendWith(MockitoExtension.class)
class ProveedorMonitoreoServiceTest {

    @Mock AuthenticatedUserProviderPort userProvider;
    @Mock ClienteInstalacionRepositoryPort clienteRepository;
    @Mock SnapshotOperativoRepositoryPort snapshotRepository;
    @Mock EmpresaRepositoryPort empresaRepository;

    ProveedorMonitoreoService service;

    @BeforeEach
    void setUp() {
        service = new ProveedorMonitoreoService(userProvider, clienteRepository, snapshotRepository,
            empresaRepository, new ObjectMapper(), new SyncProperties("https://api.ejemplo.com/", 90, 600L));
    }

    private void conRol(String rol) {
        when(userProvider.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).username("prov").roles(List.of(rol)).build());
    }

    @Test
    void soloSuperAdminPuedeListar() {
        conRol("ADMIN");
        assertThatThrownBy(() -> service.listarClientes()).isInstanceOf(BusinessException.class);
    }

    @Test
    void registraClienteYDevuelveTokenUnaVez() {
        conRol("SUPER_ADMIN");
        when(clienteRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(inv -> inv.getArgument(0));

        RegistrarClienteResponse res = service.registrarCliente(
            RegistrarClienteRequest.builder().nombre("  Parqueadero Centro  ").build());

        assertThat(res.getTenantId()).isNotBlank();
        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getSyncUrl()).isEqualTo("https://api.ejemplo.com/api/v1/sync/snapshot");
        assertThat(res.getNombre()).isEqualTo("Parqueadero Centro");
    }

    @Test
    void ultimoSnapshotSinDatosDevuelve404() {
        conRol("SUPER_ADMIN");
        when(clienteRepository.findByTenantId("t1")).thenReturn(Optional.of(
            ClienteInstalacion.builder().tenantId("t1").nombre("X").activa(true).creadaEn(Instant.now()).build()));
        when(snapshotRepository.findUltimoByTenantId("t1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ultimoSnapshot("t1")).isInstanceOf(ResourceNotFoundException.class);
    }
}
