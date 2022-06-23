package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.BehandlingId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.time.Clock
import java.util.*

class TilbakekrevingService(
    val tilbakekrevingDao: TilbakekrevingDao,
    val kravgrunnlagMapper: KravgrunnlagMapper,
    val clock: Clock
) {
    fun lagreKravgrunnlag(detaljertKravgrunnlag: DetaljertKravgrunnlagDto, kravgrunnlagXml: String) {
        val kravgrunnlag = kravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlag, kravgrunnlagXml)
        val mottattKravgrunnlag = Tilbakekreving.MottattKravgrunnlag(
            sakId = kravgrunnlag.sakId,
            behandlingId = BehandlingId(UUID.randomUUID(), Kravgrunnlag.UUID30("")),
            kravgrunnlagId = kravgrunnlag.kravgrunnlagId,
            opprettet = Tidspunkt.now(clock),
            kravgrunnlag = kravgrunnlag
        )
        val lagretKravgrunnlag = tilbakekrevingDao.lagreMottattKravgrunnlag(mottattKravgrunnlag)
    }
}