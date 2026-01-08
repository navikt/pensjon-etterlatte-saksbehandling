package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.model.KlageSaksbehandlingstidData
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonMottattSoeknad
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnhentingAvOpplysninger
import no.nav.etterlatte.brev.model.oms.Aktivitetsgrad
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon10mndBrevdata
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon4MndBrevdata
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon6MndBrevdata
import no.nav.etterlatte.brev.model.oms.NasjonalEllerUtland
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonDoedsfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonMottattSoeknad
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnhentingAvOpplysninger
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class BrevParametre {
    abstract val brevkode: Brevkoder
    abstract val spraak: Spraak

    abstract fun brevDataMapping(): BrevDataRedigerbar

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND")
    data class AktivitetspliktInformasjon4Mnd(
        val aktivitetsgrad: Aktivitetsgrad,
        val utbetaling: Boolean,
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        val halvtGrunnbeloep: Int,
        override val spraak: Spraak,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            AktivitetspliktInformasjon4MndBrevdata(aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland, halvtGrunnbeloep)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_10MND")
    data class AktivitetspliktInformasjon10Mnd(
        override val spraak: Spraak,
        val aktivitetsgrad: Aktivitetsgrad,
        val utbetaling: Boolean,
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        val halvtGrunnbeloep: Int,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_10MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            AktivitetspliktInformasjon10mndBrevdata(aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland, halvtGrunnbeloep)
    }

    // Denne brukes kun til varig unntak som har egen manuel flyt
    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND")
    data class AktivitetspliktInformasjon6Mnd(
        override val spraak: Spraak,
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        val halvtGrunnbeloep: Int,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            AktivitetspliktInformasjon6MndBrevdata(redusertEtterInntekt, nasjonalEllerUtland, halvtGrunnbeloep)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD")
    data class OmstillingsstoenadInformasjonMottattSoeknadRedigerbar(
        override val spraak: Spraak,
        val mottattDato: LocalDate,
        val borINorgeEllerIkkeAvtaleland: Boolean,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            OmstillingsstoenadInformasjonMottattSoeknad(
                mottattDato = mottattDato,
                borINorgeEllerIkkeAvtaleland = borINorgeEllerIkkeAvtaleland,
            )
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER")
    data class OmstillingsstoenadInformasjonInnhentingAvOpplysninger(
        override val spraak: Spraak,
        val borIUtlandet: Boolean,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar = OmstillingsstoenadInnhentingAvOpplysninger(borIUtlandet = borIUtlandet)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD")
    data class OmstillingsstoenadInformasjonDoedsfallRedigerbar(
        override val spraak: Spraak,
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        override val brevkode: Brevkoder = Brevkoder.OMS_INFORMASJON_DOEDSFALL,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            OmstillingsstoenadInformasjonDoedsfall(
                avdoedNavn = avdoedNavn,
                borIutland = bosattUtland,
            )
    }

    @JsonTypeName("BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD")
    data class BarnepensjonInformasjonDoedsfallRedigerbar(
        override val spraak: Spraak,
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        val erOver18Aar: Boolean,
        override val brevkode: Brevkoder = Brevkoder.BP_INFORMASJON_DOEDSFALL,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            BarnepensjonInformasjonDoedsfall(
                avdoedNavn = avdoedNavn,
                borIutland = bosattUtland,
                erOver18aar = erOver18Aar,
            )
    }

    @JsonTypeName("BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD")
    data class BarnepensjonMottattSoeknad(
        override val spraak: Spraak,
        val mottattDato: LocalDate,
        val borINorgeEllerIkkeAvtaleland: Boolean,
        val erOver18aar: Boolean,
        val bosattUtland: Boolean,
        override val brevkode: Brevkoder = Brevkoder.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            BarnepensjonInformasjonMottattSoeknad(
                mottattDato = mottattDato,
                borINorgeEllerIkkeAvtaleland = borINorgeEllerIkkeAvtaleland,
                erOver18aar = erOver18aar,
                bosattUtland = bosattUtland,
            )
    }

    @JsonTypeName("BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER")
    data class BarnepensjonInformasjonInnhentingAvOpplysninger(
        override val spraak: Spraak,
        val erOver18aar: Boolean,
        val borIUtlandet: Boolean,
        override val brevkode: Brevkoder = Brevkoder.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            BarnepensjonInnhentingAvOpplysninger(
                erOver18aar = erOver18aar,
                borIUtlandet = borIUtlandet,
            )
    }

    @JsonTypeName("KLAGE_SAKSBEHANDLINGSTID")
    data class KlageSaksbehandlingstid(
        override val spraak: Spraak,
        val sakType: SakType,
        val borIUtlandet: Boolean,
        val datoMottatKlage: LocalDate,
        val datoForVedtak: LocalDate,
    ) : BrevParametre() {
        override val brevkode: Brevkoder = Brevkoder.KLAGE_SAKSBEHANDLINGSTID

        override fun brevDataMapping(): BrevDataRedigerbar =
            KlageSaksbehandlingstidData(
                datoMottatKlage = datoMottatKlage,
                datoForVedtak = datoForVedtak,
                borIUtlandet = borIUtlandet,
                sakType = sakType,
            )
    }

    @JsonTypeName("TOMT_BREV")
    data class TomtBrev(
        override val spraak: Spraak,
    ) : BrevParametre() {
        override val brevkode: Brevkoder = Brevkoder.TOMT_INFORMASJONSBREV

        override fun brevDataMapping(): BrevDataRedigerbar = ManueltBrevData()
    }
}
