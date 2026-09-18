-- Campo opcional: superficie cubierta (m²).
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate también lo crea;
-- este script documenta el cambio esperado en PostgreSQL.
-- Propiedades existentes quedan en NULL (también se tolera 0 desde la API).

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS superficie_cubierta_m2 numeric(10,2) NULL;
