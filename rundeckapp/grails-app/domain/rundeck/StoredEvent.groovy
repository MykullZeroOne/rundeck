package rundeck

import com.dtolabs.rundeck.core.event.Event
import grails.compiler.GrailsCompileStatic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.validation.constraints.Size
import org.rundeck.app.data.model.v1.storedevent.EventSeverity

@GrailsCompileStatic
@Entity
@Table(name = "stored_event", indexes = [
    @Index(name = "STORED_EVENT_IDX_PROJECT_NAME", columnList = "projectName"),
    @Index(name = "STORED_EVENT_IDX_SUBSYSTEM", columnList = "subsystem"),
    @Index(name = "STORED_EVENT_IDX_TOPIC", columnList = "topic"),
    @Index(name = "STORED_EVENT_IDX_OBJECT_ID", columnList = "objectId"),
    @Index(name = "STORED_EVENT_IDX_SEQUENCE", columnList = "sequence"),
    @Index(name = "STORED_EVENT_IDX_LAST_UPDATED", columnList = "lastUpdated"),
    @Index(name = "STORED_EVENT_IDX_PROJ_SUBSYS_TOPIC", columnList = "projectName,subsystem,topic"),
    @Index(name = "STORED_EVENT_IDX_PROJ_SUBSYS_TOPIC_LASTUPDATED", columnList = "projectName,subsystem,topic,lastUpdated")
])
class StoredEvent implements Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @Size(max = 36)
    String serverUUID

    @Enumerated(EnumType.ORDINAL)
    EventSeverity severity = EventSeverity.INFO

    @Size(max = 255)
    String projectName

    @Size(max = 128)
    String subsystem

    @Size(max = 255)
    String topic

    @Size(max = 64)
    @Column(nullable = true)
    String objectId

    Long sequence = 0L

    @Temporal(TemporalType.TIMESTAMP)
    Date lastUpdated

    int schemaVersion = 0

    @Lob
    @Column(nullable = true)
    String meta

    StoredEvent() {}

    StoredEvent(
        String serverUUID,
        String projectName,
        String subsystem,
        String topic,
        String objectId,
        Long sequence,
        String event
    ) {
        this.serverUUID = serverUUID
        this.projectName = projectName
        this.subsystem = subsystem
        this.topic = topic
        this.objectId = objectId
        this.sequence = sequence?:0
        this.meta = event
    }
}
