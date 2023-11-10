package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import org.slf4j.LoggerFactory

class GyldigSoeknadService() {
    private val logger = LoggerFactory.getLogger(GyldigSoeknadService::class.java)

    fun hentPersongalleriFraSoeknad(soeknad: Barnepensjon): Persongalleri {
        logger.info("Hent persongalleri fra søknad")

        return Persongalleri(
            soeker = soeknad.soeker.foedselsnummer.svar.value,
            innsender = soeknad.innsender.foedselsnummer.svar.value,
            soesken = soeknad.soesken.map { it.foedselsnummer.svar.value },
            avdoed = soeknad.foreldre.filter { it.type == PersonType.AVDOED }.map { it.foedselsnummer.svar.value },
            gjenlevende =
                soeknad.foreldre.filter { it.type == PersonType.GJENLEVENDE_FORELDER }
                    .map { it.foedselsnummer.svar.value },
        )
    }
}
