package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.AvslagBrevData
import no.nav.etterlatte.brev.model.OpphoerBrevData
import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

data class AdopsjonRevurderingBrevdata(
    val virkningsdato: LocalDate,
    val adopsjonsdato: LocalDate,
    val adoptertAv1: Navn,
    val adoptertAv2: Navn? = null,
) :
    OpphoerBrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            adopsjonsdato: LocalDate,
        ): AdopsjonRevurderingBrevdata {
            val revurderingInfo =
                AvslagBrevData.valider<RevurderingInfo.Adopsjon>(
                    generellBrevData.revurderingsaarsak,
                    generellBrevData.forenkletVedtak.revurderingInfo,
                    RevurderingAarsak.ADOPSJON,
                )

            val virkningstidspunkt =
                requireNotNull(generellBrevData.forenkletVedtak.virkningstidspunkt) {
                    "brev for behandling=${generellBrevData.behandlingId} må ha virkningstidspunkt"
                }
            return AdopsjonRevurderingBrevdata(
                virkningsdato = virkningstidspunkt.atDay(1),
                adopsjonsdato = adopsjonsdato,
                adoptertAv1 = revurderingInfo.adoptertAv1,
                adoptertAv2 = revurderingInfo.adoptertAv2,
            )
        }
    }
}

data class OmgjoeringAvFarskapRevurderingBrevdata(
    val virkningsdato: LocalDate,
    val naavaerendeFar: Navn,
    var forrigeFar: Navn,
    val opprinneligInnvilgelsesdato: LocalDate,
) : OpphoerBrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            opprinneligInnvilgelsesdato: LocalDate,
        ): OmgjoeringAvFarskapRevurderingBrevdata {
            val revurderingInfo =
                AvslagBrevData.valider<RevurderingInfo.OmgjoeringAvFarskap>(
                    generellBrevData.revurderingsaarsak,
                    generellBrevData.forenkletVedtak.revurderingInfo,
                    RevurderingAarsak.OMGJOERING_AV_FARSKAP,
                )
            val virkningstidspunkt =
                requireNotNull(generellBrevData.forenkletVedtak.virkningstidspunkt) {
                    "Mangler virkningstidspunkt ${generellBrevData.behandlingId}"
                }
            return OmgjoeringAvFarskapRevurderingBrevdata(
                virkningsdato = virkningstidspunkt.atDay(1),
                naavaerendeFar = revurderingInfo.naavaerendeFar,
                forrigeFar = revurderingInfo.forrigeFar,
                opprinneligInnvilgelsesdato = opprinneligInnvilgelsesdato,
            )
        }
    }
}
