CREATE TABLE storage_deletions (
    id           UUID                     NOT NULL,
    context      VARCHAR(255)             NOT NULL,
    object_key   VARCHAR(255)             NOT NULL,
    attempts     INTEGER                  NOT NULL,
    create_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_storage_deletions PRIMARY KEY (id),
    CONSTRAINT uk_storage_deletions_context_key UNIQUE (context, object_key)
);
