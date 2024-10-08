package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.model.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.OmstillingsstoenadInformasjonDoedsfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonMottattSoeknad
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnhentingAvOpplysninger
import no.nav.etterlatte.brev.model.oms.Aktivitetsgrad
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon4MndBrevdata
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon6MndBrevdata
import no.nav.etterlatte.brev.model.oms.NasjonalEllerUtland
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonMottattSoeknad
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnhentingAvOpplysninger
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class BrevParametre {
    abstract val brevkode: Brevkoder

    abstract fun brevDataMapping(): BrevDataRedigerbar

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND")
    data class AktivitetspliktInformasjon4Mnd(
        val aktivitetsgrad: Aktivitetsgrad,
        val utbetaling: Boolean,
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            AktivitetspliktInformasjon4MndBrevdata(aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND")
    data class AktivitetspliktInformasjon6Mnd(
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            AktivitetspliktInformasjon6MndBrevdata(redusertEtterInntekt, nasjonalEllerUtland)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD")
    data class OmstillingsstoenadInformasjonMottattSoeknadRedigerbar(
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
        val borIUtlandet: Boolean,
        override val brevkode: Brevkoder = Brevkoder.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
    ) : BrevParametre() {
        override fun brevDataMapping(): BrevDataRedigerbar = OmstillingsstoenadInnhentingAvOpplysninger(borIUtlandet = borIUtlandet)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD")
    data class OmstillingsstoenadInformasjonDoedsfallRedigerbar(
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

    @JsonTypeName("TOMT_BREV")
    class TomtBrev : BrevParametre() {
        override val brevkode: Brevkoder = Brevkoder.TOMT_INFORMASJONSBREV

        override fun brevDataMapping(): BrevDataRedigerbar = ManueltBrevData()
    }
}
