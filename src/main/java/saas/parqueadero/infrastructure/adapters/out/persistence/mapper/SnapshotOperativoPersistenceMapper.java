package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.ResumenOperativo;
import saas.parqueadero.domain.model.SnapshotOperativo;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SnapshotOperativoJpaEntity;

@Component
public class SnapshotOperativoPersistenceMapper {

    public SnapshotOperativo toDomain(SnapshotOperativoJpaEntity e) {
        if (e == null) {
            return null;
        }
        return SnapshotOperativo.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .recibidoEn(e.getRecibidoEn())
            .generadoEn(e.getGeneradoEn())
            .appVersion(e.getAppVersion())
            .estadoLicencia(e.getEstadoLicencia())
            .resumen(ResumenOperativo.builder()
                .vehiculosDentro(e.getVehiculosDentro())
                .entradasHoy(e.getEntradasHoy())
                .salidasHoy(e.getSalidasHoy())
                .recaudoHoy(e.getRecaudoHoy())
                .build())
            .payloadJson(e.getPayloadJson())
            .build();
    }

    public SnapshotOperativoJpaEntity toEntity(SnapshotOperativo d) {
        if (d == null) {
            return null;
        }
        ResumenOperativo r = d.getResumen();
        return SnapshotOperativoJpaEntity.builder()
            .id(d.getId())
            .tenantId(d.getTenantId())
            .recibidoEn(d.getRecibidoEn())
            .generadoEn(d.getGeneradoEn())
            .appVersion(d.getAppVersion())
            .estadoLicencia(d.getEstadoLicencia())
            .vehiculosDentro(r == null ? null : r.getVehiculosDentro())
            .entradasHoy(r == null ? null : r.getEntradasHoy())
            .salidasHoy(r == null ? null : r.getSalidasHoy())
            .recaudoHoy(r == null ? null : r.getRecaudoHoy())
            .payloadJson(d.getPayloadJson())
            .build();
    }
}
