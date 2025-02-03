package no.nav.etterlatte.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnlag.OpplysningDao
import org.slf4j.LoggerFactory

class GrunnlagBackupKlient(
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val url = "http://etterlatte-grunnlag-backup"

    fun hentPersonGallerierFraBackup(opplysningsId: String): OpplysningDao.GrunnlagHendelse {
        logger.info("Henter backup persongalleri for id: $opplysningsId")
        try {
            runBlocking {
                val response =
                    httpClient
                        .get("$url/grunnlag/$opplysningsId") {
                            contentType(ContentType.Application.Json)
                        }
                if (response.status.isSuccess()) {
                    return@runBlocking response.body()
                } else {
                    logger.error("Fikk feil fra backup body: ${response.bodyAsText()}")
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Feilet mot grunnlag backup. Svar", e)
        }
    }
}
