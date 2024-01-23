package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

class PDFService(private val db: BrevRepository) {
    fun lagreOpplastaPDF(
        sakId: Long,
        mp: List<PartData>,
    ): Brev {
        val request =
            mp.first { it is PartData.FormItem }
                .let { objectMapper.readValue<BrevFraOpplastningRequest>((it as PartData.FormItem).value) }

        val fil: ByteArray =
            mp.first { it is PartData.FileItem }
                .let { (it as PartData.FileItem).streamProvider().readBytes() }

        val brev = lagrePdf(sakId, fil, request.innhold, request.sak)
        return brev
    }

    fun lagrePdf(
        sakId: Long,
        fil: ByteArray,
        innhold: BrevInnhold,
        sak: Sak,
    ): Brev {
        val brev =
            db.opprettBrev(
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = null,
                    soekerFnr = sak.ident,
                    prosessType = BrevProsessType.OPPLASTET_PDF,
                    mottaker = Mottaker.tom(Folkeregisteridentifikator.of(sak.ident)),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = null,
                ),
            )

        db.lagrePdf(brev.id, Pdf(fil))

        return brev
    }
}
