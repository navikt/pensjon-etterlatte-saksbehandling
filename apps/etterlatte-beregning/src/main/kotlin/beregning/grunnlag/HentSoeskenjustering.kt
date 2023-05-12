package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOESKEN_I_BEREGNINGEN

fun Grunnlagsdata<JsonNode>.hentSoeskenjustering() =
    this.hentKonstantOpplysning<Beregningsgrunnlag>(SOESKEN_I_BEREGNINGEN)