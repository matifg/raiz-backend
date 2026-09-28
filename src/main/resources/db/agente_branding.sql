-- Branding de agente para tiles / perfil público.
-- inmobiliaria reutiliza la columna existente nombre_inmobiliaria.
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate también crea estas columnas.

ALTER TABLE agentes
    ADD COLUMN IF NOT EXISTS logo_url varchar(500);

ALTER TABLE agentes
    ADD COLUMN IF NOT EXISTS cover_url varchar(500);
