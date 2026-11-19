CREATE TABLE IF NOT EXISTS ZIP(
                                  ZIP VARCHAR(10) PRIMARY KEY ,
                                  City_NAME Varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS Address (
                                       Address_ID INT PRIMARY KEY ,
                                       Zip varchar(10) NOT NULL REFERENCES ZIP(ZIP),
                                       Road_name varchar(100) NOT NULL ,
                                       House_number varchar(10) NOT NULL ,
                                       number_of_floors int NOT NULL
);

CREATE TABLE IF NOT EXISTS Sensor (
                                      Sensor_ID INT PRIMARY KEY ,
                                      type varchar(25) NOT NULL,
                                      Address INT REFERENCES Address(Address_ID) NOT NULL
);

-- Seed data: the 10 simulated MQTT sensors (simulators/sensors.json, ids 1-10)
-- spread over 3 buildings.

INSERT INTO ZIP (ZIP, City_NAME) VALUES
    ('01001', 'Kyiv'),
    ('01030', 'Kyiv'),
    ('61000', 'Kharkiv');

INSERT INTO Address (Address_ID, Zip, Road_name, House_number, number_of_floors) VALUES
    (1, '01001', 'Khreshchatyk Street',   '22', 9),
    (2, '01030', 'Volodymyrska Street',   '40', 5),
    (3, '61000', 'Sumska Street',         '15', 12);

-- type matches the "type" the dashboard API reports for these sensors.
INSERT INTO Sensor (Sensor_ID, type, Address) VALUES
    (1,  'accelerometer', 1),
    (2,  'accelerometer', 1),
    (3,  'accelerometer', 1),
    (4,  'accelerometer', 1),
    (5,  'accelerometer', 2),
    (6,  'accelerometer', 2),
    (7,  'accelerometer', 2),
    (8,  'accelerometer', 3),
    (9,  'accelerometer', 3),
    (10, 'accelerometer', 3);


