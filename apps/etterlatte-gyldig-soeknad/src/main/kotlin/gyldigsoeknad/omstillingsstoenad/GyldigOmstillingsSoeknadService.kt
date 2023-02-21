package no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad

import no.nav.etterlatte.gyldigsoeknad.client.PdlClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.soeknad.dataklasser.omstillingsstoenad.Omstillingsstoenad
import java.time.LocalDateTime

class GyldigOmstillingsSoeknadService(private val pdlClient: PdlClient) {
    fun hentPersongalleriFraSoeknad(soeknad: Omstillingsstoenad): Persongalleri {
        // TODO Må tilpasse persongalleri eller bruke noe annet?
        return Persongalleri(
            soeker = soeknad.soeker.foedselsnummer.svar.value,
            innsender = soeknad.innsender.foedselsnummer.svar.value,
            avdoed = listOf(soeknad.avdoed.foedselsnummer.svar.value),
            soesken = soeknad.barn.map { it.foedselsnummer.svar.value }
        )
    }

    fun vurderGyldighet(personGalleri: Persongalleri): GyldighetsResultat {
        // TODO EY-1776 Implementer vurdering av gyldig søknad
        return GyldighetsResultat(VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
    }
}