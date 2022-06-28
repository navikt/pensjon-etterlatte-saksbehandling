package no.nav.etterlatte.testsupport

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.BehandlingId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.SakId
import java.io.FileNotFoundException
import java.util.*

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")

val kravgrunnlagId = KravgrunnlagId(1)

fun mottattKravgrunnlag() = Tilbakekreving.MottattKravgrunnlag(
    sakId = SakId(1),
    behandlingId = BehandlingId(UUID.randomUUID(), Kravgrunnlag.UUID30("")),
    kravgrunnlagId = kravgrunnlagId,
    opprettet = Tidspunkt.now(),
    kravgrunnlag = kravgrunnlag()
)

private fun kravgrunnlag(): Kravgrunnlag =
    detaljertKravgrunnlagDto().let { KravgrunnlagMapper().toKravgrunnlag(it, "") }

private fun detaljertKravgrunnlagDto() =
    readFile("/kravgrunnlag.xml").let { KravgrunnlagJaxb.toDetaljertKravgrunnlagDto(it) }