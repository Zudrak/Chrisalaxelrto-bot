create table message_replies
(
    id         bigint auto_increment not null,
    trigger_id bigint  not null,
    message    varchar not null,
    constraint trigger_id_key
        foreign key (trigger_id) references message_triggers (id)
);

create unique index id_index
    on message_replies (id);