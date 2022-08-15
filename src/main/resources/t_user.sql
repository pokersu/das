CREATE TABLE public.t_user (
       id int8 NOT NULL,
       "name" varchar(50) NULL,
       "password" varchar(50) NULL,
       email varchar(255) NULL,
       phone varchar(20) NULL,
       address varchar(255) NULL,
       remark varchar(255) NULL,
       status int4 NULL,
       last_login_ip varchar(128) NULL,
       last_login_time timestamp NULL,
       create_time timestamp NULL,
       update_time timestamp NULL,
       delete_time timestamp NULL,
       is_deleted int4 NULL,
       CONSTRAINT t_user_pk PRIMARY KEY (id)
);