CREATE TABLE POSACCT (
  ID NUMBER PRIMARY KEY NOT NULL,
  NAME VARCHAR2(60) NOT NULL,
  BALANCE NUMBER(11,2) NOT NULL,
  CREATE_TS DATE NOT NULL,
  UPDATE_TS DATE
);

CREATE SEQUENCE POSACCT_SEQ INCREMENT BY 1 START WITH 1 MINVALUE 0 CACHE 100;

