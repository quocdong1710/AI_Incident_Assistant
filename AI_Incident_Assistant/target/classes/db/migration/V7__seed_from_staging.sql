DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'seed_import'
          AND table_name = 'engineer_staging'
    ) THEN
        INSERT INTO engineer (
            username,
            display_name,
            skills,
            active_incident_count,
            online,
            access_components,
            recent_assignments,
            team,
            status,
            p1_count
        )
        SELECT
            lower(trim(s.name)) AS username,
            trim(s.name) AS display_name,
            s.skills,
            COALESCE(s.active_issue_count, 0) AS active_incident_count,
            CASE
                WHEN lower(COALESCE(s.status, '')) IN ('busy', 'locked') THEN FALSE
                ELSE TRUE
            END AS online,
            REPLACE(COALESCE(s.skills, ''), ', ', ',') AS access_components,
            COALESCE(s.p1_count, 0) AS recent_assignments,
            s.team,
            lower(COALESCE(s.status, 'available')) AS status,
            COALESCE(s.p1_count, 0) AS p1_count
        FROM seed_import.engineer_staging s
        WHERE NULLIF(trim(s.name), '') IS NOT NULL
        ON CONFLICT (username) DO UPDATE
        SET
            display_name = EXCLUDED.display_name,
            skills = EXCLUDED.skills,
            active_incident_count = EXCLUDED.active_incident_count,
            online = EXCLUDED.online,
            access_components = EXCLUDED.access_components,
            recent_assignments = EXCLUDED.recent_assignments,
            team = EXCLUDED.team,
            status = EXCLUDED.status,
            p1_count = EXCLUDED.p1_count;

        DROP TABLE IF EXISTS engineer_staging;
    END IF;
END $$;
