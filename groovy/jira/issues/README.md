Please, start to make clean from *schemes* and after elements

## Project Scripts

### `findInactiveProjects.groovy`

**Purpose:** Identifies Jira projects that have not had any issue activity (creations or updates) for a specified period.

**Usage:**
- The script iterates through all projects in your Jira instance.
- For each project, it checks the last updated date of its issues.
- If the most recent issue update is older than the `inactivityThresholdYears` (default is 1 year), the project is flagged as potentially inactive.
- If a project has no issues, its creation date is checked against the threshold.
- The script prints out the names and keys of projects identified as inactive.

**Configuration:**
- `isPreview = true`: Set to `false` if you intend to modify the script to perform actions (currently, it only logs information).
- `inactivityThresholdYears = 1`: Modify this value at the top of the script to change the period used to define inactivity (e.g., set to `2` for two years).

**Note:** This script is read-only and does not make any changes to your Jira data. It's intended to help administrators identify projects that might be candidates for archival or deletion.