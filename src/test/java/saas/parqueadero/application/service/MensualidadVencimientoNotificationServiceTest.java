package saas.parqueadero.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@ExtendWith(MockitoExtension.class)
class MensualidadVencimientoNotificationServiceTest {

    @Mock SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;
    @Mock SedeRepositoryPort sedeRepositoryPort;
    @Mock EmpresaRepositoryPort empresaRepositoryPort;
    @Mock TwilioService twilioService;

    private MensualidadVencimientoNotificationService service() {
        return new MensualidadVencimientoNotificationService(
            suscripcionMensualRepositoryPort, sedeRepositoryPort, empresaRepositoryPort, twilioService);
    }

    @Test
    void cancelaAutomaticamenteLasVencidas() {
        SuscripcionMensual vencida = SuscripcionMensual.builder()
            .id(1L).placa("ABC123").activa(true).fechaFin(LocalDate.now().minusDays(1)).build();

        when(suscripcionMensualRepositoryPort.findActivasVencidas(any())).thenReturn(List.of(vencida));
        when(suscripcionMensualRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().cancelarVencidas();

        verify(suscripcionMensualRepositoryPort, times(1)).save(vencida);
        org.assertj.core.api.Assertions.assertThat(vencida.getActiva()).isFalse();
    }

    @Test
    void noHaceNadaSiNoHayVencidas() {
        when(suscripcionMensualRepositoryPort.findActivasVencidas(any())).thenReturn(List.of());

        service().cancelarVencidas();

        verify(suscripcionMensualRepositoryPort, never()).save(any());
    }
}
