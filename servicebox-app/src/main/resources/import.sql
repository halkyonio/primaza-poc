INSERT INTO claim(id, name, description, owner, serviceRequested, status, created) VALUES (nextval('hibernate_sequence'), 'mysql-demo',           'Quarkus JET Mysql demo',                 'quarkus team',               'mysql-7.5',        'new',      PARSEDATETIME('2022-10-18 10:30:00','yyyy-MM-dd HH:mm:ss'));
INSERT INTO claim(id, name, description, owner, serviceRequested, status, created) VALUES (nextval('hibernate_sequence'), 'postgresql-team-dev',  'Spring Boot Config Server PostgreSQL',   'snowdrop team',              'postgresql-11.5',  'pending',  PARSEDATETIME('2022-10-18 08:22:00','yyyy-MM-dd HH:mm:ss'));
INSERT INTO claim(id, name, description, owner, serviceRequested, status, created) VALUES (nextval('hibernate_sequence'), 'postgresql-team-test', 'Invoicing EAP Testing',                  'EAP QE team',                'postgresql-11.5',  'rejected', PARSEDATETIME('2022-10-18 11:00:00','yyyy-MM-dd HH:mm:ss'));
INSERT INTO claim(id, name, description, owner, serviceRequested, status, created) VALUES (nextval('hibernate_sequence'), 'mariadb-demo ',        'Dummy Java Maria DB ',                   'snowdrop r&d team',          'mariadb-7.5',      'bind',     PARSEDATETIME('2022-10-18 14:45:10','yyyy-MM-dd HH:mm:ss'));
INSERT INTO claim(id, name, description, owner, serviceRequested, status, created) VALUES (nextval('hibernate_sequence'), 'postgresql-13 ',       'Quarkus Primaza & Postgresql',           'snwodrop & devtools teams',  'postgresql-13.5',  'bind',     PARSEDATETIME('2022-10-18 20:02:01','yyyy-MM-dd HH:mm:ss'));

INSERT INTO service(id, name, version, endpoint, deployed) VALUES (1, 'MYSQL','8.0.3', 'tcp:3306', true);
INSERT INTO service(id, name, version, endpoint, deployed) VALUES (2, 'PostgreSQL','11.5','tcp:5432', false);
INSERT INTO service(id, name, version, endpoint, deployed) VALUES (3, 'ActiveMQ Artemis','2.26', 'tcp:8161', false);
INSERT INTO service(id, name, version, endpoint, deployed) VALUES (4, 'PaymentAPI','1.1','http:8080', true);