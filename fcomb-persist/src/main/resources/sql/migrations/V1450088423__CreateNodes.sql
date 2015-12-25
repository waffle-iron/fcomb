CREATE TYPE node_state AS ENUM ('initializing', 'available', 'unreachable', 'upgrading',
  'terminating', 'terminated', 'deleted');

CREATE TABLE nodes (
  id serial NOT NULL,
  state node_state NOT NULL,
  token varchar(255) NOT NULL,
  root_certificate_id integer NOT NULL REFERENCES user_certificates(id),
  signed_certificate bytea NOT NULL,
  public_key_hash varchar(64) NOT NULL,
  user_id integer NOT NULL REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX on nodes (user_id, public_key_hash);
