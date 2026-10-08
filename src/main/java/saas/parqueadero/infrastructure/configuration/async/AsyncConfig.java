package saas.parqueadero.infrastructure.configuration.async;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /** Reloj de la aplicacion, en la zona horaria del negocio. Se inyecta para poder probar fechas. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    /**
     * Hilos propios para las notificaciones: un problema o una demora de WhatsApp no ocupa los hilos
     * de las peticiones. Si la cola se llena, la notificacion se descarta (con aviso) en vez de lanzar
     * una excepcion que afectaria a la operacion que la origino.
     */
    @Bean(name = "whatsappExecutor")
    public Executor whatsappExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("whatsapp-");
        executor.setRejectedExecutionHandler((tarea, pool) ->
            log.warn("[AsyncConfig] Cola de notificaciones llena: se descarta una notificacion de WhatsApp"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
