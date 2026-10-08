package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import saas.parqueadero.application.dto.ClienteResponse;
import saas.parqueadero.application.dto.UpdateClienteRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.ClienteRepositoryPort;

class ClienteServiceTest {

    private final ClienteRepositoryPort repository = mock(ClienteRepositoryPort.class);
    private final AuthenticatedUserProviderPort userProvider = mock(AuthenticatedUserProviderPort.class);
    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(repository, new EmpresaScopeResolver(userProvider),
            Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneOffset.UTC));
        when(repository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
    }

    private void usuario(String rol, Long empresaId) {
        when(userProvider.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).username("u").roles(List.of(rol)).empresaId(empresaId).build());
    }

    @Test
    void unClienteNuevoNaceSinConsentimientoDeWhatsapp() {
        when(repository.findByEmpresaIdAndTelefono(5L, "+573001234567")).thenReturn(Optional.empty());

        Cliente cliente = service.resolverOCrear(5L, "+573001234567", null, null, "Cliente ABC123");

        assertThat(cliente.isWhatsappHabilitado()).isFalse();
        assertThat(cliente.getNombre()).isEqualTo("Cliente ABC123");
        assertThat(cliente.isPrefFacturacion()).isTrue();
        assertThat(cliente.isPrefOperativas()).isFalse();
    }

    @Test
    void conConsentimientoExplicitoSeHabilita() {
        when(repository.findByEmpresaIdAndTelefono(5L, "+573001234567")).thenReturn(Optional.empty());

        Cliente cliente = service.resolverOCrear(5L, "+573001234567", "  Juan Perez ", true, "Cliente ABC123");

        assertThat(cliente.isWhatsappHabilitado()).isTrue();
        assertThat(cliente.getNombre()).isEqualTo("Juan Perez");
    }

    @Test
    void unClienteExistenteSeReutilizaPorTelefonoYSoloCambiaLoQueLlega() {
        Cliente existente = Cliente.builder().id(8L).empresaId(5L).nombre("Juan").telefono("+573001234567")
            .whatsappHabilitado(true).build();
        when(repository.findByEmpresaIdAndTelefono(5L, "+573001234567")).thenReturn(Optional.of(existente));

        Cliente sinCambios = service.resolverOCrear(5L, "+573001234567", null, null, "Cliente X");
        assertThat(sinCambios.getId()).isEqualTo(8L);
        assertThat(sinCambios.isWhatsappHabilitado()).isTrue();
        verify(repository, never()).save(any());

        Cliente retirado = service.resolverOCrear(5L, "+573001234567", null, false, "Cliente X");
        assertThat(retirado.isWhatsappHabilitado()).isFalse();
        assertThat(retirado.getNombre()).isEqualTo("Juan");
    }

    @Test
    void elAdminSoloVeLosClientesDeSuEmpresa() {
        usuario("ADMIN", 5L);
        when(repository.findByEmpresaId(5L)).thenReturn(List.of(Cliente.builder().id(1L).empresaId(5L).nombre("A").build()));

        List<ClienteResponse> lista = service.listar(null);

        assertThat(lista).hasSize(1);
        assertThatThrownBy(() -> service.listar(99L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void nadiePuedeEditarUnClienteDeOtraEmpresa() {
        usuario("ADMIN", 5L);
        when(repository.findById(3L)).thenReturn(Optional.of(Cliente.builder().id(3L).empresaId(99L).nombre("Ajeno").build()));

        assertThatThrownBy(() -> service.actualizar(3L, UpdateClienteRequest.builder().whatsappHabilitado(true).build()))
            .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void elOperarioNoPuedeCambiarPreferencias() {
        usuario("OPERARIO", 5L);
        when(repository.findById(3L)).thenReturn(Optional.of(Cliente.builder().id(3L).empresaId(5L).nombre("A").build()));

        assertThatThrownBy(() -> service.actualizar(3L, UpdateClienteRequest.builder().whatsappHabilitado(true).build()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizarCambiaSoloLosCamposRecibidos() {
        usuario("ADMIN", 5L);
        Cliente cliente = Cliente.builder().id(3L).empresaId(5L).nombre("A").telefono("+573001234567")
            .whatsappHabilitado(false).prefFacturacion(true).prefMensualidades(true).build();
        when(repository.findById(3L)).thenReturn(Optional.of(cliente));

        ClienteResponse respuesta = service.actualizar(3L,
            UpdateClienteRequest.builder().whatsappHabilitado(true).prefMensualidades(false).build());

        assertThat(respuesta.isWhatsappHabilitado()).isTrue();
        assertThat(respuesta.isPrefMensualidades()).isFalse();
        assertThat(respuesta.isPrefFacturacion()).isTrue();
        assertThat(respuesta.getNombre()).isEqualTo("A");
    }
}
