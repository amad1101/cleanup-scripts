# Jira SQL Scripts

This directory contains SQL scripts for querying Jira database for administrative, cleanup, or reporting purposes.

**Important:**
- Always test these scripts on a non-production Jira instance first.
- Understand the script's purpose before running it.
- These scripts are provided as-is and do not modify data unless explicitly stated (most are SELECT queries).
- Table and column names might vary slightly based on your Jira version or specific database (e.g., PostgreSQL, MySQL, SQL Server). The scripts are generally written with PostgreSQL syntax in mind.

---

## `find_unused_screens.sql`

**Purpose:**
This script identifies Jira screens that are not currently associated with any screen scheme. Screens that are not part of a screen scheme are generally not usable within Jira projects (for create, edit, or view operations) and might be candidates for cleanup.

**Output Columns:**
- `screen_id`: The ID of the screen.
- `screen_name`: The name of the screen.
- `screen_description`: The description of the screen.

**Notes:**
- This is a read-only script (SELECT query) and does not make any changes to your Jira data.
- It assumes the standard Jira table names: `fieldscreen` for screen definitions and `fieldscreenschemeitem` for the items within screen schemes.
