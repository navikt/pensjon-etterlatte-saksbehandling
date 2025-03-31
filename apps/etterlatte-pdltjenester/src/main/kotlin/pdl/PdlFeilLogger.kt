package no.nav.etterlatte.pdl

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.sikkerLogg
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface PdlDataErrorResponse<T> {
    val data: T?
    val errors: List<PdlResponseError>?
}

private val logger: Logger = LoggerFactory.getLogger("PdlDelvisDataLogger")

fun loggDelvisReturnerteData(
    result: PdlDataErrorResponse<*>,
    req: Any,
) {
    if (result.data != null) {
        val unauthorized = result.errors?.firstOrNull { it.extensions?.code == "unauthorized" }

        if (unauthorized != null) {
            logger.warn(
                "Saksbehandler har ikke tilgang til å se person: ${unauthorized.extensions?.details?.cause}",
            )
            throw ManglerTilgangTilPerson()
        }

        result.errors?.joinToString(",")?.let { feil ->
            logger.error(
                "Fikk data fra PDL, men også feil / mangler. Dette kan gjøre at saksbehandler får et " +
                    "ukomplett bilde av dataene i PDL, uten at vi indikerer dette." +
                    " Se sikkerlogg for feilmelding",
            )
            sikkerLogg.error("PDL feil $feil \n\n Request: $req")
        }
    }
}

/*
    Statuskoden her tolkes rett i frontend.
    401 vil si at man er utlogget så 403 er i compliance med nye etterlevelseskrav om at informasjonsflyt
    er lov for beskyttede personer.
 */
class ManglerTilgangTilPerson :
    ForespoerselException(
        status = HttpStatusCode.Forbidden.value,
        code = "MANGLER_TILGANG_TIL_PERSON",
        detail = "Mangler tilgang til person",
    )
