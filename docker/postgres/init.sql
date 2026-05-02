CREATE SCHEMA IF NOT EXISTS seed_import;

CREATE TABLE IF NOT EXISTS seed_import.engineer_staging (
    user_id VARCHAR(255),
    name VARCHAR(255),
    group_id VARCHAR(255),
    group_name VARCHAR(255),
    role VARCHAR(255),
    team VARCHAR(255),
    skills VARCHAR(255),
    status VARCHAR(255),
    active_issue_count INTEGER,
    p1_count INTEGER,
    current_issue_key VARCHAR(255),
    current_issue_summary VARCHAR(1000),
    shift VARCHAR(255),
    timezone VARCHAR(255),
    locked_reason VARCHAR(1000)
);

TRUNCATE TABLE seed_import.engineer_staging;

COPY seed_import.engineer_staging (
    user_id,
    name,
    group_id,
    group_name,
    role,
    team,
    skills,
    status,
    active_issue_count,
    p1_count,
    current_issue_key,
    current_issue_summary,
    shift,
    timezone,
    locked_reason
)
FROM '/docker-entrypoint-initdb.d/team_members.csv'
WITH (
    FORMAT csv,
    HEADER true,
    DELIMITER ',',
    ENCODING 'UTF8'
);
