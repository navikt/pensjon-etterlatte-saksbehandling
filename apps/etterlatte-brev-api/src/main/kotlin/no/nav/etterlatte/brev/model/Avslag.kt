package no.nav.etterlatte.brev.model

import no.nav.etterlatte.grunnlag.Persongalleri
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.vedtak.Vedtak

data class AvslagBrevRequest(
    val saksnummer: String,
    val barn: Soeker,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = "avslag"

    companion object {
        fun fraVedtak(vedtak: Vedtak, grunnlag: Persongalleri, avsender: Avsender): AvslagBrevRequest = AvslagBrevRequest(
            saksnummer = vedtak.sak.id.toString(),
            barn = grunnlag.soeker,
            avdoed = grunnlag.avdoed,
            aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
            spraak = Spraak.NB, // todo, må hentes.
            avsender = avsender,
            mottaker = Mottaker(grunnlag.innsender.navn),
        )
    }
}