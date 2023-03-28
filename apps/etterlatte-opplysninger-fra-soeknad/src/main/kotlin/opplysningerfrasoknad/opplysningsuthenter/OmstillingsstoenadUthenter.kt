package no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Samtykke
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerOmstillingSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadstypeOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utbetalingsinformasjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.Spraak
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt

internal object OmstillingsstoenadUthenter {
    fun lagOpplysningsListe(jsonNode: JsonNode): List<Grunnlagsopplysning<out Any?>> {
        val omstillingsstoenad = objectMapper.treeToValue<Omstillingsstoenad>(jsonNode)

        return listOfNotNull(
            avdoed(omstillingsstoenad),
            soeker(omstillingsstoenad),
            innsender(omstillingsstoenad),
            utbetalingsinformasjon(omstillingsstoenad),
            samtykke(omstillingsstoenad),
            spraak(omstillingsstoenad),
            soeknadMottattDato(omstillingsstoenad),
            soeknadsType(omstillingsstoenad),
            personGalleri(omstillingsstoenad)
        )
    }

    private fun soeknadMottattDato(soknad: Omstillingsstoenad): Grunnlagsopplysning<SoeknadMottattDato> {
        val opplysning = SoeknadMottattDato(
            mottattDato = soknad.mottattDato
        )
        return lagOpplysning(Opplysningstype.SOEKNAD_MOTTATT_DATO, kilde(soknad), opplysning, null)
    }

    private fun kilde(soknad: Omstillingsstoenad): Grunnlagsopplysning.Kilde {
        return Grunnlagsopplysning.Privatperson(
            soknad.innsender.foedselsnummer.svar.value,
            soknad.mottattDato.toTidspunkt()
        )
    }

    private fun avdoed(soknad: Omstillingsstoenad): Grunnlagsopplysning<AvdoedSoeknad> {
        val opplysning = avdoedOpplysning(soknad.avdoed)
        return lagOpplysning(Opplysningstype.AVDOED_SOEKNAD_V1, kilde(soknad), opplysning, null)
    }

    private fun soeker(soknad: Omstillingsstoenad): Grunnlagsopplysning<SoekerOmstillingSoeknad> {
        val soeker = soknad.soeker
        val opplysning = SoekerOmstillingSoeknad(
            PersonType.GJENLEVENDE,
            soeker.fornavn.svar,
            soeker.etternavn.svar,
            soeker.foedselsnummer.svar,
            soeker.adresse?.svar,
            soeker.statsborgerskap.svar,
            soeker.kontaktinfo.telefonnummer.svar.innhold,
            soeker.sivilstatus.svar
        )
        return lagOpplysning(Opplysningstype.SOEKER_SOEKNAD_V1, kilde(soknad), opplysning, null)
    }

    private fun innsender(soknad: Omstillingsstoenad): Grunnlagsopplysning<InnsenderSoeknad> {
        val opplysning = InnsenderSoeknad(
            PersonType.INNSENDER,
            soknad.innsender.fornavn.svar,
            soknad.innsender.etternavn.svar,
            soknad.innsender.foedselsnummer.svar
        )
        return lagOpplysning(Opplysningstype.INNSENDER_SOEKNAD_V1, kilde(soknad), opplysning, null)
    }

    private fun utbetalingsinformasjon(
        soknad: Omstillingsstoenad
    ): Grunnlagsopplysning<Utbetalingsinformasjon> {
        val opplysning = utbetalingsinformasjonOpplysning(soknad.utbetalingsInformasjon)
        return lagOpplysning(Opplysningstype.UTBETALINGSINFORMASJON_V1, kilde(soknad), opplysning, null)
    }

    private fun samtykke(soknad: Omstillingsstoenad): Grunnlagsopplysning<Samtykke> {
        val opplysning = Samtykke(soknad.harSamtykket.svar)
        return lagOpplysning(Opplysningstype.SAMTYKKE, kilde(soknad), opplysning, null)
    }

    private fun spraak(soknad: Omstillingsstoenad): Grunnlagsopplysning<Spraak> {
        return lagOpplysning(Opplysningstype.SPRAAK, kilde(soknad), soknad.spraak, null)
    }

    private fun soeknadsType(soknad: Omstillingsstoenad): Grunnlagsopplysning<SoeknadstypeOpplysning> {
        return lagOpplysning(Opplysningstype.SOEKNADSTYPE_V1, kilde(soknad), SoeknadstypeOpplysning(soknad.type), null)
    }

    private fun personGalleri(soknad: Omstillingsstoenad): Grunnlagsopplysning<Persongalleri> {
        val opplysning = Persongalleri(
            soeker = soknad.soeker.foedselsnummer.svar.value,
            innsender = soknad.innsender.foedselsnummer.svar.value,
            avdoed = listOf(soknad.avdoed.foedselsnummer.svar.value)
        )
        return lagOpplysning(Opplysningstype.PERSONGALLERI_V1, kilde(soknad), opplysning, null)
    }
}