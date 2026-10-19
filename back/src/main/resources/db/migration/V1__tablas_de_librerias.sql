-- ---------------------------------------------------------------------------
-- V1 · Las tablas de las librerías spring-* (0.12.5).
--
-- Las librerías traen sus entidades JPA pero NO sus migraciones. Aquí manda Flyway y
-- Hibernate solo valida, así que si nadie las crea el back no arranca
-- (`Schema-validation: missing table [app_settings]`). Mismo DDL que pimon3d.
--
-- AL SUBIR DE VERSIÓN LAS LIBRERÍAS: columna nueva = migración nueva. ContextoArrancaTest
-- lo detecta en `mvn test`.
-- ---------------------------------------------------------------------------

-- ── spring-common: ajustes editables ───────────────────────────────────────
CREATE TABLE app_settings (
    module_key  varchar(64)     NOT NULL,
    payload     TEXT            NOT NULL,
    updated_at  timestamp(6)    NOT NULL,

    CONSTRAINT app_settings_pkey PRIMARY KEY (module_key)
);

-- ── spring-users: cuentas y roles ──────────────────────────────────────────
CREATE TABLE app_users (
    id          varchar(36)     NOT NULL,
    created_at  timestamp(6)    NOT NULL,
    updated_at  timestamp(6)    NOT NULL,
    email       varchar(255)    NOT NULL UNIQUE,
    name        varchar(255)    NOT NULL,
    password    varchar(255)    NOT NULL,
    active      boolean         NOT NULL,

    CONSTRAINT app_users_pkey PRIMARY KEY (id)
);

CREATE TABLE user_roles (
    user_id     varchar(36)     NOT NULL,
    role        varchar(255)    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_EDITOR', 'ROLE_STAFF')),

    CONSTRAINT fk_user_roles_usuario FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_user_roles ON user_roles (user_id, role);

-- ── spring-users: recuperar contraseña ─────────────────────────────────────
-- Aquí se entra solo con Google, pero la entidad la registra la librería igualmente y
-- `validate` exige la tabla.
CREATE TABLE tokens_recuperacion (
    id          varchar(36)     NOT NULL,
    created_at  timestamp(6)    NOT NULL,
    updated_at  timestamp(6)    NOT NULL,
    user_id     varchar(36)     NOT NULL,
    hash        varchar(64)     NOT NULL,
    expira_en   timestamp(6)    NOT NULL,
    usado_en    timestamp(6),

    CONSTRAINT tokens_recuperacion_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_token_recuperacion_hash ON tokens_recuperacion (hash);
CREATE INDEX idx_token_recuperacion_usuario ON tokens_recuperacion (user_id);

-- ── spring-google: entrar con Google ───────────────────────────────────────
-- Se crea aunque Google esté apagado: con `habilitado=false` la librería no registra la
-- entidad y `validate` no la mira. Así encenderlo es solo cambiar variables de entorno.
CREATE TABLE cuentas_google (
    id              varchar(36)     NOT NULL,
    created_at      timestamp(6)    NOT NULL,
    updated_at      timestamp(6)    NOT NULL,
    user_id         varchar(36)     NOT NULL,
    -- Identificador estable de la persona en Google. Se busca por aquí, no por correo:
    -- el correo de Gmail puede cambiar y la persona sigue siendo la misma.
    sub             varchar(255)    NOT NULL,
    email           varchar(255)    NOT NULL,
    vinculada_en    timestamp(6)    NOT NULL,

    CONSTRAINT cuentas_google_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cuentas_google_sub  UNIQUE (sub),
    CONSTRAINT uk_cuentas_google_user UNIQUE (user_id),
    CONSTRAINT fk_cuentas_google_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);
