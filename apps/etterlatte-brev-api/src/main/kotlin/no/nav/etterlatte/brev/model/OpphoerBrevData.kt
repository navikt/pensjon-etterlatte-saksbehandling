package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.AvslagBrevData.valider
import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

abstract class OpphoerBrevData : BrevData()

data class AdopsjonRevurderingBrevdata(
    val virkningsdato: LocalDate,
    val adopsjonsdato: LocalDate,
    val adoptertAv1: Navn,
    val adoptertAv2: Navn? = null,
) :
    OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): AdopsjonRevurderingBrevdata {
            val revurderingInfo =
                valider<RevurderingInfo.Adopsjon>(
                    behandling,
                    RevurderingAarsak.ADOPSJON,
                )

            return AdopsjonRevurderingBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                adopsjonsdato = behandling.adopsjonsdato!!,
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
        fun fra(behandling: Behandling): OmgjoeringAvFarskapRevurderingBrevdata {
            val revurderingInfo =
                valider<RevurderingInfo.OmgjoeringAvFarskap>(
                    behandling,
                    RevurderingAarsak.OMGJOERING_AV_FARSKAP,
                )
            return OmgjoeringAvFarskapRevurderingBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                naavaerendeFar = revurderingInfo.naavaerendeFar,
                forrigeFar = revurderingInfo.forrigeFar,
                opprinneligInnvilgelsesdato = behandling.innvilgelsesdato!!,
            )
        }
    }
}
