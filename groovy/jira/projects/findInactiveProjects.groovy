import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.jql.builder.JqlQueryBuilder
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import java.time.LocalDate
import java.time.ZoneId
// import java.time.temporal.ChronoUnit // Not strictly necessary for this logic

def isPreview = true
def inactivityThresholdYears = 1

log.info "Starting inactive project check (isPreview: ${isPreview}, inactivityThresholdYears: ${inactivityThresholdYears})"

try {
    def projectManager = ComponentAccessor.getProjectManager()
    def issueManager = ComponentAccessor.getIssueManager() // Not strictly needed if only using SearchService for issues
    def searchService = ComponentAccessor.getComponent(SearchService.class)
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

    if (!searchService) {
        log.error "SearchService could not be retrieved. Please check Jira configuration and plugin availability."
        return // Exit script if essential service is missing
    }
    if (!user) {
        log.warn "No logged-in user found by ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(). JQL searches might behave unexpectedly or fail. Ensure the script is run in a context where a user is authenticated."
        // Depending on Jira setup, some scripts might run as a system user or require a specific user context.
    }


    log.info "Iterating through all projects..."
    projectManager.getProjectObjects().each { project ->
        log.debug "-----------------------------------------------------"
        log.debug "Checking project: ${project.getName()} (Key: ${project.getKey()}, ID: ${project.getId()})"

        def jqlQueryBuilder = JqlQueryBuilder.newBuilder()
        jqlQueryBuilder.where().project(project.getId())
        jqlQueryBuilder.orderBy().updatedDate(com.atlassian.jira.jql.orderBy.SortOrder.DESC)
        def query = jqlQueryBuilder.buildQuery()

        // It's good practice to validate the query with the user context
        def parseResult = searchService.parseQuery(user, query.getQueryString())
        if (!parseResult.isValid()) {
            log.error "Invalid JQL query for project ${project.getName()} (Key: ${project.getKey()}): ${parseResult.getErrors().toString()}"
            return // continue to next project using 'return' in a closure
        }

        try {
            // Execute the search, requesting only 1 issue (the most recently updated)
            def searchResults = searchService.search(user, parseResult.getQuery(), PagerFilter.getLimitedFilter(1))
            def issues = searchResults.getResults()

            if (issues && !issues.isEmpty()) {
                def lastIssue = issues.first()
                // Convert Timestamp to LocalDate
                def lastUpdatedDate = lastIssue.getUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                def currentDate = LocalDate.now()
                def thresholdDate = currentDate.minusYears(inactivityThresholdYears)

                log.debug "Project: ${project.getName()}, Last issue: ${lastIssue.getKey()}, Last issue update: ${lastUpdatedDate}, Threshold date for inactivity: ${thresholdDate}"

                if (lastUpdatedDate.isBefore(thresholdDate)) {
                    log.warn "INACTIVE PROJECT FOUND: ${project.getName()} (Key: ${project.getKey()}). Last issue update: ${lastUpdatedDate} (Threshold: ${thresholdDate})"
                    if (!isPreview) {
                        log.info "ACTION (isPreview=false): Specific action for inactive project ${project.getName()} would be performed here."
                        // Example: projectArchivingService.archiveProject(project) or send notification
                    }
                } else {
                    log.info "Project ${project.getName()} (Key: ${project.getKey()}) is considered ACTIVE. Last issue update: ${lastUpdatedDate}"
                }
            } else {
                // No issues found in the project
                // Consider project creation date or other criteria if needed.
                // For now, log it as "no issues". This could mean it's inactive since creation or genuinely empty.
                def projectCreatedDate = project.getCreated()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                log.info "No issues found for project: ${project.getName()} (Key: ${project.getKey()}). Project created: ${projectCreatedDate ?: 'N/A'}."
                // Decide if "no issues" means "inactive" based on requirements.
                // If a project with no issues should be considered inactive if older than threshold:
                if (projectCreatedDate) {
                    def thresholdDate = LocalDate.now().minusYears(inactivityThresholdYears)
                    if (projectCreatedDate.isBefore(thresholdDate)) {
                         log.warn "INACTIVE PROJECT FOUND (no issues, created before threshold): ${project.getName()} (Key: ${project.getKey()}). Created: ${projectCreatedDate}"
                         if (!isPreview) {
                            log.info "ACTION (isPreview=false): Specific action for inactive project (no issues) ${project.getName()} would be performed here."
                        }
                    } else {
                        log.info "Project ${project.getName()} (Key: ${project.getKey()}) has no issues, but was created on/after ${thresholdDate}. Not considered inactive by creation date."
                    }
                } else {
                     log.warn "Project ${project.getName()} (Key: ${project.getKey()}) has no issues and no creation date available. Marking as potentially inactive."
                }
            }
        } catch (Exception searchEx) {
            log.error "Error searching issues for project ${project.getName()} (Key: ${project.getKey()}): ${searchEx.getMessage()}", searchEx
        }
    }
    log.debug "-----------------------------------------------------"
} catch (Exception e) {
    log.error "FATAL ERROR during inactive project check: ${e.getMessage()}", e
}

log.info "Inactive project check script finished."
