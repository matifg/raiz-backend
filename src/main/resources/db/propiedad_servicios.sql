-- Checklist de servicios / atributos en propiedades.
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate también los crea;
-- este script documenta el cambio esperado en PostgreSQL.

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS luz boolean NOT NULL DEFAULT false;

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS agua boolean NOT NULL DEFAULT false;

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS gas boolean NOT NULL DEFAULT false;

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS pavimento boolean NOT NULL DEFAULT false;

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS cloacas boolean NOT NULL DEFAULT false;

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS apta_credito boolean NOT NULL DEFAULT false;
