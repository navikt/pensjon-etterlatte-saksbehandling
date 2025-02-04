package no.nav.etterlatte.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnlag.OpplysningDao
import org.slf4j.LoggerFactory

class GrunnlagBackupKlient(
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val url = "http://etterlatte-grunnlag-backup"

    fun hentPersonGallerierFraBackup(opplysningsId: List<String>): HendelseWrapper {
        logger.info("Henter backup persongalleri for antall ${opplysningsId.size}")
        return try {
            return runBlocking {
                val response =
                    httpClient
                        .post("$url/grunnlag/hendelser") {
                            contentType(ContentType.Application.Json)
                            setBody(Lister(opplysningsId))
                        }
                return@runBlocking response.body()
            }
        } catch (e: Exception) {
            throw RuntimeException("Feilet mot grunnlag backup. Svar", e)
        }
    }
}

data class Lister(
    val ider: List<String>,
)

data class HendelseWrapper(
    val hendelser: List<OpplysningDao.GrunnlagHendelse>,
)
