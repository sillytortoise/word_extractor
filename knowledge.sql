use knowledge;
create table field(
	uid varchar(50),
    domain varchar(50),
    seed varchar(200),
    primary key(uid,domain)
);

create table `user`(
	uid varchar(50) primary key,
    passwd varchar(200)
);
