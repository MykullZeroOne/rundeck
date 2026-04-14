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

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "base_report", indexes = [
    @Index(name = "EXEC_REPORT_IDX_0", columnList = "ctx_project,dateCompleted"),
    @Index(name = "EXEC_REPORT_IDX_1", columnList = "ctx_project"),
    @Index(name = "BASE_REPORT_IDX_2", columnList = "ctx_project,dateCompleted,dateStarted")
])
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("rundeck.BaseReport")
class BaseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @Column(nullable = true)
    String node

    @Lob
    String title

    @NotNull
    @Size(max = 256)
    @Column(nullable = false)
    String status

    @Deprecated
    @NotNull
    @Size(max = 256)
    @Column(nullable = false)
    String actionType

    @Column(name = "ctx_project")
    String project

    @Deprecated
    @Column(nullable = true)
    String ctxType

    @Deprecated
    @Column(nullable = true)
    String ctxName

    @Deprecated
    @Column(nullable = true)
    String maprefUri

    @Size(max = 3072)
    @Column(nullable = true)
    String reportId

    @Column(nullable = true)
    String tags

    String author

    @Temporal(TemporalType.TIMESTAMP)
    Date dateStarted

    @Temporal(TemporalType.TIMESTAMP)
    Date dateCompleted

    @Lob
    String message

    public static final ArrayList<String> exportProps = [
            'node',
            'title',
            'status',
            'actionType',
            'project',
            'reportId',
            'tags',
            'author',
            'message',
            'dateStarted',
            'dateCompleted'
    ]

    @Transient
    def Map toMap(){
        def map=this.properties.subMap(exportProps)
        if(map.status=='timeout'){
            map.status='timedout'
        }
        if(map.actionType=='timeout'){
            map.actionType='timedout'
        }
        map
    }

    static buildFromMap(BaseReport obj, Map data) {
        data.each { k, v ->
            if ((k == 'status' || k == 'actionType')) {
                if (v == 'timedout') {
                    //XXX: use 'timeout' internally for timedout status, due to previous varchar(7) length limitations on
                    // the field :-Σ
                    v = 'timeout'
                }else if (v == 'succeeded') {
                    v='succeed'
                }else if (v.toString().length()>7) {
                    v='other'
                }
            }
            obj[k] = v
        }
    }

    static BaseReport fromMap(Map data) {
        def BaseReport report = new BaseReport()
        buildFromMap(report, data.subMap(exportProps))
        report
    }
}
