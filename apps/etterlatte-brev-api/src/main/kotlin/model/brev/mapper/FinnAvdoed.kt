package model.brev.mapper

import model.brev.Avdoed
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.helse.rapids_rivers.asLocalDate

fun Vedtak.finnAvdoed(): Avdoed = this.grunnlag
    .find { it.opplysningType === Opplysningstyper.AVDOED_PDL_V1 }!!
    .let {
        Avdoed(
            navn = "${it.opplysning["fornavn"].asText()} ${it.opplysning["etternavn"].asText()}",
            doedsdato = it.opplysning["doedsdato"].asLocalDate()
        )
    }
