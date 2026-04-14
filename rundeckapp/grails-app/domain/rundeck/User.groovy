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
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.validation.constraints.Pattern
import org.rundeck.app.data.model.v1.user.RdUser

@Entity
@Table(name = "rduser")
class User implements RdUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @Pattern(regexp = '^[a-zA-Z0-9\\p{L}\\p{M}\\.,@\\(\\)\\s_\\\\\'/-]+$')
    @Column(nullable = false)
    String login

    @Column(nullable = true)
    String password

    @Column(nullable = true)
    String firstName

    @Column(nullable = true)
    String lastName

    @Column(nullable = true)
    String email

    @Temporal(TemporalType.TIMESTAMP)
    Date dateCreated

    @Temporal(TemporalType.TIMESTAMP)
    Date lastUpdated

    @Column(nullable = true)
    String dashboardPref

    @Column(nullable = true)
    String filterPref

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    Date lastLogin

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    Date lastLogout

    @Column(nullable = true)
    String lastSessionId

    @Column(nullable = true)
    String lastLoggedHostName

    @OneToMany(mappedBy = "user")
    Set<AuthToken> authTokens

    @Override
    Long getId() {
        return this.@id
    }
}
