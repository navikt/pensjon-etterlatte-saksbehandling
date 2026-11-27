package no.nav.etterlatte.brev.batch

import no.nav.etterlatte.brev.BrevBatchRequest
import no.nav.etterlatte.brev.validerInformasjonsbrev
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.ManglerTilgang
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID

class BrevBatchConfig(
    private val props: () -> Map<String, String>,
) {
    constructor(env: Miljoevariabler) : this({ env.props })

    val aktivert get() = props()["BREV_BATCH_AKTIVERT"]?.toBooleanStrictOrNull() ?: false
    val tillattInnsender get() = props()["BREV_BATCH_TILLATT_INNSENDER"]?.takeIf { it.isNotBlank() }
    val intervall get() = Duration.ofSeconds(props()["BREV_BATCH_INTERVALL_SEKUNDER"]?.toLongOrNull()?.coerceAtLeast(1) ?: 10)
    val maksForsoek get() = props()["BREV_BATCH_MAKS_FORSOEK"]?.toIntOrNull()?.coerceIn(1, 10) ?: 3
}

class BrevBatchService(
    private val dao: BrevBatchDao,
    private val config: BrevBatchConfig,
) {
    fun autoriser(bruker: BrukerTokenInfo): String {
        if (bruker !is Systembruker || config.tillattInnsender == null || bruker.ident() != config.tillattInnsender) {
            throw ManglerTilgang()
        }
        return bruker.ident()
    }

    fun opprett(
        bruker: BrukerTokenInfo,
        noekkel: UUID,
        request: BrevBatchRequest,
    ) = run {
        val innsender = autoriser(bruker)
        if (!config.aktivert) throw ForespoerselException(503, "BREV_BATCH_DEAKTIVERT", "Brevbatch er deaktivert")
        val normalisert = normaliser(request)
        val id = dao.opprett(innsender, noekkel, normalisert, fingeravtrykk(normalisert))
        dao.hent(id, innsender) ?: throw GenerellIkkeFunnetException()
    }

    fun hent(
        bruker: BrukerTokenInfo,
        batchId: UUID,
    ) = dao.hent(batchId, autoriser(bruker)) ?: throw GenerellIkkeFunnetException()

    fun hentSaker(
        bruker: BrukerTokenInfo,
        batchId: UUID,
        offset: Int,
        limit: Int,
    ) = run {
        val innsender = autoriser(bruker)
        if (offset < 0 || limit !in 1..100) throw UgyldigForespoerselException("BREV_BATCH_SIDE", "Ugyldig paginering")
        dao.hentSaker(batchId, innsender, offset, limit) ?: throw GenerellIkkeFunnetException()
    }

    companion object {
        const val MAKS_REQUEST_BYTES = 256 * 1024

        fun normaliser(request: BrevBatchRequest): BrevBatchRequest {
            val sakIder = request.sakIder.distinct().sortedBy { it.sakId }
            if (sakIder.isEmpty() || sakIder.size > 1000 || sakIder.any { it.sakId <= 0 }) {
                throw UgyldigForespoerselException("BREV_BATCH_SAKER", "Batchen må ha mellom 1 og 1000 unike positive sak-ID-er")
            }
            validerInformasjonsbrev(request.tittel, request.innhold)
            return request.copy(sakIder = sakIder)
        }

        fun fingeravtrykk(request: BrevBatchRequest): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(request.toJson().toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
