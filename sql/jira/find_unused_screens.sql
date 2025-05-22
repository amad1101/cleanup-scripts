-- This script identifies Jira screens that are not currently part of any screen scheme.
-- Such screens might be unused and candidates for cleanup.
-- It selects the ID, name, and description of these screens.

SELECT
    fs.id AS screen_id,
    fs.name AS screen_name,
    fs.description AS screen_description
FROM
    public.fieldscreen fs -- Contains all screens
WHERE
    fs.id NOT IN (
        SELECT DISTINCT
            fssi.fieldscreen -- IDs of screens used in screen schemes
        FROM
            public.fieldscreenschemeitem fssi
        WHERE 
            fssi.fieldscreen IS NOT NULL -- Ensure we only consider valid screen IDs
    );

-- Note: 
-- The table names (`fieldscreen`, `fieldscreenschemeitem`) and schema (`public`) are standard for Jira PostgreSQL databases. 
-- Adjust if your Jira instance uses a different RDBMS or schema.
-- Always test SQL scripts in a non-production environment first.
