create table message_triggers
(
    id       bigint  not null,
    guild_id bigint  not null,
    regex    varchar not null,
    constraint guild_id_key
        foreign key (guild_id) references guild_properties (guild_id)
);

create unique index message_trigger_index
    on message_triggers (id);