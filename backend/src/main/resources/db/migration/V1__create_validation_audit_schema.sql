create table data_sources (
    id uuid primary key,
    code varchar(120) not null unique,
    display_name varchar(200) not null,
    schema_version varchar(40) not null,
    active boolean not null
);

create table validation_runs (
    id uuid primary key,
    data_source_id uuid not null references data_sources(id),
    original_filename varchar(255) not null,
    status varchar(40) not null,
    outcome varchar(40) not null,
    max_severity varchar(40),
    total_rows integer not null,
    valid_rows integer not null,
    invalid_rows integer not null,
    issue_count integer not null,
    submitted_at timestamptz not null,
    completed_at timestamptz not null
);

create table quarantined_records (
    id uuid primary key,
    validation_run_id uuid not null references validation_runs(id) on delete cascade,
    row_number integer not null,
    raw_record jsonb not null,
    created_at timestamptz not null,
    unique (validation_run_id, row_number)
);

create table validation_issues (
    id uuid primary key,
    validation_run_id uuid not null references validation_runs(id) on delete cascade,
    quarantined_record_id uuid references quarantined_records(id) on delete set null,
    row_number integer,
    category varchar(80) not null,
    code varchar(120) not null,
    field_name varchar(120),
    severity varchar(40) not null,
    message varchar(500) not null,
    created_at timestamptz not null
);

create index idx_validation_runs_submitted_at on validation_runs (submitted_at desc);
create index idx_validation_issues_run_id on validation_issues (validation_run_id);
create index idx_quarantined_records_run_id on quarantined_records (validation_run_id);

insert into data_sources (id, code, display_name, schema_version, active)
values (
    '11111111-1111-1111-1111-111111111111',
    'transaction-events',
    'Transaction Events',
    'v1',
    true
);
