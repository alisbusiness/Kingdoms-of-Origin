CREATE TABLE IF NOT EXISTS players (
  uuid TEXT PRIMARY KEY,
  username TEXT,
  current_origin_id TEXT,
  previous_origin_id TEXT,
  first_join_origin_assigned INTEGER DEFAULT 0,
  created_at INTEGER,
  updated_at INTEGER
);

CREATE TABLE IF NOT EXISTS office_state (
  office_id TEXT PRIMARY KEY,
  holder_uuid TEXT,
  holder_origin_before_office TEXT,
  active_king_origin_id TEXT,
  term_started_at INTEGER,
  term_ends_at INTEGER,
  phase TEXT
);

CREATE TABLE IF NOT EXISTS elections (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  office_id TEXT,
  status TEXT,
  nomination_opens_at INTEGER,
  voting_opens_at INTEGER,
  voting_closes_at INTEGER,
  winner_uuid TEXT,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS candidates (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  election_id INTEGER,
  player_uuid TEXT,
  slogan TEXT,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS votes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  election_id INTEGER,
  voter_uuid TEXT,
  candidate_uuid TEXT,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  event_type TEXT,
  actor_uuid TEXT,
  target_uuid TEXT,
  payload_json TEXT,
  created_at INTEGER
);
