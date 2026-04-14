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

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil

@Entity
@Table(name = "storage", indexes = [
    @Index(name = "STORAGE_IDX_NAMESPACE", columnList = "namespace")
])
class Storage implements RundeckStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @Size(max = 255)
    @Column(nullable = true)
    String namespace

    @Size(max = 2048)
    @Column(nullable = true)
    String dir

    @NotBlank
    @Size(max = 1024)
    @Column(nullable = false)
    String name

    /**
     * json encoded metadata
     */
    @Lob
    @Column(nullable = true)
    String jsonData

    /**
     * Unique sha1 of namespace+dir+name to prevent duplicate
     */
    @NotBlank
    @Size(min = 40, max = 40)
    @Column(nullable = false, unique = true)
    String pathSha

    @Lob
    @Column(nullable = true)
    byte[] data

    @Temporal(TemporalType.TIMESTAMP)
    Date dateCreated

    @Temporal(TemporalType.TIMESTAMP)
    Date lastUpdated

    private void setupSha() {
        dir = dir ?: ''
        pathSha = ((namespace ?: '') + ':' + getPath().path).encodeAsSHA1()
    }

    @PrePersist
    def beforeInsert() {
        setupSha()
    }

    @PreUpdate
    def beforeUpdate() {
        setupSha()
    }

    @Transient
    public Path getPath() {
        return PathUtil.asPath((dir?(dir+'/'):'')+name)
    }

    public void setPath(String path){
        def path1 = PathUtil.asPath(path)
        def parent = PathUtil.parentPath(path1)
        dir= parent?parent.path:''
        name=path1.name
    }

    @Transient
    public Map getStorageMeta() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    public void setStorageMeta(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }

}
