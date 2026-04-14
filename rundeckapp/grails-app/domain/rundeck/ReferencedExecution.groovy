package rundeck

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution
import org.rundeck.app.data.model.v1.job.JobDataSummary

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "referenced_execution",
       indexes = [@Index(name = "REFEXEC_IDX_JOBUUID", columnList = "job_uuid,status")])
class ReferencedExecution implements RdReferencedExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    String jobUuid
    String status

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    @NotNull
    Execution execution

    static List<JobDataSummary> parentJobSummaries(String jobUuid, int max = 0){
        // Grails 7/Hibernate 6: Use HQL for LEFT OUTER JOIN - most reliable for complex queries
        // Fallback to criteria for DataTest compatibility
        try {
            String hql = '''
                SELECT DISTINCT e.scheduledExecution
                FROM ReferencedExecution re
                LEFT JOIN re.execution e
                WHERE re.jobUuid = :jobUuid
                AND e.scheduledExecution IS NOT NULL
            '''
            def results = executeQuery(hql, [jobUuid: jobUuid])
            if (max > 0 && results.size() > max) {
                results = results[0..<max]
            }
            return results*.toJobDataSummary()
        } catch (UnsupportedOperationException e) {
            // DataTest fallback: Load objects and extract scheduledExecution values
            def results = findAllByJobUuid(jobUuid, [max: max > 0 ? max : null])
            return results*.execution*.scheduledExecution.findAll { it != null }.unique()*.toJobDataSummary()
        }
    }

    static List<String> executionProjectList(String jobUuid, int max = 0){
        // Grails 7/Hibernate 6: Use HQL for LEFT OUTER JOIN - most reliable for complex queries
        // Fallback to criteria for DataTest compatibility
        try {
            String hql = '''
                SELECT DISTINCT e.project
                FROM ReferencedExecution re
                LEFT JOIN re.execution e
                WHERE re.jobUuid = :jobUuid
            '''
            def results = executeQuery(hql, [jobUuid: jobUuid])
            if (max > 0 && results.size() > max) {
                results = results[0..<max]
            }
            return results as List<String>
        } catch (UnsupportedOperationException e) {
            // DataTest fallback: Load objects and extract project values
            def results = findAllByJobUuid(jobUuid, [max: max > 0 ? max : null])
            return results*.execution*.project.findAll { it != null }.unique() as List<String>
        }
    }

    Serializable getExecutionId(){
        execution.id
    }
}
