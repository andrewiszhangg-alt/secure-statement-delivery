-- Account types live in their own table; accounts reference one by id instead of carrying a name.
CREATE TABLE account_types (
    id            UUID                     NOT NULL,
    create_time   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL,
    code          VARCHAR(30)              NOT NULL,
    label         VARCHAR(100)             NOT NULL,
    CONSTRAINT pk_account_types PRIMARY KEY (id),
    CONSTRAINT uk_account_types_code UNIQUE (code)
);

-- Default account types
INSERT INTO account_types (id, create_time, updated_time, version, code, label) VALUES
    (gen_random_uuid(), now(), now(), 0, 'SAVINGS',     'Savings'),
    (gen_random_uuid(), now(), now(), 0, 'CREDIT_CARD', 'Credit card'),
    (gen_random_uuid(), now(), now(), 0, 'BUSINESS',    'Business'),
    (gen_random_uuid(), now(), now(), 0, 'PERSONAL',    'Personal');

ALTER TABLE accounts ADD COLUMN account_type_id UUID;
UPDATE accounts SET account_type_id = (SELECT id FROM account_types WHERE code = 'PERSONAL');
ALTER TABLE accounts ALTER COLUMN account_type_id SET NOT NULL;
ALTER TABLE accounts ADD CONSTRAINT fk_accounts_account_type FOREIGN KEY (account_type_id)
    REFERENCES account_types (id) ON DELETE RESTRICT;
CREATE INDEX idx_account_account_type_id ON accounts (account_type_id);

ALTER TABLE accounts DROP COLUMN account_name;
