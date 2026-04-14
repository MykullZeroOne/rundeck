package rundeck

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.model.v1.execution.RdJobStats

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version

@Entity
@Table(name = "scheduled_execution_stats")
class ScheduledExecutionStats implements RdJobStats {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @Lob
    String content

    String jobUuid

    @Version
    Long version = 0L

    @Transient
    Map getContentMap() {
        if (null != content) {
            final ObjectMapper objMapper = new ObjectMapper()
            try{
                return objMapper.readValue(content, Map.class)
            }catch (JsonParseException e){
                return null
            }
        } else {
            return null
        }

    }


    void setContentMap(Map obj) {
        if (null != obj) {
            final ObjectMapper objMapper = new ObjectMapper()
            content = objMapper.writeValueAsString(obj)
        } else {
            content = null
        }
    }

    Long getVersion(){
        version
    }

}
