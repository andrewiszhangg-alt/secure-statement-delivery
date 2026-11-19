ALTER TABLE user_profiles RENAME COLUMN cognito_sub TO idp_uuid;
ALTER TABLE user_profiles RENAME CONSTRAINT uk_user_profiles_cognito_sub TO uk_user_profiles_idp_uuid;

-- Replace legacy non-UUID values (e.g. the old dev seed) so the column can become UUID.
UPDATE user_profiles
SET idp_uuid = gen_random_uuid()::text
WHERE idp_uuid !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

ALTER TABLE user_profiles ALTER COLUMN idp_uuid TYPE UUID USING idp_uuid::uuid;
