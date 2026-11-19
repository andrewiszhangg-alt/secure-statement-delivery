CREATE INDEX idx_account_statement_account_create_time
    ON account_statements (account_id, create_time DESC);
