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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import org.rundeck.app.data.model.v1.authtoken.AuthTokenType
import org.rundeck.app.data.model.v1.authtoken.AuthenticationToken

import java.time.Clock

@Entity
@Table(name = "auth_token", indexes = [
    @Index(name = "IDX_TOKEN", columnList = "token"),
    @Index(name = "IDX_TYPE", columnList = "type")
])
class AuthToken implements AuthenticationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    @NotNull
    @Column(nullable = false, unique = true)
    String token

    @Lob
    @NotNull
    @Column(nullable = false)
    String authRoles

    @Column(nullable = true)
    String uuid

    @Column(nullable = true)
    String creator

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    Date expiration

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    Date dateCreated

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    Date lastUpdated

    @Column(nullable = true)
    String name

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    AuthTokenType type = AuthTokenType.USER

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    AuthTokenMode tokenMode = AuthTokenMode.SECURED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user

    @Transient
    private transient String clearToken = null

    @PrePersist
    def beforeInsert() {
        encodeToken()
    }

    /**
     * Encodes the token value according to the tokenMode set.
     */
    private void encodeToken() {
        this.clearToken = token
        this.tokenMode = this.tokenMode ?: AuthTokenMode.LEGACY
        this.token = encodeTokenValue(this.clearToken, this.tokenMode)
    }

    /**
     * Encodes a clear token value according to the tokenMode supplied.
     */
    public static String encodeTokenValue(String clearValue, AuthTokenMode mode){
        if(!clearValue)
            throw new IllegalArgumentException("Illegal token value supplied: " + clearValue)

        switch (mode) {
            case AuthTokenMode.SECURED:
                return clearValue.encodeAsSHA256()

            case AuthTokenMode.LEGACY:
                return clearValue

            default:
                return clearValue
        }
    }

    @Transient
    String getClearToken() {
        return clearToken
    }

    @Override
    @Transient
    Set<String> getAuthRolesSet() {
        return parseAuthRoles(authRoles)
    }

    static String generateAuthRoles(Collection<String> roles) {
        new HashSet(roles.collect { it.trim() }.findAll { it }).join(',')
    }

    static Set<String> parseAuthRoles(String authRoles) {
        if (!authRoles) {
            return []
        }
        new HashSet(authRoles.split(' *, *').collect { it.trim() }.findAll { it } as List)
    }

    boolean tokenIsExpired() {
        expiration!=null && (expiration < Date.from(Clock.systemUTC().instant()))
    }

    /**
     * @return Printable value for token: the uuid, or a truncated token value
     */
    @Transient
    String getPrintableToken() {
        uuid ? "[ID: $uuid]" : (printable(token))
    }

    /**
     * @return Printable truncated token value
     */
    static String printable(String authtoken) {
        (authtoken.size() > 5 ? authtoken.substring(0, 5) : '') + "****"
    }

    @Override
    String toString() {
        "Auth Token: ${printableToken}"
    }

    @Override
    @Transient
    String getOwnerName() {
        return user.login
    }

}
