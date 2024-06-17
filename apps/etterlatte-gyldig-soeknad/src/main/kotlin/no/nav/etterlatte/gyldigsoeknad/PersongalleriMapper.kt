package no.nav.etterlatte.gyldigsoeknad

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import org.slf4j.LoggerFactory

object PersongalleriMapper {
    private val logger = LoggerFactory.getLogger(PersongalleriMapper::class.java)

    fun hentPersongalleriFraSoeknad(soeknad: InnsendtSoeknad): Persongalleri {
        logger.info("Hent persongalleri fra søknad")

        return when (soeknad) {
            is Omstillingsstoenad -> opprettPersongalleriOMS(soeknad)
            is Barnepensjon -> opprettPersongalleriBP(soeknad)
            else -> {
                sikkerlogger().error(
                    "Kunne ikke opprette persongalleri for søknad tilhørende fnr=${soeknad.soeker.foedselsnummer?.svar?.value}",
                )
                throw Exception("Ukjent type søknad ${soeknad.type}")
            }
        }
    }

    private fun opprettPersongalleriOMS(soeknad: Omstillingsstoenad): Persongalleri =
        Persongalleri(
            soeker = soeknad.soeker.foedselsnummer.svar.value,
            innsender = soeknad.innsender.foedselsnummer.svar.value,
            avdoed =
                listOfNotNull(
                    soeknad.avdoed.foedselsnummer
                        ?.svar
                        ?.value,
                ),
            soesken = soeknad.barn.mapNotNull { it.foedselsnummer?.svar?.value },
        )

    private fun opprettPersongalleriBP(soeknad: Barnepensjon): Persongalleri {
        logger.info("Hent persongalleri fra søknad")

        val soekerFnr =
            checkNotNull(
                soeknad.soeker.foedselsnummer
                    ?.svar
                    ?.value,
            ) {
                "Kan ikke opprette persongalleri når søker sitt fnr. mangler!"
            }

        return Persongalleri(
            soeker = soekerFnr,
            innsender = soeknad.innsender.foedselsnummer.svar.value,
            soesken = soeknad.soesken.mapNotNull { it.foedselsnummer?.svar?.value },
            avdoed = soeknad.foreldre.filter { it.type == PersonType.AVDOED }.mapNotNull { it.foedselsnummer?.svar?.value },
            gjenlevende =
                soeknad.foreldre
                    .filter { it.type == PersonType.GJENLEVENDE_FORELDER }
                    .mapNotNull { it.foedselsnummer?.svar?.value },
        )
    }
}
