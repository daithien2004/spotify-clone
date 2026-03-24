CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Assign default ROLE_USER to all existing users
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER' FROM users;
