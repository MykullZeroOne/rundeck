/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData

@Entity
@Table(name = "log_file_storage_request")
class LogFileStorageRequest implements LogFileStorageRequestData{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @NotNull
    @OneToOne
    @JoinColumn(name = "execution_id", unique = true)
    Execution execution

    @Size(max = 255)
    String pluginName

    String filetype
    Boolean completed

    Date dateCreated
    Date lastUpdated

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

    @Override
    Serializable getExecutionId() {
        return execution.id
    }

    @Override
    String getExecutionUuid() {
        return execution.uuid
    }
}
