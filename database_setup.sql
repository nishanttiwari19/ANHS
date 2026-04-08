-- Adarsh Narmada Hr Sec School Gangeo - MySQL Database Setup
-- Run this script in your MySQL Workbench or phpMyAdmin

CREATE DATABASE IF NOT EXISTS adarsh_school_db;
USE adarsh_school_db;

-- 1. Notifications (News & Updates)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    content TEXT,
    date DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Admission Applications
CREATE TABLE IF NOT EXISTS applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(255),
    student_class VARCHAR(50),
    mobile VARCHAR(15),
    data_json JSON,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. Career Applications
CREATE TABLE IF NOT EXISTS careers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    profile VARCHAR(100),
    mobile VARCHAR(15),
    data_json JSON,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 4. Principal Profile
CREATE TABLE IF NOT EXISTS principal_profile (
    id INT PRIMARY KEY DEFAULT 1,
    name VARCHAR(255),
    quote TEXT,
    message TEXT,
    photo VARCHAR(255)
);

-- Insert default principal data if not exists
INSERT IGNORE INTO principal_profile (id, name, quote, message, photo) 
VALUES (1, 'Principal Name', 'Education is the manifestation of perfection...', 'Welcome to our school...', 'default_principal.jpg');

-- 5. Testimonials
CREATE TABLE IF NOT EXISTS testimonials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    relation VARCHAR(255),
    message TEXT
);

-- 6. Faculty (Teachers)
CREATE TABLE IF NOT EXISTS faculty (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    photo VARCHAR(255)
);

-- 7. Vision & Mission
CREATE TABLE IF NOT EXISTS vision_mission (
    id INT PRIMARY KEY DEFAULT 1,
    content TEXT
);

INSERT IGNORE INTO vision_mission (id, content) 
VALUES (1, 'To provide quality education and foster holistic development.');

-- 8. Students (For ID Card Generator)
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    father_name VARCHAR(255),
    student_class VARCHAR(50),
    roll_no VARCHAR(50),
    dob DATE,
    blood_group VARCHAR(10),
    photo VARCHAR(255),
    address TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 9. Student Results
CREATE TABLE IF NOT EXISTS results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    roll_no VARCHAR(50) UNIQUE NOT NULL,
    student_name VARCHAR(255) NOT NULL,
    student_class VARCHAR(50),
    subjects_json JSON,
    total_marks INT,
    obtained_marks INT,
    percentage DECIMAL(5,2),
    grade VARCHAR(10),
    status VARCHAR(20), -- PASS/FAIL
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- 10. E-Library Books
CREATE TABLE IF NOT EXISTS books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    pdf_path VARCHAR(255),
    upload_date DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- 11. Website Enquiries
CREATE TABLE IF NOT EXISTS enquiries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    mobile VARCHAR(15),
    subject VARCHAR(255),
    message TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- 12. Admin Authentication
CREATE TABLE IF NOT EXISTS admin_users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial admin (username: Nishant, password: Nishant@2005)
-- Hash: 33a15cb941217549cf43eb0083fcd4a613ab1419da4c0529e74dbeb6889270e51
INSERT IGNORE INTO admin_users (username, password_hash) 
VALUES ('Nishant', '33a15cb941217549cf43eb0083fcd4a613ab1419da4c0529e74dbeb6889270e51');

CREATE TABLE IF NOT EXISTS sessions (
    token VARCHAR(100) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    expiry DATETIME NOT NULL
);
