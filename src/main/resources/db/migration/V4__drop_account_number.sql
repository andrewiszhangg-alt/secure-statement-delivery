-- Accounts are identified by their id; the account number is dropped.
ALTER TABLE accounts DROP CONSTRAINT uk_account_profile_number;
ALTER TABLE accounts DROP COLUMN account_number;
