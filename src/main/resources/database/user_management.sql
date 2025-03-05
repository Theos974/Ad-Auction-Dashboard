CREATE DATABASE IF NOT EXISTS ad_auction_user;
USE ad_auction_user;

CREATE TABLE IF NOT EXISTS users (
     id INT PRIMARY KEY AUTO_INCREMENT,
     username VARCHAR(50) UNIQUE NOT NULL,
     email VARCHAR(255) UNIQUE NOT NULL,
     phone VARCHAR(20) UNIQUE NOT NULL,
     password VARCHAR(255) NOT NULL,
     role ENUM('admin', 'user') NOT NULL DEFAULT 'user'
);

CREATE PROCEDURE IF NOT EXISTS AddUser(IN p_username VARCHAR(50), IN p_email VARCHAR(255), IN p_phone VARCHAR(20), IN p_password VARCHAR(255), IN p_role ENUM('admin', 'user'))
BEGIN
    INSERT INTO users (username, email, phone, password, role) VALUES (p_username, p_email, p_phone, p_password, p_role);
END;

CREATE PROCEDURE IF NOT EXISTS DeleteUser(IN p_id INT)
BEGIN
    DELETE FROM users WHERE id = p_id;
END;

CREATE PROCEDURE IF NOT EXISTS UpdateUser(IN p_id INT, IN p_username VARCHAR(50), IN p_email VARCHAR(255), IN p_phone VARCHAR(20), IN p_password VARCHAR(255), IN p_role ENUM('admin', 'user'))
BEGIN
    UPDATE users SET username = p_username, email = p_email, phone = p_phone, password = p_password, role = p_role WHERE id = p_id;
END;