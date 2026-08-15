-- Etapa 1 Inmo360 → WordPress: columna para idempotencia de exportación.
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate también la crea;
-- este script documenta el cambio esperado en PostgreSQL.

ALTER TABLE propiedades
    ADD COLUMN IF NOT EXISTS wordpress_page_id varchar(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_propiedades_wordpress_page_id
    ON propiedades (wordpress_page_id)
    WHERE wordpress_page_id IS NOT NULL;
