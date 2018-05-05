CREATE TABLE IF NOT EXISTS users (
    id_user SERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    public_key BYTEA NOT NULL,
    signup_time TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS observed (
    id_observer INT NOT NULL REFERENCES users(id_user),
    id_observed INT NOT NULL REFERENCES users(id_user),
    PRIMARY KEY(id_observer, id_observed)
);

CREATE TABLE IF NOT EXISTS ephemeral_keys (
    id_key SERIAL PRIMARY KEY,
    id_user INT NOT NULL REFERENCES users(id_user),
    key BYTEA NOT NULL,
    signature BYTEA NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id_message SERIAL PRIMARY KEY,
    id_from INT NOT NULL REFERENCES users(id_user),
    id_to INT NOT NULL REFERENCES users(id_user),
    message bytea NOT NULL,
    key bytea NOT NULL,
    signature bytea NOT NULL,
    time TIMESTAMP NOT NULL DEFAULT current_timestamp
);
