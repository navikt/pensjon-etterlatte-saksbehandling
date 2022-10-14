package no.nav.etterlatte.brev.model.mapper

import no.nav.etterlatte.brev.model.Avdoed
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.helse.rapids_rivers.asLocalDate

fun Vedtak.finnAvdoed(): Avdoed = this.grunnlag
    .find { it.opplysningType === Opplysningstype.AVDOED_PDL_V1 }!!
    .let {
        Avdoed(
            navn = "${it.opplysning["fornavn"].asText()} ${it.opplysning["etternavn"].asText()}",
            doedsdato = it.opplysning["doedsdato"].asLocalDate()
        )
    }