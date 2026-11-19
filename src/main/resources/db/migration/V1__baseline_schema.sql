CREATE TABLE user_profiles (
    id            UUID                     NOT NULL,
    create_time   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL,
    cognito_sub   VARCHAR(255)             NOT NULL,
    email         VARCHAR(255)             NOT NULL,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uk_user_profiles_cognito_sub UNIQUE (cognito_sub)
);

CREATE INDEX idx_user_profile_email ON user_profiles (email);

CREATE TABLE accounts (
    id               UUID                     NOT NULL,
    create_time      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_time     TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT                   NOT NULL,
    user_profile_id  UUID                     NOT NULL,
    account_name     VARCHAR(255)             NOT NULL,
    account_number   VARCHAR(255)             NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user_profile FOREIGN KEY (user_profile_id)
        REFERENCES user_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT uk_account_profile_number UNIQUE (user_profile_id, account_number)
);

CREATE INDEX idx_account_user_profile_id ON accounts (user_profile_id);

CREATE TABLE media_resources (
    id                 UUID                     NOT NULL,
    create_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL,
    context            VARCHAR(255)             NOT NULL,
    object_key         VARCHAR(255)             NOT NULL,
    url                VARCHAR(2000)            NOT NULL,
    content_type       VARCHAR(255),
    original_filename  VARCHAR(255),
    CONSTRAINT pk_media_resources PRIMARY KEY (id),
    CONSTRAINT uk_media_resources_object_key UNIQUE (object_key)
);

CREATE TABLE account_statements (
    id                 UUID                     NOT NULL,
    create_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL,
    account_id         UUID                     NOT NULL,
    media_resource_id  UUID                     NOT NULL,
    CONSTRAINT pk_account_statements PRIMARY KEY (id),
    CONSTRAINT fk_account_statements_account FOREIGN KEY (account_id)
        REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_account_statements_media FOREIGN KEY (media_resource_id)
        REFERENCES media_resources (id) ON DELETE RESTRICT,
    CONSTRAINT uk_account_statements_media UNIQUE (media_resource_id)
);

CREATE INDEX idx_account_statement_account_id ON account_statements (account_id);
