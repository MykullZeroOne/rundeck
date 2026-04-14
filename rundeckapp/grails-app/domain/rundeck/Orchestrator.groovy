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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank

/**
 * Represents a registration of a orchestrator and configuration
 */
@Entity
@Table(name = "orchestrator")
class Orchestrator implements OrchestratorData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    /**
     * type is the type of orchestrator to initiate, e.g. "limitRun"
     */
    @NotBlank
    String type

    /**
     * content contains data to use for the orchestrator, e.g. a percentage
     */
    @Lob
    String content

    @Transient
    Map getConfiguration() {
        //de-serialize the json
        if (null != content) {
            final ObjectMapper mapper = new ObjectMapper()
            try{
                return mapper.readValue(content, Map.class)
            }catch (JsonParseException e){
                return null
            }
        } else {
            return null
        }

    }

    void setConfiguration(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            content = mapper.writeValueAsString(obj)
        } else {
            content = null
        }
    }

    static Orchestrator fromMap(Map data){
        Orchestrator n = new Orchestrator(type:data.type)
        n.configuration=data.configuration instanceof Map?data.configuration:[:]
        return n;
    }
    Map toMap(){
        if(type){
            def data=[type:type]
            if(this.configuration){
                data.configuration=this.configuration
            }
            return data
        }else{
            return null
        }
    }


    String toString ( ) {
        return "Orchestrator{" +
        "type='" + type + '\'' +
        ", content='" + content + '\'' +
        '}' ;
    }

}
