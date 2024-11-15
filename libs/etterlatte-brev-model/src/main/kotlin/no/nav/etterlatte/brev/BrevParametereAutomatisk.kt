package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInformasjonDoedsfall

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class BrevParametereAutomatisk {
    abstract val brevkode: Brevkoder

    abstract fun brevDataMapping(): BrevDataRedigerbar

    @JsonTypeName("BP_INFORMASJON_DOEDSFALL")
    data class BarnepensjonInformasjonDoedsfallRedigerbar(
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        val erOver18Aar: Boolean,
        override val brevkode: Brevkoder = Brevkoder.BP_INFORMASJON_DOEDSFALL,
    ) : BrevParametereAutomatisk() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            BarnepensjonInformasjonDoedsfall(
                avdoedNavn = avdoedNavn,
                borIutland = bosattUtland,
                erOver18aar = erOver18Aar,
            )
    }

    @JsonTypeName("BARNEPENSJON_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT")
    data class BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunktRedigerbar(
        val avdoedNavn: String,
        val borIutland: Boolean,
        override val brevkode: Brevkoder = Brevkoder.BP_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT,
    ) : BrevParametereAutomatisk() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
                avdoedNavn = avdoedNavn,
                borIutland = borIutland,
            )
    }

    @JsonTypeName("OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL")
    data class OmstillingsstoenadInformasjonDoedsfallRedigerbar(
        val bosattUtland: Boolean,
        val avdoedNavn: String,
        override val brevkode: Brevkoder = Brevkoder.OMS_INFORMASJON_DOEDSFALL,
    ) : BrevParametereAutomatisk() {
        override fun brevDataMapping(): BrevDataRedigerbar =
            OmstillingsstoenadInformasjonDoedsfall(
                avdoedNavn = avdoedNavn,
                borIutland = bosattUtland,
            )
    }
}
