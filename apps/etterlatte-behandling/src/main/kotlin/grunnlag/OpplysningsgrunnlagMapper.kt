package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning

class OpplysningsgrunnlagMapper(
    private val grunnlaghendelser: List<OpplysningDao.GrunnlagHendelse>,
    private val persongalleri: Persongalleri,
) {
    fun hentGrunnlag(): Grunnlag {
        val senestVersjon = grunnlaghendelser.maxOfOrNull { it.hendelseNummer } ?: 0

        val (personopplysninger, saksopplysninger) = grunnlaghendelser.partition { it.opplysning.fnr != null }
        val (soeker, familie) = personopplysninger.partition { it.opplysning.fnr!!.value == persongalleri.soeker }

        val soekerMap =
            soeker.associateBy(
                { it.opplysning.opplysningType },
                { Opplysning.Konstant.create(it.opplysning) },
            )
        val familieMap =
            familie
                .groupBy { it.opplysning.fnr }
                .values
                .map { familiemedlem ->
                    familiemedlem.associateBy(
                        { it.opplysning.opplysningType },
                        { Opplysning.Konstant.create(it.opplysning) },
                    )
                }
        val sakMap =
            saksopplysninger.associateBy(
                { it.opplysning.opplysningType },
                { Opplysning.Konstant.create(it.opplysning) },
            )

        val sakId = grunnlaghendelser.first().sakId

        return Grunnlag(
            soeker = soekerMap,
            familie = familieMap,
            sak = sakMap,
            metadata = Metadata(sakId, senestVersjon),
        )
    }
}
