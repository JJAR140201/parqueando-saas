package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.parqueadero.application.dto.CreateEmpresaResponse;
import saas.parqueadero.application.dto.CreateEmpresaSedeRequest;
import saas.parqueadero.application.dto.CreateEmpresaRequest;
import saas.parqueadero.application.dto.IssueLicenciaRequest;
import saas.parqueadero.application.dto.LicenciaAdminRequest;
import saas.parqueadero.application.dto.LicenciaIssuedResponse;
import saas.parqueadero.application.dto.LicenciaRedemptionRequest;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.RegisterUserResponse;
import saas.parqueadero.application.dto.SedeSummaryResponse;
import saas.parqueadero.application.dto.ValidateLicenciaRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.EstadoLicencia;
import saas.parqueadero.domain.model.Licencia;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.LicenciaRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;
import saas.parqueadero.licensing.LicenseSerialCodec;

@ExtendWith(MockitoExtension.class)
class LicenciaServiceTest {

    @Mock AuthenticatedUserProviderPort userProvider;
    @Mock LicenciaRepositoryPort licenciaRepository;
    @Mock EmpresaRepositoryPort empresaRepository;
    @Mock UsuarioRepositoryPort usuarioRepository;
    @Mock TenantProvisioningService tenantProvisioningService;
    @Mock TokenIssuanceService tokenIssuanceService;

    private LicenseSerialCodec codec;
    private LicenciaService service;

    @BeforeEach
    void setUp() {
        codec = new LicenseSerialCodec(Base64.getDecoder().decode("Q0hBTkdFX1RISVNfTElDRU5TRV9TRUNSRVRfMzJCIQ=="));
        service = new LicenciaService(userProvider, licenciaRepository, empresaRepository, usuarioRepository,
            tenantProvisioningService, tokenIssuanceService, codec);
    }

    private void conRol(String rol) {
        when(userProvider.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).username("super").roles(List.of(rol)).build());
    }

    @Test
    void soloSuperAdminPuedeEmitir() {
        conRol("ADMIN");
        assertThatThrownBy(() -> service.issue(IssueLicenciaRequest.builder().build()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void emiteLicenciaPendienteConCodigoLegible() {
        conRol("SUPER_ADMIN");
        when(licenciaRepository.save(any())).thenAnswer(inv -> {
            Licencia licencia = inv.getArgument(0);
            licencia.setId(10L);
            return licencia;
        });

        LicenciaIssuedResponse response = service.issue(IssueLicenciaRequest.builder().duracionDias(30).build());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCodigo()).contains("-");
        assertThat(response.getEstado()).isEqualTo(EstadoLicencia.PENDIENTE.name());
        assertThat(response.getFechaExpiracion()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void validaCodigoDesconocidoComoInvalido() {
        String codigo = codec.generate(LocalDate.now().plusDays(365));
        when(licenciaRepository.findByCodigo(any())).thenReturn(Optional.empty());

        var resultado = service.validate(ValidateLicenciaRequest.builder().codigo(codigo).build());

        assertThat(resultado.isValida()).isFalse();
    }

    @Test
    void redimeLicenciaCreaEmpresaYUsuarioYLoguea() {
        String codigo = codec.generate(LocalDate.now().plusDays(365));
        Licencia licencia = Licencia.builder()
            .id(1L)
            .codigo(codigo.replaceAll("[\\s-]", ""))
            .estado(EstadoLicencia.PENDIENTE)
            .fechaExpiracion(LocalDate.now().plusDays(365))
            .fechaEmision(LocalDateTime.now())
            .build();

        when(licenciaRepository.findByCodigo(any())).thenReturn(Optional.of(licencia));
        when(tenantProvisioningService.createEmpresaWithSedes(any())).thenReturn(CreateEmpresaResponse.builder()
            .empresaId(5L).nit("900").nombre("Cliente")
            .sedes(List.of(SedeSummaryResponse.builder().id(50L).nombre("Principal").capacidadTotal(10).capacidadActual(10).build()))
            .build());
        when(tenantProvisioningService.createUserForEmpresa(any())).thenReturn(RegisterUserResponse.builder()
            .usuarioId(100L).username("admin.cliente").rol("ADMIN").empresaId(5L).sedeId(50L).build());
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(saas.parqueadero.domain.model.Usuario.builder()
            .id(100L).username("admin.cliente").rol(saas.parqueadero.domain.model.RolUsuario.ADMIN).empresaId(5L).sedeId(50L).build()));
        when(tokenIssuanceService.buildLoginResponse(any())).thenReturn(LoginResponse.builder()
            .accessToken("token").refreshToken("refresh").usuarioId(100L).build());

        LicenciaRedemptionRequest request = LicenciaRedemptionRequest.builder()
            .codigo(codigo)
            .empresa(CreateEmpresaRequest.builder().nit("900").nombre("Cliente")
                .sedes(List.of(CreateEmpresaSedeRequest.builder().nombre("Principal").capacidadTotal(10).build()))
                .build())
            .admin(LicenciaAdminRequest.builder().username("admin.cliente").password("secret123").build())
            .build();

        LoginResponse response = service.redeem(request);

        assertThat(response.getAccessToken()).isEqualTo("token");
        assertThat(licencia.getEstado()).isEqualTo(EstadoLicencia.REDIMIDA);
        assertThat(licencia.getEmpresaId()).isEqualTo(5L);
    }

    @Test
    void redimirCodigoYaUtilizadoFalla() {
        String codigo = codec.generate(LocalDate.now().plusDays(365));
        Licencia licencia = Licencia.builder()
            .id(1L)
            .codigo(codigo.replaceAll("[\\s-]", ""))
            .estado(EstadoLicencia.REDIMIDA)
            .fechaExpiracion(LocalDate.now().plusDays(365))
            .build();
        when(licenciaRepository.findByCodigo(any())).thenReturn(Optional.of(licencia));

        LicenciaRedemptionRequest request = LicenciaRedemptionRequest.builder()
            .codigo(codigo)
            .empresa(CreateEmpresaRequest.builder().nit("900").nombre("Cliente")
                .sedes(List.of(CreateEmpresaSedeRequest.builder().nombre("Principal").capacidadTotal(10).build()))
                .build())
            .admin(LicenciaAdminRequest.builder().username("admin.cliente").password("secret123").build())
            .build();

        assertThatThrownBy(() -> service.redeem(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void redimirCodigoVencidoFalla() {
        String codigo = codec.generate(LocalDate.now().minusDays(1));
        Licencia licencia = Licencia.builder()
            .id(1L)
            .codigo(codigo.replaceAll("[\\s-]", ""))
            .estado(EstadoLicencia.PENDIENTE)
            .fechaExpiracion(LocalDate.now().minusDays(1))
            .build();
        when(licenciaRepository.findByCodigo(any())).thenReturn(Optional.of(licencia));

        LicenciaRedemptionRequest request = LicenciaRedemptionRequest.builder()
            .codigo(codigo)
            .empresa(CreateEmpresaRequest.builder().nit("900").nombre("Cliente")
                .sedes(List.of(CreateEmpresaSedeRequest.builder().nombre("Principal").capacidadTotal(10).build()))
                .build())
            .admin(LicenciaAdminRequest.builder().username("admin.cliente").password("secret123").build())
            .build();

        assertThatThrownBy(() -> service.redeem(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void redimirCodigoConFirmaInvalidaFalla() {
        LicenciaRedemptionRequest request = LicenciaRedemptionRequest.builder()
            .codigo("XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXX")
            .empresa(CreateEmpresaRequest.builder().nit("900").nombre("Cliente")
                .sedes(List.of(CreateEmpresaSedeRequest.builder().nombre("Principal").capacidadTotal(10).build()))
                .build())
            .admin(LicenciaAdminRequest.builder().username("admin.cliente").password("secret123").build())
            .build();

        assertThatThrownBy(() -> service.redeem(request)).isInstanceOf(BusinessException.class);
    }
}
