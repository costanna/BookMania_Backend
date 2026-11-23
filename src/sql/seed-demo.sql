-- =====================================================================
-- BookMania - datos de demostración
-- =====================================================================
-- 4 categorías, 37 libros en castellano (Brandon Sanderson, Harry Potter,
-- Sarah J. Maas, Canción de hielo y fuego, Joe Abercrombie) con portada de
-- Apple Books, 6 lectores ficticios, préstamos (activos, vencido, con
-- prórrogas, devueltos), reservas (pendientes, cumplida, cancelada,
-- caducada) y multas.
--
-- Uso: SOLO sobre una base vacía, después de que el backend haya arrancado
-- una vez (Hibernate crea las tablas). Ejecutar en una sola transacción:
--
--   psql "$DATABASE_URL" -1 -v ON_ERROR_STOP=1 -f src/sql/seed-demo.sql
--
-- - Usa ids explícitos: si ya hay datos, falla por clave duplicada y, con
--   -1, no se guarda nada.
-- - Las fechas son relativas a CURRENT_DATE: el estado de préstamos y
--   reservas (vence mañana, vencido...) tiene sentido el día de la carga.
-- - Los lectores (@example.com) tienen una contraseña aleatoria cifrada con
--   bcrypt (pgcrypto): nadie puede iniciar sesión con ellos.
-- - El administrador no se crea aquí: regístrate en la app y luego
--   UPDATE users SET role = 'ADMIN' WHERE email = '...';
-- - Copias disponibles = totales - préstamos activos (ISSUED/OVERDUE), y
--   solo hay reservas pendientes en libros agotados, igual que en la app.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO categories (id, name, description) VALUES
  (1, 'Fantasía épica', 'Grandes sagas con mundos extensos, sistemas de magia y tramas corales.'),
  (2, 'Fantasía oscura', 'Grimdark: personajes moralmente ambiguos, violencia y poca épica heroica.'),
  (3, 'Fantasía juvenil', 'Aventuras de iniciación para lectores jóvenes (y no tan jóvenes).'),
  (4, 'Fantasía romántica', 'Romantasy: la historia de amor pesa tanto como la magia y la guerra.');
INSERT INTO books (id, title, author, isbn, pages, publish_year, cover_url, total_copies, available_copies, created_at) VALUES
  (1, 'El imperio final', 'Brandon Sanderson', '9788490192955', NULL, 2006, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/46/46/cc/4646cc37-41cd-7f2a-b88e-937db4b4356e/9788490192955.jpg/600x600bb.jpg', 3, 3, (CURRENT_DATE - 119)::timestamp + time '10:30'),
  (2, 'El Pozo de la Ascensión', 'Brandon Sanderson', '9788490192252', NULL, 2007, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/07/76/46/07764661-3f1a-0ec8-db6f-d965712287fa/9788490192252.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 118)::timestamp + time '10:30'),
  (3, 'El Héroe de las Eras', 'Brandon Sanderson', '9788490193884', NULL, 2008, 'https://is1-ssl.mzstatic.com/image/thumb/Publication221/v4/47/eb/c2/47ebc2cf-f4d0-c898-f733-186a0afff95b/9788490193884.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 117)::timestamp + time '10:30'),
  (4, 'Aleación de ley', 'Brandon Sanderson', '9788490192214', NULL, 2011, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/f9/5e/e2/f95ee287-9281-4c07-42d6-5f74bfc93999/9788490192214.jpg/600x600bb.jpg', 2, 1, (CURRENT_DATE - 116)::timestamp + time '10:30'),
  (5, 'El camino de los reyes', 'Brandon Sanderson', '9788490691779', NULL, 2010, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/82/06/18/820618dd-83d4-d368-cc77-1e7af0c7687d/9788490691779.jpg/600x600bb.jpg', 2, 0, (CURRENT_DATE - 115)::timestamp + time '10:30'),
  (6, 'Palabras radiantes', 'Brandon Sanderson', '9788490691786', NULL, 2014, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/21/cb/f9/21cbf9f5-14e4-ea7c-16a6-0523197bce4e/9788490691786.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 114)::timestamp + time '10:30'),
  (7, 'Juramentada', 'Brandon Sanderson', '9788417347154', NULL, 2017, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/bc/66/92/bc669225-1de9-e758-0441-7977bbcf4751/9788417347154.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 113)::timestamp + time '10:30'),
  (8, 'El ritmo de la guerra', 'Brandon Sanderson', '9788417347963', NULL, 2020, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/59/75/30/5975307b-9939-dc18-c28b-6116dce1898f/9788417347963.jpg/600x600bb.jpg', 2, 1, (CURRENT_DATE - 112)::timestamp + time '10:30'),
  (9, 'Elantris', 'Brandon Sanderson', '9788490694107', NULL, 2005, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/07/ab/97/07ab971f-590b-6b42-ea16-f39ea592429b/9788490694107.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 111)::timestamp + time '10:30'),
  (10, 'Trenza del mar Esmeralda', 'Brandon Sanderson', '9788418037825', NULL, 2023, 'https://is1-ssl.mzstatic.com/image/thumb/Publication126/v4/85/4a/31/854a31e9-94f4-d274-521b-62d37b5e8ec5/9788418037825.jpg/600x600bb.jpg', 3, 3, (CURRENT_DATE - 110)::timestamp + time '10:30'),
  (11, 'Harry Potter y la piedra filosofal', 'J. K. Rowling', '9781781106051', NULL, 1997, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/a0/70/21/a07021f4-a3e7-f62f-40c3-38864cef8360/9781781106051.jpg/600x600bb.jpg', 4, 3, (CURRENT_DATE - 109)::timestamp + time '10:30'),
  (12, 'Harry Potter y la cámara secreta', 'J. K. Rowling', '9781781106068', NULL, 1998, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/58/d2/aa/58d2aa35-ba35-839b-a916-b91ccf405e22/9781781106068.jpg/600x600bb.jpg', 3, 3, (CURRENT_DATE - 108)::timestamp + time '10:30'),
  (13, 'Harry Potter y el prisionero de Azkaban', 'J. K. Rowling', '9781781106075', NULL, 1999, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/ea/95/7a/ea957a40-eda6-0fea-abc7-60672a90853d/9781781106075.jpg/600x600bb.jpg', 3, 2, (CURRENT_DATE - 107)::timestamp + time '10:30'),
  (14, 'Harry Potter y el cáliz de fuego', 'J. K. Rowling', '9781781106082', NULL, 2000, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/bc/a5/17/bca51761-ad6d-c690-7624-ee83aaf63d95/9781781106082.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 106)::timestamp + time '10:30'),
  (15, 'Harry Potter y la Orden del Fénix', 'J. K. Rowling', '9781781106099', NULL, 2003, 'https://is1-ssl.mzstatic.com/image/thumb/Publication221/v4/c3/c7/1c/c3c71cf0-4a53-3598-5386-cdbafd443504/9781781106099.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 105)::timestamp + time '10:30'),
  (16, 'Harry Potter y el misterio del príncipe', 'J. K. Rowling', '9781781106105', NULL, 2005, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/de/6f/25/de6f2525-6850-670d-1676-5b9bbaf45e80/9781781106105.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 104)::timestamp + time '10:30'),
  (17, 'Harry Potter y las Reliquias de la Muerte', 'J. K. Rowling', '9781781106112', NULL, 2007, 'https://is1-ssl.mzstatic.com/image/thumb/Publication221/v4/1b/28/3d/1b283d19-bb16-e5e8-cad9-b66a1f564274/9781781106112.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 103)::timestamp + time '10:30'),
  (18, 'Una corte de rosas y espinas', 'Sarah J. Maas', '9788408156987', NULL, 2015, 'https://is1-ssl.mzstatic.com/image/thumb/Publication122/v4/b1/1e/4c/b11e4cc5-79ba-2fc2-a7db-24e55fe2d831/9788408156987.jpg/600x600bb.jpg', 3, 0, (CURRENT_DATE - 102)::timestamp + time '10:30'),
  (19, 'Una corte de niebla y furia', 'Sarah J. Maas', '9788408174349', NULL, 2016, 'https://is1-ssl.mzstatic.com/image/thumb/Publication112/v4/6e/63/af/6e63aff2-ffe1-179e-9948-54f8e1ec712f/9788408174349.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 101)::timestamp + time '10:30'),
  (20, 'Una corte de alas y ruina', 'Sarah J. Maas', '9788408179344', NULL, 2017, 'https://is1-ssl.mzstatic.com/image/thumb/Publication112/v4/f1/5a/18/f15a18fb-0e08-ce5b-3401-c69589d57577/9788408179344.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 100)::timestamp + time '10:30'),
  (21, 'Una corte de llamas plateadas', 'Sarah J. Maas', '9788408250081', NULL, 2021, 'https://is1-ssl.mzstatic.com/image/thumb/Publication115/v4/c8/85/9f/c8859fc7-f269-83d3-bcaa-5ffa7519371e/9788408250081.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 99)::timestamp + time '10:30'),
  (22, 'Casa de tierra y sangre', 'Sarah J. Maas', '9786073846097', NULL, 2020, 'https://is1-ssl.mzstatic.com/image/thumb/Publication221/v4/29/87/03/298703dc-785c-c808-f46e-504e51a1cdf6/9786073846097.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 98)::timestamp + time '10:30'),
  (23, 'Casa de cielo y aliento', 'Sarah J. Maas', '9786073846103', NULL, 2022, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/1b/6b/9f/1b6b9fb0-ca77-1a0b-d190-0eb5675d4a88/9786073846103.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 97)::timestamp + time '10:30'),
  (24, 'Casa de llama y sombra', 'Sarah J. Maas', '9786073844642', NULL, 2024, 'https://is1-ssl.mzstatic.com/image/thumb/Publication122/v4/09/48/de/0948de49-7e1d-a98f-e29f-6a851e6a4659/9786073844642.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 96)::timestamp + time '10:30'),
  (25, 'Juego de tronos', 'George R. R. Martin', '9788401031595', NULL, 1996, 'https://is1-ssl.mzstatic.com/image/thumb/Publication126/v4/7b/84/4e/7b844e33-4018-440f-88aa-e4eaf4d74acb/9788401031595.jpg/600x600bb.jpg', 3, 0, (CURRENT_DATE - 95)::timestamp + time '10:30'),
  (26, 'Choque de reyes', 'George R. R. Martin', '9788401031786', NULL, 1998, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/23/a9/c3/23a9c34a-20cc-98a2-3fbd-12395be0d257/9788401031786.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 94)::timestamp + time '10:30'),
  (27, 'Tormenta de espadas', 'George R. R. Martin', '9788401032615', NULL, 2000, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/9c/e4/e2/9ce4e2a1-07ed-338f-d8c8-fd1243fea9b2/9788401032615.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 93)::timestamp + time '10:30'),
  (28, 'Festín de cuervos', 'George R. R. Martin', '9788401032622', NULL, 2005, 'https://is1-ssl.mzstatic.com/image/thumb/Publication126/v4/4d/aa/49/4daa4980-f512-a6c6-8e94-a44b9040cd52/9788401032622.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 92)::timestamp + time '10:30'),
  (29, 'Danza de dragones', 'George R. R. Martin', '9788401032639', NULL, 2011, 'https://is1-ssl.mzstatic.com/image/thumb/Publication116/v4/72/a9/10/72a910ea-d029-2209-d39e-20f2f871c00d/9788401032639.jpg/600x600bb.jpg', 2, 1, (CURRENT_DATE - 91)::timestamp + time '10:30'),
  (30, 'El caballero de los Siete Reinos', 'George R. R. Martin', '9788401036880', NULL, 2015, 'https://is1-ssl.mzstatic.com/image/thumb/Publication221/v4/ba/90/fc/ba90fcdd-7e30-a74d-7c4a-e0d75c41a397/9788401036880.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 90)::timestamp + time '10:30'),
  (31, 'La voz de las espadas', 'Joe Abercrombie', '9788420609959', NULL, 2006, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/be/96/d5/be96d51f-7e3a-87f0-14a4-cc5a6c829182/9788420609959.jpg/600x600bb.jpg', 1, 0, (CURRENT_DATE - 89)::timestamp + time '10:30'),
  (32, 'Antes de que los cuelguen', 'Joe Abercrombie', '9788420609966', NULL, 2007, 'https://is1-ssl.mzstatic.com/image/thumb/Publication221/v4/83/bb/5c/83bb5cb3-4c6e-f17c-d11c-5fa3f6784363/9788420609966.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 88)::timestamp + time '10:30'),
  (33, 'El último argumento de los reyes', 'Joe Abercrombie', '9788420609973', NULL, 2008, 'https://is1-ssl.mzstatic.com/image/thumb/Publication118/v4/b9/b9/db/b9b9db1f-1082-9c96-84bf-6beecf1bd45b/9788420609973.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 87)::timestamp + time '10:30'),
  (34, 'La mejor venganza', 'Joe Abercrombie', '9788411487122', NULL, 2009, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/31/54/c6/3154c642-7dba-0004-bbfe-e271af40c586/9788411487122.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 86)::timestamp + time '10:30'),
  (35, 'Los héroes', 'Joe Abercrombie', '9788411488327', NULL, 2011, 'https://is1-ssl.mzstatic.com/image/thumb/Publication211/v4/8b/73/e4/8b73e46e-b069-6925-7e13-9195dd0ed48e/9788411488327.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 85)::timestamp + time '10:30'),
  (36, 'Tierras rojas', 'Joe Abercrombie', '9788420682914', NULL, 2012, 'https://is1-ssl.mzstatic.com/image/thumb/Publication113/v4/56/52/28/565228f2-ed5a-06e5-2012-5a5ae84a5f72/9788420682914.jpg/600x600bb.jpg', 1, 1, (CURRENT_DATE - 84)::timestamp + time '10:30'),
  (37, 'Un poco de odio', 'Joe Abercrombie', '9788491818045', NULL, 2019, 'https://is1-ssl.mzstatic.com/image/thumb/Publication113/v4/42/86/0c/42860ca7-d531-c6aa-ae83-d3705dcf0d41/9788491818045.jpg/600x600bb.jpg', 2, 2, (CURRENT_DATE - 83)::timestamp + time '10:30');
INSERT INTO book_categories (book_id, category_id) VALUES
  (1, 1),
  (2, 1),
  (3, 1),
  (4, 1),
  (5, 1),
  (6, 1),
  (7, 1),
  (8, 1),
  (9, 1),
  (10, 1),
  (11, 3),
  (12, 3),
  (13, 3),
  (14, 3),
  (15, 3),
  (16, 3),
  (17, 3),
  (18, 4),
  (19, 4),
  (20, 4),
  (21, 4),
  (22, 4),
  (23, 4),
  (24, 4),
  (25, 1),
  (25, 2),
  (26, 1),
  (26, 2),
  (27, 1),
  (27, 2),
  (28, 1),
  (28, 2),
  (29, 1),
  (29, 2),
  (30, 1),
  (31, 2),
  (32, 2),
  (33, 2),
  (34, 2),
  (35, 2),
  (36, 2),
  (37, 2);
INSERT INTO users (id, name, email, password, role, member_since, active, penalty_until, created_at) VALUES
  (1, 'Lucía Fernández', 'lucia.fernandez@example.com', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'USER', CURRENT_DATE - 200, true, NULL, (CURRENT_DATE - 200)::timestamp + time '10:30'),
  (2, 'Marcos Ruiz', 'marcos.ruiz@example.com', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'USER', CURRENT_DATE - 180, true, CURRENT_DATE + 23, (CURRENT_DATE - 180)::timestamp + time '10:30'),
  (3, 'Carmen Ortega', 'carmen.ortega@example.com', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'USER', CURRENT_DATE - 160, true, NULL, (CURRENT_DATE - 160)::timestamp + time '10:30'),
  (4, 'Javier Molina', 'javier.molina@example.com', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'USER', CURRENT_DATE - 150, true, NULL, (CURRENT_DATE - 150)::timestamp + time '10:30'),
  (5, 'Elena Navarro', 'elena.navarro@example.com', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'USER', CURRENT_DATE - 140, true, CURRENT_DATE - 39, (CURRENT_DATE - 140)::timestamp + time '10:30'),
  (6, 'Pablo Serrano', 'pablo.serrano@example.com', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'USER', CURRENT_DATE - 120, true, NULL, (CURRENT_DATE - 120)::timestamp + time '10:30');
INSERT INTO loans (id, user_id, book_id, issue_date, due_date, return_date, extensions_used, status, created_at) VALUES
  (1, 1, 5, CURRENT_DATE - 12, CURRENT_DATE + 9, NULL, 0, 'ISSUED', (CURRENT_DATE - 12)::timestamp + time '10:30'),
  (2, 1, 18, CURRENT_DATE - 19, CURRENT_DATE + 2, NULL, 0, 'ISSUED', (CURRENT_DATE - 19)::timestamp + time '10:30'),
  (3, 1, 4, CURRENT_DATE - 25, CURRENT_DATE + 6, NULL, 1, 'ISSUED', (CURRENT_DATE - 25)::timestamp + time '10:30'),
  (4, 1, 11, CURRENT_DATE - 70, CURRENT_DATE - 49, CURRENT_DATE - 52, 0, 'RETURNED', (CURRENT_DATE - 70)::timestamp + time '10:30'),
  (5, 2, 25, CURRENT_DATE - 30, CURRENT_DATE - 9, NULL, 0, 'OVERDUE', (CURRENT_DATE - 30)::timestamp + time '10:30'),
  (6, 2, 27, CURRENT_DATE - 90, CURRENT_DATE - 69, CURRENT_DATE - 60, 0, 'RETURNED', (CURRENT_DATE - 90)::timestamp + time '10:30'),
  (7, 4, 5, CURRENT_DATE - 6, CURRENT_DATE + 15, NULL, 0, 'ISSUED', (CURRENT_DATE - 6)::timestamp + time '10:30'),
  (8, 4, 25, CURRENT_DATE - 15, CURRENT_DATE + 6, NULL, 0, 'ISSUED', (CURRENT_DATE - 15)::timestamp + time '10:30'),
  (9, 4, 1, CURRENT_DATE - 40, CURRENT_DATE - 19, CURRENT_DATE - 20, 0, 'RETURNED', (CURRENT_DATE - 40)::timestamp + time '10:30'),
  (10, 5, 25, CURRENT_DATE - 3, CURRENT_DATE + 18, NULL, 0, 'ISSUED', (CURRENT_DATE - 3)::timestamp + time '10:30'),
  (11, 5, 18, CURRENT_DATE - 8, CURRENT_DATE + 13, NULL, 0, 'ISSUED', (CURRENT_DATE - 8)::timestamp + time '10:30'),
  (12, 5, 6, CURRENT_DATE - 100, CURRENT_DATE - 79, CURRENT_DATE - 74, 0, 'RETURNED', (CURRENT_DATE - 100)::timestamp + time '10:30'),
  (13, 3, 18, CURRENT_DATE - 1, CURRENT_DATE + 20, NULL, 0, 'ISSUED', (CURRENT_DATE - 1)::timestamp + time '10:30'),
  (14, 3, 12, CURRENT_DATE - 50, CURRENT_DATE - 29, CURRENT_DATE - 35, 0, 'RETURNED', (CURRENT_DATE - 50)::timestamp + time '10:30'),
  (15, 3, 29, CURRENT_DATE - 20, CURRENT_DATE + 1, NULL, 0, 'ISSUED', (CURRENT_DATE - 20)::timestamp + time '10:30'),
  (16, 3, 8, CURRENT_DATE - 40, CURRENT_DATE + 11, NULL, 3, 'ISSUED', (CURRENT_DATE - 40)::timestamp + time '10:30'),
  (17, 6, 31, CURRENT_DATE - 10, CURRENT_DATE + 11, NULL, 0, 'ISSUED', (CURRENT_DATE - 10)::timestamp + time '10:30'),
  (18, 6, 29, CURRENT_DATE - 22, CURRENT_DATE - 1, CURRENT_DATE - 1, 0, 'RETURNED', (CURRENT_DATE - 22)::timestamp + time '10:30'),
  (19, 6, 11, CURRENT_DATE - 4, CURRENT_DATE + 17, NULL, 0, 'ISSUED', (CURRENT_DATE - 4)::timestamp + time '10:30'),
  (20, 4, 13, CURRENT_DATE - 9, CURRENT_DATE + 12, NULL, 0, 'ISSUED', (CURRENT_DATE - 9)::timestamp + time '10:30');
INSERT INTO reservations (id, user_id, book_id, queue_position, reservation_date, expiry_date, status, created_at) VALUES
  (1, 3, 5, 1, CURRENT_DATE - 5, NULL, 'PENDING', (CURRENT_DATE - 5)::timestamp + time '10:30'),
  (2, 6, 5, 2, CURRENT_DATE - 2, NULL, 'PENDING', (CURRENT_DATE - 2)::timestamp + time '10:30'),
  (3, 6, 25, 1, CURRENT_DATE - 7, NULL, 'PENDING', (CURRENT_DATE - 7)::timestamp + time '10:30'),
  (4, 1, 25, 2, CURRENT_DATE - 6, NULL, 'PENDING', (CURRENT_DATE - 6)::timestamp + time '10:30'),
  (5, 3, 25, 3, CURRENT_DATE - 2, NULL, 'PENDING', (CURRENT_DATE - 2)::timestamp + time '10:30'),
  (6, 4, 18, 1, CURRENT_DATE - 1, NULL, 'PENDING', (CURRENT_DATE - 1)::timestamp + time '10:30'),
  (7, 4, 31, 1, CURRENT_DATE - 4, NULL, 'PENDING', (CURRENT_DATE - 4)::timestamp + time '10:30'),
  (8, 1, 29, 1, CURRENT_DATE - 10, CURRENT_DATE + 2, 'PENDING', (CURRENT_DATE - 10)::timestamp + time '10:30'),
  (9, 4, 1, 1, CURRENT_DATE - 45, NULL, 'FULFILLED', (CURRENT_DATE - 45)::timestamp + time '10:30'),
  (10, 5, 26, 1, CURRENT_DATE - 30, NULL, 'CANCELLED', (CURRENT_DATE - 30)::timestamp + time '10:30'),
  (11, 6, 7, 1, CURRENT_DATE - 40, CURRENT_DATE - 33, 'EXPIRED', (CURRENT_DATE - 40)::timestamp + time '10:30');
INSERT INTO fines (id, loan_id, user_id, days_overdue, penalty_days, penalty_until, created_at) VALUES
  (1, 5, 2, 1, 31, CURRENT_DATE + 23, (CURRENT_DATE - 8)::timestamp + time '10:30'),
  (2, 6, 2, 9, 39, CURRENT_DATE - 21, (CURRENT_DATE - 60)::timestamp + time '10:30'),
  (3, 12, 5, 5, 35, CURRENT_DATE - 39, (CURRENT_DATE - 74)::timestamp + time '10:30');
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT max(id) FROM categories));
SELECT setval(pg_get_serial_sequence('books', 'id'), (SELECT max(id) FROM books));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('loans', 'id'), (SELECT max(id) FROM loans));
SELECT setval(pg_get_serial_sequence('reservations', 'id'), (SELECT max(id) FROM reservations));
SELECT setval(pg_get_serial_sequence('fines', 'id'), (SELECT max(id) FROM fines));
