package no.nav.etterlatte.brev.pdfgen

import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal

data class SlatePDFMal(
    val slate: Slate,
) : PDFMal
