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
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.rundeck.app.data.model.v1.pluginMeta.RdPluginMeta

@Entity
@Table(name = "plugin_meta")
class PluginMeta implements RdPluginMeta{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    String key
    String project

    @Lob
    String jsonData

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

    @Transient
    public Map getPluginData() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    /**
     * store data under a key
     */
    public void storePluginData(String key, Object obj) {
        setPluginData(getPluginData().put(key, obj))
    }

    public void setPluginData(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }
}
