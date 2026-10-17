package saas.parqueadero.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.ClienteResponse;
import saas.parqueadero.application.dto.UpdateClienteRequest;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.port.in.ClienteUseCase;
import saas.parqueadero.domain.port.out.ClienteRepositoryPort;

@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteUseCase {

    private final ClienteRepositoryPort clienteRepository;
    private final EmpresaScopeResolver scopeResolver;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(Long empresaId) {
        Long empresa = scopeResolver.resolver(empresaId, true);
        return clienteRepository.findByEmpresaId(empresa).stream().map(ClienteService::toResponse).toList();
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, UpdateClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        // Solo se puede editar un cliente de la propia empresa
        scopeResolver.resolver(cliente.getEmpresaId(), false);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            cliente.setNombre(request.getNombre().trim());
        }
        if (request.getWhatsappHabilitado() != null) {
            cliente.setWhatsappHabilitado(request.getWhatsappHabilitado());
        }
        if (request.getPrefFacturacion() != null) {
            cliente.setPrefFacturacion(request.getPrefFacturacion());
        }
        if (request.getPrefMensualidades() != null) {
            cliente.setPrefMensualidades(request.getPrefMensualidades());
        }
        if (request.getPrefConfirmacionesPago() != null) {
            cliente.setPrefConfirmacionesPago(request.getPrefConfirmacionesPago());
        }
        if (request.getPrefOperativas() != null) {
            cliente.setPrefOperativas(request.getPrefOperativas());
        }
        cliente.setActualizadoEn(clock.instant());
        return toResponse(clienteRepository.save(cliente));
    }

    /**
     * Busca al cliente por empresa y telefono y lo crea si no existe. Si llegan nombre o consentimiento
     * se actualizan; los clientes nuevos nacen sin consentimiento de WhatsApp salvo que se indique.
     */
    @Transactional
    public Cliente resolverOCrear(Long empresaId, String telefono, String nombre, Boolean whatsappHabilitado,
        String nombreDeRespaldo) {
        Instant ahora = clock.instant();
        String nombreLimpio = nombre == null || nombre.isBlank() ? null : nombre.trim();
        Cliente existente = clienteRepository.findByEmpresaIdAndTelefono(empresaId, telefono).orElse(null);

        if (existente == null) {
            return clienteRepository.save(Cliente.builder()
                .empresaId(empresaId)
                .nombre(nombreLimpio != null ? nombreLimpio : nombreDeRespaldo)
                .telefono(telefono)
                .whatsappHabilitado(Boolean.TRUE.equals(whatsappHabilitado))
                .prefFacturacion(true)
                .prefMensualidades(true)
                .prefConfirmacionesPago(true)
                .prefOperativas(false)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build());
        }

        boolean cambio = false;
        if (nombreLimpio != null && !nombreLimpio.equals(existente.getNombre())) {
            existente.setNombre(nombreLimpio);
            cambio = true;
        }
        if (whatsappHabilitado != null && whatsappHabilitado != existente.isWhatsappHabilitado()) {
            existente.setWhatsappHabilitado(whatsappHabilitado);
            cambio = true;
        }
        if (cambio) {
            existente.setActualizadoEn(ahora);
            return clienteRepository.save(existente);
        }
        return existente;
    }

    @Transactional(readOnly = true)
    public Map<Long, Cliente> porIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return clienteRepository.findAllByIds(new HashSet<>(ids)).stream()
            .collect(Collectors.toMap(Cliente::getId, Function.identity()));
    }

    static ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.builder()
            .id(cliente.getId())
            .empresaId(cliente.getEmpresaId())
            .nombre(cliente.getNombre())
            .telefono(cliente.getTelefono())
            .whatsappHabilitado(cliente.isWhatsappHabilitado())
            .prefFacturacion(cliente.isPrefFacturacion())
            .prefMensualidades(cliente.isPrefMensualidades())
            .prefConfirmacionesPago(cliente.isPrefConfirmacionesPago())
            .prefOperativas(cliente.isPrefOperativas())
            .build();
    }
}
