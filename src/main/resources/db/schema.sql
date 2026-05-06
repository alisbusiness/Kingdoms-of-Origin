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
  phase TEXT,
  active_perks TEXT
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
  promised_perks TEXT,
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

CREATE TABLE IF NOT EXISTS ruler_trust (
  ruler_uuid TEXT PRIMARY KEY,
  trust_score INTEGER DEFAULT 50,
  updated_at INTEGER
);

CREATE TABLE IF NOT EXISTS promise_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ruler_uuid TEXT,
  election_id INTEGER,
  promised_perk TEXT,
  enacted_perks TEXT,
  honored INTEGER,
  created_at INTEGER
);

CREATE TABLE IF NOT EXISTS treasury_state (
  office_id TEXT PRIMARY KEY,
  raw_diamonds INTEGER DEFAULT 0,
  diamond_blocks INTEGER DEFAULT 0,
  currency_supply INTEGER DEFAULT 0,
  taxes_collected INTEGER DEFAULT 0,
  public_spending INTEGER DEFAULT 0,
  treasury_withdrawals INTEGER DEFAULT 0,
  emergency_minting INTEGER DEFAULT 0,
  xp_tax_rate INTEGER DEFAULT 0,
  trade_tax_rate INTEGER DEFAULT 0,
  resource_tithe_rate INTEGER DEFAULT 0,
  emergency_levy_rate INTEGER DEFAULT 0,
  legitimacy INTEGER DEFAULT 70,
  corruption_heat INTEGER DEFAULT 0,
  unrest INTEGER DEFAULT 0,
  revolt_active INTEGER DEFAULT 0,
  revolt_started_at INTEGER DEFAULT 0,
  capture_progress INTEGER DEFAULT 0,
  transition_freeze_until INTEGER DEFAULT 0,
  updated_at INTEGER
);

CREATE TABLE IF NOT EXISTS currency_balances (
  player_uuid TEXT PRIMARY KEY,
  balance INTEGER DEFAULT 0,
  updated_at INTEGER
);

CREATE TABLE IF NOT EXISTS revolt_participants (
  player_uuid TEXT PRIMARY KEY,
  side TEXT,
  joined_at INTEGER
);

CREATE TABLE IF NOT EXISTS treasury_ledger (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  office_id TEXT,
  actor_uuid TEXT,
  type TEXT,
  amount INTEGER,
  public INTEGER,
  note TEXT,
  created_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_elections_office_status
  ON elections (office_id, status, id DESC);

CREATE INDEX IF NOT EXISTS idx_candidates_election_player
  ON candidates (election_id, player_uuid);

CREATE INDEX IF NOT EXISTS idx_votes_election_voter
  ON votes (election_id, voter_uuid);

CREATE INDEX IF NOT EXISTS idx_votes_election_candidate
  ON votes (election_id, candidate_uuid);

CREATE INDEX IF NOT EXISTS idx_history_created_at
  ON history (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_treasury_ledger_office_public
  ON treasury_ledger (office_id, public, id DESC);
