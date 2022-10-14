package no.nav.etterlatte.statistikk

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.time.Instant

class StatistikkService(
    private val repository: StatistikkRepository
) : VedlikeholdService {

    fun registrerStatistikkForVedtak(vedtak: Vedtak): StoenadRad? {
        return when (vedtak.type) {
            VedtakType.INNVILGELSE -> repository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
            VedtakType.AVSLAG -> null
            VedtakType.ENDRING -> repository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
            VedtakType.OPPHOER -> repository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
        }
    }

    private fun hentSoesken(vedtak: Vedtak): List<Foedselsnummer> {
        val grunnlag = vedtak.grunnlag
        val soekerFnr = vedtak.sak.ident
        val avdoedPdl = grunnlag.finnOpplysning<Person>(Opplysningstype.AVDOED_PDL_V1)
        val avdoedesBarn = avdoedPdl?.opplysning?.avdoedesBarn ?: emptyList()
        return avdoedesBarn
            .filter { it.foedselsnummer.value != soekerFnr }
            .map { it.foedselsnummer }
    }

    private fun hentForeldre(vedtak: Vedtak): List<Foedselsnummer> {
        val grunnlag = vedtak.grunnlag
        val soekerPdl = grunnlag.finnOpplysning<Person>(Opplysningstype.SOEKER_PDL_V1)
        return soekerPdl?.opplysning?.familieRelasjon?.foreldre ?: emptyList()
    }

    private fun vedtakTilStoenadsrad(vedtak: Vedtak): StoenadRad =
        StoenadRad(
            -1,
            vedtak.sak.ident,
            hentForeldre(vedtak),
            hentSoesken(vedtak),
            "40",
            vedtak.beregning?.sammendrag?.firstOrNull()?.beloep.toString(),
            "FOLKETRYGD",
            "",
            vedtak.behandling.id,
            vedtak.sak.id,
            vedtak.sak.id,
            Instant.now(),
            vedtak.sak.sakType,
            "",
            vedtak.vedtakFattet!!.ansvarligSaksbehandler,
            vedtak.attestasjon?.attestant,
            vedtak.virk.fom.atDay(1),
            vedtak.virk.tom?.atEndOfMonth()
        )

    override fun slettSak(sakId: Long) {
        repository.slettSak(sakId)
    }
}

inline fun <reified T> List<Grunnlagsopplysning<ObjectNode>>.finnOpplysning(
    opplysningstype: Opplysningstype
): VilkaarOpplysning<T>? {
    return this.find { it.opplysningType == opplysningstype }
        ?.let {
            VilkaarOpplysning(
                it.id,
                it.opplysningType,
                it.kilde,
                objectMapper.readValue(it.opplysning.toString())
            )
        }
}