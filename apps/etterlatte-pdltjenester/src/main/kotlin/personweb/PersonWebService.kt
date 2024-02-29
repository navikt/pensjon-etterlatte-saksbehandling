package no.nav.etterlatte.personweb

import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonNavn
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.PersonMapper
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

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
        foedselsnummer: Folkeregisteridentifikator,
        bruker: BrukerTokenInfo,
    ): PersonNavn {
        logger.info("Henter navn for fnr=$foedselsnummer fra PDL")

        return pdlOboKlient.hentPersonNavn(foedselsnummer.value, bruker).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString()

                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen $foedselsnummer")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=$foedselsnummer fra PDL: $pdlFeil",
                    )
                }
            } else {
                PersonMapper.mapPersonNavn(
                    ppsKlient = ppsKlient,
                    fnr = foedselsnummer,
                    hentPerson = it.data.hentPerson,
                )
            }
        }
    }

    private fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}
