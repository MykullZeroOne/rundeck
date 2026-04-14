package rundeck

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "job_file_record")
class JobFileRecord {
    /**
     * Original file name
     */
    public static final String STATE_TEMP = 'temp'
    public static final String STATE_DELETED = 'deleted'
    public static final String STATE_EXPIRED = 'expired'
    public static final String STATE_RETAINED = 'retained'
    /**
     * State changes [to: [from,from]]
     */
    @Transient
    public static final Map<String, List<String>> STATES = [
            (STATE_EXPIRED) : [STATE_TEMP],
            (STATE_RETAINED): [STATE_TEMP],
            (STATE_DELETED) : [STATE_TEMP, STATE_RETAINED]
    ]

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @Size(max = 1024)
    String fileName

    /**
     * Data size
     */
    @NotNull
    Long size

    /**
     * The type of file record
     */
    @NotBlank
    @Size(max = 255)
    String recordType

    /**
     * Name used for the file record (e.g. option name)
     */
    @Size(max = 255)
    String recordName

    @NotBlank
    @Size(max = 255)
    String user

    Date dateCreated
    Date lastUpdated
    Date expirationDate

    /**
     * State of the file,
     * 'temp' = The file is uploaded but not yet used, -> expired, retained, deleted
     * 'deleted' = The file was deleted
     * 'expired' = The file was removed after expiration
     * 'retained' = The file is available and was retained, -> deleted
     */
    @NotBlank
    @Size(max = 255)
    String fileState

    @NotBlank
    String uuid

    String serverNodeUUID

    @NotBlank
    @Size(min = 64, max = 64)
    String sha

    @NotBlank
    String jobId

    @NotBlank
    @Size(max = 255)
    String storageType // storage plugin type "tmpdir", "storage"

    @NotBlank
    @Lob
    String storageReference // path in storage facility, or temp dir, depends on backend plugin

    @Lob
    String storageMeta // metadata...?

    @NotBlank
    String project

    @ManyToOne
    @JoinColumn(name = "execution_id")
    Execution execution

    @PrePersist
    void onPrePersist() {
        Date now = new Date()
        if (dateCreated == null) {
            dateCreated = now
        }
        lastUpdated = now
    }

    @PreUpdate
    void onPreUpdate() {
        lastUpdated = new Date()
    }

    boolean stateIsExpired() {
        fileState == STATE_EXPIRED
    }

    boolean stateIsTemp() {
        fileState == STATE_TEMP
    }

    boolean stateIsRetained() {
        fileState == STATE_RETAINED
    }

    boolean stateIsDeleted() {
        fileState == STATE_DELETED
    }

    public void stateExpired() {
        state(STATE_EXPIRED)
    }

    public void stateRetained() {
        state(STATE_RETAINED)
    }
    public boolean canBecomeRetained() {
        state(STATE_RETAINED,true)
    }

    public void stateDeleted() {
        state(STATE_DELETED)
    }

    public boolean state(String toState, boolean test=false) {
        changeState(STATES[toState], toState,test)
    }

    private boolean changeState(List<String> fromStates, String toState, boolean test=false) {
        if (!(fromStates && fromStates.contains(fileState) || fileState == toState)) {
            if(test){
                return false
            }
            throw new IllegalStateException("Cannot change to '$toState' state from $fileState")
        }
        if(test){
            return true
        }
        fileState = toState
        true
    }

    @Override
    public String toString() {
        return "rundeck.JobFileRecord{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", size=" + size +
                ", recordType='" + recordType + '\'' +
                ", recordName='" + recordName + '\'' +
                ", user='" + user + '\'' +
                ", dateCreated=" + dateCreated +
                ", expirationDate=" + expirationDate +
                ", fileState='" + fileState + '\'' +
                ", uuid='" + uuid + '\'' +
                ", serverNodeUUID='" + serverNodeUUID + '\'' +
                ", jobId='" + jobId + '\'' +
                ", execution.id=" + execution?.id +
                "} " + super.toString();
    }

    /**
     * Exported form for api
     */

    Map exportMap() {
        [
                id        : uuid,
                execId    : execution?.id,
                optionName: recordName
        ] + properties.subMap([
                'jobId',
                'fileName',
                'sha',
                'size',
                'dateCreated',
                'expirationDate',
                'user',
                'fileState',
                'serverNodeUUID',
        ]
        )
    }

    def toMap() {
        [execId: execution?.id] + properties.subMap(
                [
                        'uuid',
                        'recordName',
                        'jobId',
                        'fileName',
                        'sha',
                        'size',
                        'dateCreated',
                        'lastUpdated',
                        'expirationDate',
                        'user',
                        'fileState',
                        'storageReference',
                        'storageType',
                        'storageMeta',
                        'serverNodeUUID',
                        'recordType',
                        'project'
                ]
        )
    }

    static JobFileRecord fromMap(Map map) {
        new JobFileRecord(
                uuid: map.uuid,
                recordName: map.recordName,
                jobId: map.jobId,
                fileName: map.fileName,
                sha: map.sha,
                size: map.size,
                dateCreated: map.dateCreated,
                lastUpdated: map.lastUpdated,
                expirationDate: map.expirationDate,
                user: map.user,
                fileState: map.fileState,
                storageReference: map.storageReference,
                storageType: map.storageType,
                storageMeta: map.storageMeta,
                serverNodeUUID: map.serverNodeUUID,
                execution: map.execution,
                recordType: map.recordType,
                project: map.project
        )
    }
}
