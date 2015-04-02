
DROP DATABASE IF EXISTS hibernate;
CREATE DATABASE hibernate;
USE hibernate;

CREATE TABLE author (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE greeting (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  content varchar(255) DEFAULT NULL,
  date datetime DEFAULT NULL,
  author_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  KEY fk_author (author_id),
  CONSTRAINT fk_author FOREIGN KEY (author_id) REFERENCES author (id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
