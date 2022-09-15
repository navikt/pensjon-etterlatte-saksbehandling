package no.nav.etterlatte.brev.model.mapper

import no.nav.etterlatte.brev.model.Barn
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper

fun Vedtak.finnBarn(): Barn = this.grunnlag
    .find { it.opplysningType === Opplysningstyper.SOEKER_PDL_V1 }!!
    .let {
        Barn(
            navn = "${it.opplysning["fornavn"].asText()} ${it.opplysning["etternavn"].asText()}",
            fnr = it.opplysning["foedselsnummer"].asText()
        )
    }