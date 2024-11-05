package no.nav.etterlatte.pdl

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
        result.errors?.joinToString(",")?.let { feil ->
            val stackTrace = Exception()
            logger.error(
                "Fikk data fra PDL, men også feil / mangler. Dette kan gjøre at saksbehandler får et " +
                    "ukomplett bilde av dataene i PDL, uten at vi indikerer dette." +
                    " Se sikkerlogg for feilmelding",
                stackTrace,
            )
            sikkerLogg.error("Request: $req \n PDL feil $feil", stackTrace)
        }
    }
}
