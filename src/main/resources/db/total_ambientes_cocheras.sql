-- Campos opcionales en propiedades: total ambientes y cocheras.
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate también los crea;
-- este script documenta el cambio esperado en PostgreSQL.

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS total_ambientes int4 NULL;

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS cocheras int4 NULL;
