package no.nav.etterlatte.personweb

import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.PersonMapper
import org.slf4j.LoggerFactory
import personweb.dto.PersonNavn

class PdlForesporselFeilet(message: String) : ForespoerselException(
    status = 500,
    code = "UKJENT_FEIL_PDL",
    detail = message,
)

class PersonWebService(
    private val pdlOboKlient: PdlOboKlient,
    private val ppsKlient: ParallelleSannheterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentPersonNavn(
        ident: String,
        bruker: BrukerTokenInfo,
    ): PersonNavn {
        logger.info("Henter navn for ident=${ident.maskerFnr()} fra PDL")

        return pdlOboKlient.hentPersonNavn(ident, bruker).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString()

                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke person i PDL")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med ident=${ident.maskerFnr()} fra PDL: $pdlFeil",
                    )
                }
            } else {
                PersonMapper.mapPersonNavn(
                    ppsKlient = ppsKlient,
                    ident = ident,
                    hentPerson = it.data.hentPerson,
                )
            }
        }
    }

    private fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}
