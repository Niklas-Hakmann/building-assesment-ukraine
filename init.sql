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
                                      Address INT REFERENCES Address(Address_ID)
);
