package no.nav.etterlatte.brev.batch

import no.nav.etterlatte.brev.BrevBatchRequest
import no.nav.etterlatte.brev.StrukturertInformasjonsbrevRequest
import no.nav.etterlatte.brev.behandling.erOver18
import no.nav.etterlatte.brev.behandling.hentForelderVerge
import no.nav.etterlatte.brev.behandling.hentGjenlevende
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentErForeldreloes
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.SakService

class BrevBatchSakFeil(
    val kode: String,
) : Exception(kode)

class BrevBatchSnapshot(
    private val sakService: SakService,
    private val grunnlagService: GrunnlagService,
) {
    fun bygg(
        sakId: SakId,
        request: BrevBatchRequest,
    ): StrukturertInformasjonsbrevRequest {
        val (sak, grunnlag) =
            inTransaction {
                val sak = sakService.finnSak(sakId) ?: throw BrevBatchSakFeil("SAK_MANGLER")
                val grunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(sakId) ?: throw BrevBatchSakFeil("GRUNNLAG_MANGLER")
                sak to grunnlag
            }
        return map(sak, grunnlag, request)
    }

    companion object {
        fun map(
            sak: Sak,
            grunnlag: Grunnlag,
            request: BrevBatchRequest,
        ): StrukturertInformasjonsbrevRequest {
            val spraak =
                grunnlag.sak.hentKonstantOpplysning<Spraak>(Opplysningstype.SPRAAK)?.verdi
                    ?: throw BrevBatchSakFeil("SPRAAK_MANGLER")
            if (spraak != request.spraak) throw BrevBatchSakFeil("SPRAAK_AVVIK")
            return try {
                StrukturertInformasjonsbrevRequest(
                    sak,
                    grunnlag.mapSoeker(null),
                    grunnlag.mapInnsender(),
                    grunnlag.mapAvdoede(),
                    verge(sak, grunnlag),
                    grunnlag.hentGjenlevende(),
                    request.tittel,
                    request.innhold,
                    request.spraak,
                )
            } catch (e: BrevBatchSakFeil) {
                throw e
            } catch (_: Exception) {
                throw BrevBatchSakFeil("GRUNNLAG_UFULLSTENDIG")
            }
        }

        private fun verge(
            sak: Sak,
            grunnlag: Grunnlag,
        ): Verge? {
            // Samme mottakerregler som hentVergeForSak, uten logging av identiteter eller ukjent verge.
            val soeker = grunnlag.soeker.hentSoekerPdlV1()?.verdi ?: throw BrevBatchSakFeil("GRUNNLAG_UFULLSTENDIG")
            val registrerteVerger = soeker.vergemaalEllerFremtidsfullmakt ?: emptyList()
            if (registrerteVerger.any { it.vergeEllerFullmektig.motpartsPersonident == null }) {
                throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
            }
            val verger = hentVerger(registrerteVerger, grunnlag.soeker.hentFoedselsnummer()?.verdi)
            if (verger.size > 1) throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
            if (verger.size == 1) {
                val ident = verger.single().vergeEllerFullmektig.motpartsPersonident ?: throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
                val navn =
                    verger
                        .single()
                        .vergeEllerFullmektig.navn
                        ?.takeIf { it.isNotBlank() } ?: throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
                return Vergemaal(navn, ident)
            }
            if (sak.sakType == SakType.BARNEPENSJON && !grunnlag.erOver18(null)) {
                val ansvarligeForeldre =
                    grunnlag.soeker
                        .hentFamilierelasjon()
                        ?.verdi
                        ?.ansvarligeForeldre
                        .orEmpty()
                val ansvarligeGjenlevende =
                    grunnlag.hentGjenlevende().distinct().filter { ident ->
                        ansvarligeForeldre.any { it.value == ident }
                    }
                if (ansvarligeGjenlevende.size > 1) throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
                if (grunnlag.sak.hentErForeldreloes()?.verdi == true) return null
                if (ansvarligeGjenlevende.size != 1) throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
                return grunnlag.hentForelderVerge() ?: throw BrevBatchSakFeil("MOTTAKER_UAVKLART")
            }
            return null
        }
    }
}
