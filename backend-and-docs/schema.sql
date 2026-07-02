-- Leo Express Central Europe Rail & Coach Database Schema
-- Dialect: PostgreSQL

-- 1. STATIONS TABLE
CREATE TABLE stations (
    id VARCHAR(10) PRIMARY KEY, -- e.g. 'PRG', 'VIE'
    name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    is_coach_stop BOOLEAN DEFAULT FALSE
);

-- 2. TRAINS & COACHES TABLES
CREATE TABLE trains (
    id VARCHAR(20) PRIMARY KEY, -- e.g. 'LE-1352'
    model VARCHAR(50) NOT NULL,
    total_carriages INT DEFAULT 5,
    economy_capacity INT DEFAULT 200,
    premium_capacity INT DEFAULT 40,
    status VARCHAR(20) DEFAULT 'ACTIVE' -- ACTIVE, MAINTENANCE
);

CREATE TABLE coaches (
    id VARCHAR(20) PRIMARY KEY, -- e.g. 'LC-5923'
    model VARCHAR(50) NOT NULL,
    capacity INT DEFAULT 50,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 3. ROUTES TABLE
CREATE TABLE routes (
    id SERIAL PRIMARY KEY,
    from_station_id VARCHAR(10) REFERENCES stations(id) ON DELETE CASCADE,
    to_station_id VARCHAR(10) REFERENCES stations(id) ON DELETE CASCADE,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    transport_type VARCHAR(10) CHECK (transport_type IN ('TRAIN', 'COACH')),
    vehicle_id VARCHAR(20), -- references trains(id) or coaches(id)
    base_price_czk DECIMAL(10,2) NOT NULL,
    carrier_code VARCHAR(5) DEFAULT 'LE',
    stops TEXT -- Comma-separated intermediate station names
);

-- 4. USERS & LOYALTY ACCOUNTS
CREATE TABLE loyalty_accounts (
    id SERIAL PRIMARY KEY,
    loyalty_number VARCHAR(20) UNIQUE NOT NULL,
    points INT DEFAULT 0,
    tier VARCHAR(20) DEFAULT 'STANDARD', -- STANDARD, BRONZE, SILVER, GOLD
    joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'PASSENGER', -- PASSENGER, STAFF, ADMIN
    loyalty_account_id INT REFERENCES loyalty_accounts(id) ON DELETE SET NULL,
    preferred_language VARCHAR(5) DEFAULT 'en',
    currency VARCHAR(5) DEFAULT 'CZK',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. SAVED PASSENGERS & PAYMENTS
CREATE TABLE saved_passengers (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    discount_type VARCHAR(20) DEFAULT 'NONE' -- NONE, STUDENT, SENIOR, CHILD
);

CREATE TABLE saved_payment_methods (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    card_holder VARCHAR(100) NOT NULL,
    masked_number VARCHAR(25) NOT NULL,
    expiry_date VARCHAR(5) NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    token VARCHAR(255) NOT NULL
);

-- 6. BOOKINGS & TICKETS
CREATE TABLE bookings (
    id VARCHAR(30) PRIMARY KEY, -- e.g. 'LE-482103-CZ'
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    booking_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, CANCELLED, REFUNDED
    total_price_czk DECIMAL(10,2) NOT NULL
);

CREATE TABLE tickets (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(30) REFERENCES bookings(id) ON DELETE CASCADE,
    route_id INT REFERENCES routes(id) ON DELETE RESTRICT,
    departure_date DATE NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    carriage_number VARCHAR(20) NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    ticket_type VARCHAR(20) DEFAULT 'ADULT',
    price_paid_czk DECIMAL(10,2) NOT NULL,
    qr_code_data VARCHAR(255) NOT NULL
);

-- 7. PAYMENTS TRANSACTION LOGS
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(30) REFERENCES bookings(id) ON DELETE CASCADE,
    transaction_reference VARCHAR(100) UNIQUE NOT NULL,
    amount_czk DECIMAL(10,2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'CZK',
    payment_status VARCHAR(20) DEFAULT 'SUCCESSFUL', -- SUCCESSFUL, FAILED
    payment_method_id INT REFERENCES saved_payment_methods(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. PUSH NOTIFICATIONS
CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'INFO', -- INFO, DELAY, PLATFORM, PROMO
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- SAMPLE SEED DATA
-- ============================================================

-- Seed Stations
INSERT INTO stations (id, name, city, country, latitude, longitude, is_coach_stop) VALUES
('PRG', 'Praha hl.n.', 'Prague', 'Czech Republic', 50.0830, 14.4350, FALSE),
('OSV', 'Ostrava hl.n.', 'Ostrava', 'Czech Republic', 49.8510, 18.2680, FALSE),
('BRN', 'Brno hl.n.', 'Brno', 'Czech Republic', 49.1900, 16.6130, FALSE),
('PLZ', 'Plzeň hl.n.', 'Pilsen', 'Czech Republic', 49.7430, 13.3890, FALSE),
('BTS', 'Bratislava hl.st.', 'Bratislava', 'Slovakia', 48.1580, 17.1060, FALSE),
('KSC', 'Košice hl.st.', 'Kosice', 'Slovakia', 48.7280, 21.2670, FALSE),
('KRK', 'Kraków Główny', 'Krakow', 'Poland', 50.0680, 19.9470, FALSE),
('VIE', 'Wien Hauptbahnhof', 'Vienna', 'Austria', 48.1850, 16.3760, FALSE),
('BER', 'Berlin Hbf', 'Berlin', 'Germany', 52.5250, 13.3690, FALSE),
('MUC', 'München Hbf', 'Munich', 'Germany', 48.1400, 11.5580, FALSE);

-- Seed Trains and Coaches
INSERT INTO trains (id, model, total_carriages, economy_capacity, premium_capacity, status) VALUES
('LE-FLIRT-1', 'Stadler FLIRT Leo-Edition', 5, 200, 40, 'ACTIVE'),
('LE-FLIRT-2', 'Stadler FLIRT Leo-Edition', 5, 200, 40, 'ACTIVE');

INSERT INTO coaches (id, model, capacity, status) VALUES
('LC-BUS-1', 'Scania Irizar i8', 50, 'ACTIVE'),
('LC-BUS-2', 'Scania Irizar i8', 50, 'ACTIVE');

-- Seed Routes
INSERT INTO routes (from_station_id, to_station_id, departure_time, arrival_time, transport_type, vehicle_id, base_price_czk, stops) VALUES
('PRG', 'OSV', '06:12:00', '09:32:00', 'TRAIN', 'LE-FLIRT-1', 329.00, 'Pardubice, Olomouc'),
('PRG', 'OSV', '10:12:00', '13:32:00', 'TRAIN', 'LE-FLIRT-2', 359.00, 'Pardubice, Olomouc'),
('PRG', 'OSV', '14:12:00', '17:32:00', 'TRAIN', 'LE-FLIRT-1', 389.00, 'Pardubice, Olomouc'),
('PRG', 'KRK', '16:10:00', '22:45:00', 'TRAIN', 'LE-FLIRT-2', 590.00, 'Olomouc, Ostrava, Katowice'),
('PRG', 'VIE', '08:00:00', '13:15:00', 'COACH', 'LC-BUS-1', 420.00, 'Brno'),
('PRG', 'BTS', '09:30:00', '14:00:00', 'COACH', 'LC-BUS-2', 350.00, 'Brno'),
('BTS', 'VIE', '10:00:00', '11:15:00', 'COACH', 'LC-BUS-1', 149.00, 'Bratislava Airport'),
('KRK', 'OSV', '11:30:00', '13:45:00', 'COACH', 'LC-BUS-2', 199.00, 'Katowice'),
('OSV', 'KSC', '09:40:00', '12:55:00', 'TRAIN', 'LE-FLIRT-1', 280.00, 'Žilina, Poprad-Tatry');

-- Seed Loyalty Accounts & Users
INSERT INTO loyalty_accounts (loyalty_number, points, tier) VALUES
('LE-1250', 1250, 'GOLD');

INSERT INTO users (email, password_hash, full_name, role, loyalty_account_id, preferred_language, currency) VALUES
('jan.novak@leoexpress.cz', '$2a$12$4mUfP50/BwZf6IqM1w7C8eGf8z2kF6xK.qF8iV8aXf6hO7gR8fT2S', 'Jan Novák', 'PASSENGER', 1, 'cs', 'CZK'),
('admin@leoexpress.com', '$2a$12$9pM.u8vY3gPqRzK7mN8xU.L3G8Xf9R2jD6oF5mS8aYf1hE7gR8qG9', 'System Admin', 'ADMIN', NULL, 'en', 'EUR');

-- Seed Saved Passengers
INSERT INTO saved_passengers (user_id, first_name, last_name, birth_date, discount_type) VALUES
(1, 'Jan', 'Novák', '1990-05-12', 'NONE'),
(1, 'Marie', 'Nováková', '1993-09-24', 'NONE');

-- Seed Saved Payment Method
INSERT INTO saved_payment_methods (user_id, card_holder, masked_number, expiry_date, card_type, token) VALUES
(1, 'Jan Novák', '**** **** **** 5183', '08/28', 'Visa', 'tok_simulated_leoexpress_983271');

-- Seed Bookings & Tickets
INSERT INTO bookings (id, user_id, status, total_price_czk) VALUES
('LE-482103-CZ', 1, 'ACTIVE', 329.00);

INSERT INTO tickets (booking_id, route_id, departure_date, seat_number, carriage_number, passenger_name, price_paid_czk, qr_code_data) VALUES
('LE-482103-CZ', 1, '2026-07-03', '24B', 'Car 3 (Premium)', 'Jan Novák', 329.00, 'LEOEXPRESS|LE-482103-CZ|PRG|OSV|2026-07-03|24B');
