CREATE TABLE IF NOT EXISTS settings (
    row_guard INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(32) NOT NULL,
    public_key BLOB(32) NOT NULL,
    private_key BLOB(32) NOT NULL,
    check(row_guard = 0)
);

CREATE TABLE IF NOT EXISTS ephemeral_keys (
    id_key INTEGER PRIMARY KEY AUTOINCREMENT,
    public_key BLOB(32) NOT NULL,
    private_key BLOB(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id_user INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(32) NOT NULL,
    public_key BLOB(32) NOT NULL,
    is_verified TINYINT(1) NOT NULL,
    out_sequence INTEGER NOT NULL,
    in_sequence INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS friends (
    id_user INTEGER PRIMARY KEY REFERENCES users(id_user)
);

CREATE TABLE IF NOT EXISTS messages (
    id_message INTEGER PRIMARY KEY AUTOINCREMENT,
    id_from INTEGER NOT NULL REFERENCES users(id_user),
    message VARCHAR(8196) NOT NULL
);
