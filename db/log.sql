create table log(
    id bigint not null auto_increment primary key,
    log_type int not null,
    content varchar(1023) not null
) engine=InnoDB default charset=utf8mb4, row_format=DYNAMIC;
