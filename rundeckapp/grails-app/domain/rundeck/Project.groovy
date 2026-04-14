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

import org.rundeck.app.data.model.v1.project.RdProject

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Created by greg on 2/19/15.
 */
@Entity
@Table(name = "project",
       indexes = [@Index(name = "PROJECT_IDX_NAME", columnList = "name")])
class Project implements RdProject {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @NotNull
    @Pattern(regexp = '^[a-zA-Z0-9\\.,@\\(\\)_\\\\/-]+$')
    @Column(unique = true)
    String name

    @Size(max = 255)
    String description

    @Temporal(TemporalType.TIMESTAMP)
    Date dateCreated

    @Temporal(TemporalType.TIMESTAMP)
    Date lastUpdated

    @Enumerated(EnumType.STRING)
    State state

    @PrePersist
    void onCreate() {
        Date now = new Date()
        if (dateCreated == null) {
            dateCreated = now
        }
        if (lastUpdated == null) {
            lastUpdated = now
        }
    }

    @PreUpdate
    void onUpdate() {
        lastUpdated = new Date()
    }
}
