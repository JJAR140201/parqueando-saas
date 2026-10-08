-- Una sola placa ACTIVA por sede y empresa (refuerza el bloqueo de sede de la aplicacion).
-- Si ya hubiera duplicados historicos, no se crea el indice: se avisa y se debe depurar a mano.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM registro_parqueo
        WHERE estado = 'ACTIVO'
        GROUP BY placa, sede_id, empresa_id
        HAVING count(*) > 1
    ) THEN
        RAISE WARNING 'Hay placas ACTIVAS duplicadas en registro_parqueo: no se crea uk_registro_activo_placa_sede. Depura los duplicados y crea el indice manualmente.';
    ELSE
        CREATE UNIQUE INDEX IF NOT EXISTS uk_registro_activo_placa_sede
            ON registro_parqueo (placa, sede_id, empresa_id)
            WHERE estado = 'ACTIVO';
    END IF;
END
$$;
