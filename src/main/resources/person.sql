drop table if exists hs.public.person;

create table hs.public.person
(
    id serial constraint table_name_pk primary key,
    name    varchar(255),
    age     varchar(255),
    address varchar(255)
);

-- insert into hs.public.person(id,name,age,address)
-- values (1,'이경원','32','인천');
-- insert into hs.public.person(id,name,age,address)
-- values (2,'홍길동','30','서울');
-- insert into hs.public.person(id,name,age,address)
-- values (3,'아무개','25','강원');

