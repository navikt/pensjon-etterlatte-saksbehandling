package no.nav.etterlatte

data class PostgresProperties(
    val databaseName: String,
    val host: String,
    val firstMappedPort: Int,
    val username: String,
    val password: String,
)
