/* 
	# 
	# Bases de Dados 2020/2021
	# Projecto
	#
*/


/* 
Estes comandos drop (comentados) permitem remover as tabelas (se ja' tiverem sido criadas anteriormente)

drop table customer;
drop table bid;
drop table auction_product;
drop table message;
drop table updates;
*/

CREATE TABLE customer (
	user_id	 BIGINT,
	username VARCHAR(512) UNIQUE NOT NULL,
	password VARCHAR(512) NOT NULL,
	email	 VARCHAR(512) UNIQUE NOT NULL,
	PRIMARY KEY(user_id)
);

CREATE TABLE bid (
	bid_id		 BIGINT,
	price		 FLOAT(8) NOT NULL,
	bid_time		 TIMESTAMP NOT NULL,
	auction_product_id BIGINT NOT NULL,
	customer_user_id	 BIGINT NOT NULL,
	PRIMARY KEY(bid_id)
);

CREATE TABLE auction_product (
	id		 BIGINT,
	start_price	 FLOAT(8) NOT NULL,
	end_time		 TIMESTAMP NOT NULL,
	title		 VARCHAR(512) NOT NULL,
	description	 VARCHAR(512) NOT NULL,
	product_id 	 BIGINT UNIQUE NOT NULL,
	product_name	 VARCHAR(512) NOT NULL,
	customer_user_id	 BIGINT NOT NULL,
	expired		 BOOL NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE message (
	message_id	 BIGINT,
	message		 VARCHAR(512) NOT NULL,
	message_time	 TIMESTAMP NOT NULL,
	customer_user_id	 BIGINT NOT NULL,
	auction_product_id BIGINT NOT NULL,
	PRIMARY KEY(message_id)
);

CREATE TABLE updates (
	update_id		 BIGINT UNIQUE,
	auction_product_id BIGINT NOT NULL,
	start_price	 FLOAT(8) NOT NULL,
	end_time		 TIMESTAMP NOT NULL,
	title		 VARCHAR(512) NOT NULL,
	description	 VARCHAR(512) NOT NULL,
	PRIMARY KEY(update_id)
);

ALTER TABLE bid ADD CONSTRAINT bid_fk1 FOREIGN KEY (auction_product_id) REFERENCES auction_product(id);
ALTER TABLE bid ADD CONSTRAINT bid_fk2 FOREIGN KEY (customer_user_id) REFERENCES customer(user_id);
ALTER TABLE auction_product ADD CONSTRAINT auction_product_fk1 FOREIGN KEY (customer_user_id) REFERENCES customer(user_id);
ALTER TABLE message ADD CONSTRAINT message_fk1 FOREIGN KEY (customer_user_id) REFERENCES customer(user_id);
ALTER TABLE message ADD CONSTRAINT message_fk2 FOREIGN KEY (auction_product_id) REFERENCES auction_product(id);
ALTER TABLE updates ADD CONSTRAINT updates_fk1 FOREIGN KEY (auction_product_id) REFERENCES auction_product(id);



INSERT INTO customer VALUES (1, 'inserido1', 'pass', 'inserido1@gmail.com');
INSERT INTO customer VALUES (2, 'joaquina', 'pass', 'joaquina@gmail.com');
INSERT INTO customer VALUES (3, 'senhorJoao', 'pass', 'senhorjoao@gmail.com');
INSERT INTO customer VALUES (4, 'pessoa 1', 'pass', 'pessoa1@gmail.com');
INSERT INTO customer VALUES (5, 'pessoa 2', 'pass', 'pessoa2@gmail.com');
INSERT INTO customer VALUES (6, 'pessoa 3', 'pass', 'pessoa3@gmail.com');
INSERT INTO customer VALUES (7, 'pessoa 4', 'pass', 'pessoa4@gmail.com');
INSERT INTO customer VALUES (8, 'alex', 'pass', 'alex@gmail.com');

INSERT INTO auction_product VALUES (1, 50, '2021-06-02 10:23:54','Relogio da avo', 'Muito Velho', 9781, 'Relogio', 2, false);
INSERT INTO auction_product VALUES (2, 20, '2021-06-05 20:00:00','Mochila Vermelha', 'Bom estado', 9782, 'Mochila', 3, false);
INSERT INTO auction_product VALUES (3, 10, '2021-06-08 23:59:00','Rubiks cube', 'GANS', 9783, 'Cube', 7, false);

INSERT INTO bid VALUES (1, 21, '2021-06-03 14:00:00', 2, 4);
INSERT INTO bid VALUES (2, 22, '2021-06-03 15:00:00', 2, 2);
INSERT INTO bid VALUES (3, 51, '2021-06-01 08:45:00', 1, 4);




INSERT INTO message VALUES (1, 'Mensagem 1', '2021-05-28 11:30:00', 1 , 1);
INSERT INTO message VALUES (2, 'Mensagem 2', '2021-05-28 11:40:00', 3 , 1);
INSERT INTO message VALUES (3, 'Mochila feia', '2021-05-28 09:20:00', 1 , 2);