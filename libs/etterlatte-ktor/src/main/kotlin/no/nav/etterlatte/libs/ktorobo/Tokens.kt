package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

sealed class TokenRequest(
    open val scopes: List<String>,
)

data class OboTokenRequest(
    override val scopes: List<String>,
    val accessToken: String,
) : TokenRequest(scopes)

data class ClientCredentialsTokenRequest(override val scopes: List<String>) : TokenRequest(scopes)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessToken(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("token_type")
    val tokenType: String,
)
