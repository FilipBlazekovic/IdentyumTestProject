DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS phones;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    id IDENTITY NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(64) NOT NULL,
    salt VARCHAR(8) NOT NULL,
    creation_tstamp TIMESTAMP DEFAULT NOW()
);

CREATE TABLE phones
(
    id IDENTITY NOT NULL PRIMARY KEY,
    user_id LONG NOT NULL,
    phone_number VARCHAR(16) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_request_id VARCHAR(64) NOT NULL,
    creation_tstamp TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE images
(
    id IDENTITY NOT NULL PRIMARY KEY,
    owner_id LONG NOT NULL,
    image_name VARCHAR(128) NOT NULL,
    image_data CLOB NOT NULL,
    FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE permissions
(
    id IDENTITY NOT NULL PRIMARY KEY,
    image_id LONG NOT NULL,
    permitted_user_id LONG NOT NULL,
    FOREIGN KEY(image_id) REFERENCES images(id) ON DELETE CASCADE,
    FOREIGN KEY(permitted_user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(image_id, permitted_user_id)
);

CREATE TABLE sessions
(
    id IDENTITY NOT NULL PRIMARY KEY,
    user_id LONG NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    session_creation_tstamp TIMESTAMP NOT NULL DEFAULT NOW(),
--  session_last_access_tstamp TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
