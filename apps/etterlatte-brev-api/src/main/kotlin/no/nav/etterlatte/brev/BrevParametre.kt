package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.oms.Aktivitetsgrad
import no.nav.etterlatte.brev.model.oms.AktivitetspliktBrevdata
import no.nav.etterlatte.brev.model.oms.NasjonalEllerUtland
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonDoedsfall

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class BrevParametre {
    abstract val brevkode: EtterlatteBrevKode

    abstract fun brevDataMapping(req: RedigerbarTekstRequest): BrevDataRedigerbar

    @JsonTypeName("OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND")
    data class Aktivitetsplikt(
        val aktivitetsgrad: Aktivitetsgrad,
        val utbetaling: Boolean,
        val redusertEtterInntekt: Boolean,
        val nasjonalEllerUtland: NasjonalEllerUtland,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
    ) : BrevParametre() {
        override fun brevDataMapping(req: RedigerbarTekstRequest): BrevDataRedigerbar =
            AktivitetspliktBrevdata(aktivitetsgrad, utbetaling, redusertEtterInntekt, nasjonalEllerUtland)
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL_INNHOLD")
    data class OmstillingsstoenadInformasjonDoedsfallRedigerbar(
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
    ) : BrevParametre() {
        override fun brevDataMapping(req: RedigerbarTekstRequest): BrevDataRedigerbar =
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
        override fun brevDataMapping(req: RedigerbarTekstRequest): BrevDataRedigerbar =
            BarnepensjonInformasjonDoedsfall(
                innhold = emptyList(),
                avdoedNavn = avdoedNavn,
                borIutland = bosattUtland,
                erOver18aar = erOver18Aar,
            )
    }

    @JsonTypeName("TOMT_BREV")
    class TomtBrev : BrevParametre() {
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.TOM_DELMAL

        override fun brevDataMapping(req: RedigerbarTekstRequest): BrevDataRedigerbar = ManueltBrevData()
    }
}
