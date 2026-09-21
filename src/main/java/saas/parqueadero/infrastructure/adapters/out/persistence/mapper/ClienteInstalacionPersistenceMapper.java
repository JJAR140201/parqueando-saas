package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.ClienteInstalacion;
import saas.parqueadero.domain.model.ResumenOperativo;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.ClienteInstalacionJpaEntity;

/**
 * Mapeo manual (no MapStruct) porque el resumen operativo vive anidado en el
 * dominio y aplanado en columnas en la entidad.
 */
@Component
public class ClienteInstalacionPersistenceMapper {

    public ClienteInstalacion toDomain(ClienteInstalacionJpaEntity e) {
        if (e == null) {
            return null;
        }
        return ClienteInstalacion.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .nombre(e.getNombre())
            .empresaId(e.getEmpresaId())
            .tokenHash(e.getTokenHash())
            .activa(e.isActiva())
            .creadaEn(e.getCreadaEn())
            .ultimaSyncEn(e.getUltimaSyncEn())
            .appVersion(e.getAppVersion())
            .estadoLicencia(e.getEstadoLicencia())
            .licenciaExpiraEn(e.getLicenciaExpiraEn())
            .ultimoResumen(ResumenOperativo.builder()
                .vehiculosDentro(e.getUltimoVehiculosDentro())
                .entradasHoy(e.getUltimoEntradasHoy())
                .salidasHoy(e.getUltimoSalidasHoy())
                .recaudoHoy(e.getUltimoRecaudoHoy())
                .build())
            .build();
    }

    public ClienteInstalacionJpaEntity toEntity(ClienteInstalacion d) {
        if (d == null) {
            return null;
        }
        ResumenOperativo r = d.getUltimoResumen();
        return ClienteInstalacionJpaEntity.builder()
            .id(d.getId())
            .tenantId(d.getTenantId())
            .nombre(d.getNombre())
            .empresaId(d.getEmpresaId())
            .tokenHash(d.getTokenHash())
            .activa(d.isActiva())
            .creadaEn(d.getCreadaEn())
            .ultimaSyncEn(d.getUltimaSyncEn())
            .appVersion(d.getAppVersion())
            .estadoLicencia(d.getEstadoLicencia())
            .licenciaExpiraEn(d.getLicenciaExpiraEn())
            .ultimoVehiculosDentro(r == null ? null : r.getVehiculosDentro())
            .ultimoEntradasHoy(r == null ? null : r.getEntradasHoy())
            .ultimoSalidasHoy(r == null ? null : r.getSalidasHoy())
            .ultimoRecaudoHoy(r == null ? null : r.getRecaudoHoy())
            .build();
    }
}
