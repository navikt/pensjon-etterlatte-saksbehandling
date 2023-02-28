package no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad

import no.nav.etterlatte.gyldigsoeknad.client.PdlClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderErGjenlevende
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoMedSiviltilstand
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.tidspunkt.standardTidssoneUTC
import java.time.Clock
import java.time.LocalDateTime

class GyldigOmstillingsSoeknadService(
    private val pdlClient: PdlClient,
    private val clock: Clock = Clock.system(standardTidssoneUTC)
) {
    fun hentPersongalleriFraSoeknad(soeknad: Omstillingsstoenad): Persongalleri {
        // TODO Må tilpasse persongalleri eller bruke noe annet?
        return Persongalleri(
            soeker = soeknad.soeker.foedselsnummer.svar.value,
            innsender = soeknad.innsender.foedselsnummer.svar.value,
            avdoed = listOf(soeknad.avdoed.foedselsnummer.svar.value),
            soesken = soeknad.barn.map { it.foedselsnummer.svar.value }
        )
    }

    fun vurderGyldighet(innsender: String?, avdoed: List<String>): GyldighetsResultat {
        val innsenderGyldighet = innsender?.let {
            hentInnsenderMedNavn(it)
        }
        val avdoedGyldighet = avdoed.map {
            hentAvdoedMedSiviltilstand(it)
        }

        val vurderinger = listOf(
            innsenderErGjenlevende(innsenderGyldighet, avdoedGyldighet)
        )

        val resultat =
            if (vurderinger.any { it.resultat == VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING }) {
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
            } else if (vurderinger.any { it.resultat == VurderingsResultat.IKKE_OPPFYLT }) {
                VurderingsResultat.IKKE_OPPFYLT
            } else {
                VurderingsResultat.OPPFYLT
            }

        return GyldighetsResultat(
            resultat,
            vurderinger,
            LocalDateTime.ofInstant(clock.instant(), standardTidssoneUTC)
        )
    }

    private fun hentInnsenderMedNavn(innsender: String): PersonInfoGyldighet {
        val innsenderPdl = pdlClient.hentPerson(innsender, PersonRolle.GJENLEVENDE)
        return PersonInfoGyldighet(
            "${innsenderPdl.fornavn} ${innsenderPdl.etternavn}",
            innsenderPdl.foedselsnummer.value
        )
    }

    private fun hentAvdoedMedSiviltilstand(avdoed: String): PersonInfoMedSiviltilstand {
        val avdoedPdl = pdlClient.hentPerson(avdoed, PersonRolle.AVDOED)
        val personInfo =
            PersonInfoGyldighet("${avdoedPdl.fornavn} ${avdoedPdl.etternavn}", avdoedPdl.foedselsnummer.value)
        return PersonInfoMedSiviltilstand(personInfo, avdoedPdl.siviltilstand)
    }

    /*
    * Sjekker at innsender er gjenlevende ved å se at angitt avdød og innsender har eller har hatt en felles
    * siviltilstand i PDL.
    */
    private fun innsenderErGjenlevende(
        innsender: PersonInfoGyldighet?,
        avdoed: List<PersonInfoMedSiviltilstand>
    ): VurdertGyldighet {
        val resultat = if (innsender == null || avdoed.isEmpty()) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val avdoedHarSiviltilstandMedInnsender = avdoed
                .mapNotNull { it.siviltilstand }
                .flatten()
                .any { it.relatertVedSiviltilstand?.value == innsender.fnr }
            if (avdoedHarSiviltilstandMedInnsender) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
        }
        return VurdertGyldighet(
            GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
            resultat,
            InnsenderErGjenlevende(innsender, avdoed)
        )
    }
}