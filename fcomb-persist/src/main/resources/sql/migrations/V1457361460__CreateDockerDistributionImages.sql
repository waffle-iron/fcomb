CREATE TABLE docker_distribution_images (
  id serial primary key,
  name varchar(256) not null,
  user_id integer not null references users(id),
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

CREATE UNIQUE INDEX ON docker_distribution_images (lower(name));
