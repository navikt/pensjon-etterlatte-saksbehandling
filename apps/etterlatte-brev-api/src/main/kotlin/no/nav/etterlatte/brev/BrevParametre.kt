package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonMottattSoeknad
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnhentingAvOpplysninger
import no.nav.etterlatte.brev.model.oms.Aktivitetsgrad
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon4MndBrevdata
import no.nav.etterlatte.brev.model.oms.AktivitetspliktInformasjon6MndBrevdata
import no.nav.etterlatte.brev.model.oms.NasjonalEllerUtland
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonDoedsfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonMottattSoeknad
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnhentingAvOpplysninger
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class BrevParametre {
    abstract val brevkode: EtterlatteBrevKode

    abstract fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND")
    data class AktivitetspliktInformasjon4Mnd(
        val aktivitetsgrad: Aktivitetsgrad,
        val utbetaling: Boolean,
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            AktivitetspliktInformasjon4MndBrevdata(aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND")
    data class AktivitetspliktInformasjon6Mnd(
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            AktivitetspliktInformasjon6MndBrevdata(redusertEtterInntekt, nasjonalEllerUtland)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD")
    data class OmstillingsstoenadInformasjonMottattSoeknadRedigerbar(
        val mottattDato: LocalDate,
        val borINorgeEllerIkkeAvtaleland: Boolean,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            OmstillingsstoenadInformasjonMottattSoeknad(
                mottattDato = mottattDato,
                borINorgeEllerIkkeAvtaleland = borINorgeEllerIkkeAvtaleland,
            )
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER")
    data class OmstillingsstoenadInformasjonInnhentingAvOpplysninger(
        val borIUtlandet: Boolean,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            OmstillingsstoenadInnhentingAvOpplysninger(borIUtlandet = borIUtlandet)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD")
    data class OmstillingsstoenadInformasjonDoedsfallRedigerbar(
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            OmstillingsstoenadInformasjonDoedsfall(
                innhold = emptyList(),
                avdoedNavn = avdoedNavn,
                borIutland = bosattUtland,
            )
    }

    @JsonTypeName("BARNEPENSJON_INFORMASJON_DOEDSFALL_INNHOLD")
    data class BarnepensjonInformasjonDoedsfallRedigerbar(
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        val erOver18Aar: Boolean,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            BarnepensjonInformasjonDoedsfall(
                innhold = emptyList(),
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
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
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
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
    ) : BrevParametre() {
        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar =
            BarnepensjonInnhentingAvOpplysninger(
                erOver18aar = erOver18aar,
                borIUtlandet = borIUtlandet,
            )
    }

    @JsonTypeName("TOMT_BREV")
    class TomtBrev : BrevParametre() {
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.TOM_DELMAL

        override fun brevDataMapping(req: BrevDataRedigerbarRequest): BrevDataRedigerbar = ManueltBrevData()
    }
}
