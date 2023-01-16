package no.nav.etterlatte.vedtaksvurdering.database

import kotliquery.Row
import kotliquery.Session
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.vedtaksvurdering.Beregningsresultat
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import java.io.Serializable
import java.sql.Date
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(datasource: DataSource) {

    private val repositoryProxy: RepositoryProxy = RepositoryProxy(datasource)

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository = VedtaksvurderingRepository(datasource)
    }

    fun opprettVedtak(
        behandlingsId: UUID,
        sakid: Long,
        fnr: String,
        saktype: SakType,
        behandlingtype: BehandlingType,
        virkningsDato: LocalDate,
        beregningsresultat: Beregningsresultat?,
        vilkaarsresultat: VilkaarsvurderingDto
    ) = repositoryProxy.opprett(
        query = "INSERT INTO vedtak(behandlingId, sakid, fnr, behandlingtype, saktype, vedtakstatus, datovirkfom,  beregningsresultat, vilkaarsresultat) " + // ktlint-disable max-line-length
            "VALUES (:behandlingId, :sakid, :fnr, :behandlingtype, :saktype, :vedtakstatus, :datovirkfom, :beregningsresultat, :vilkaarsresultat)", // ktlint-disable max-line-length
        params = mapOf(
            "behandlingId" to behandlingsId,
            "sakid" to sakid,
            "fnr" to fnr,
            "behandlingtype" to behandlingtype.name,
            "saktype" to saktype.name,
            "vedtakstatus" to VedtakStatus.BEREGNET.name,
            "datovirkfom" to Date.valueOf(virkningsDato),
            "beregningsresultat" to beregningsresultat?.let { objectMapper.writeValueAsString(it) },
            "vilkaarsresultat" to vilkaarsresultat.let { objectMapper.writeValueAsString(it) }
        ),
        loggtekst = "Oppretter vedtak behandlingid: $behandlingsId sakid: $sakid"
    )

    fun oppdaterVedtak(
        behandlingsId: UUID,
        beregningsresultat: Beregningsresultat?,
        vilkaarsresultat: VilkaarsvurderingDto,
        virkningsDato: LocalDate
    ) = repositoryProxy.oppdater(
        query = "UPDATE vedtak SET datovirkfom = :datovirkfom, beregningsresultat = :beregningsresultat, vilkaarsresultat = :vilkaarsresultat WHERE behandlingId = :behandlingid", // ktlint-disable max-line-length
        params = mapOf(
            "datovirkfom" to Date.valueOf(virkningsDato),
            "beregningsresultat" to objectMapper.writeValueAsString(beregningsresultat),
            "vilkaarsresultat" to objectMapper.writeValueAsString(vilkaarsresultat),
            "behandlingid" to behandlingsId
        ),
        loggtekst = "Oppdaterer vedtak behandlingid: $behandlingsId "
    ).also { require(it == 1) }

    // Kan det finnes flere vedtak for en behandling? Hør med Henrik
    fun lagreIverksattVedtak(
        behandlingsId: UUID
    ) = repositoryProxy.oppdater(
        query = "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId",
        params = mapOf("vedtakstatus" to VedtakStatus.IVERKSATT.name, "behandlingId" to behandlingsId),
        loggtekst = "Lagrer iverksatt vedtak"
    ).also { require(it == 1) }

    fun hentVedtakBolk(behandlingsidenter: List<UUID>) = repositoryProxy.hentListeMedKotliquery(
        query = "SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat," +
            "vedtakfattet, id, fnr, datoFattet, datoattestert, attestant," +
            "datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak where behandlingId = ANY(:behandlingId)",
        { session: Session -> mapOf("behandlingId" to session.createArrayOf("uuid", behandlingsidenter)) }
    ) {
        it.toVedtak()
    }

    fun hentVedtak(behandlingsId: UUID): Vedtak? {
        val hentVedtak =
            "SELECT sakid, behandlingId, saksbehandlerId, beregningsresultat, vilkaarsresultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom, vedtakstatus, saktype, behandlingtype FROM vedtak WHERE behandlingId = :behandlingId" // ktlint-disable max-line-length
        return repositoryProxy.hentMedKotliquery(
            query = hentVedtak,
            params = mapOf("behandlingId" to behandlingsId)
        ) { it.toVedtak() }
    }

    private fun Row.toVedtak() = Vedtak(
        sakId = longOrNull("sakid"),
        behandlingId = uuid("behandlingid"),
        saksbehandlerId = stringOrNull("saksbehandlerid"),
        beregningsResultat = stringOrNull("beregningsresultat")?.let {
            objectMapper.readValue(
                it,
                Beregningsresultat::class.java
            )
        },
        vilkaarsResultat = stringOrNull("vilkaarsresultat")?.toJsonNode(),
        vedtakFattet = boolean("vedtakfattet"),
        id = long("id"),
        fnr = stringOrNull("fnr"),
        datoFattet = sqlTimestampOrNull("datofattet")?.toInstant(),
        datoattestert = sqlTimestampOrNull("datoattestert")?.toInstant(),
        attestant = stringOrNull("attestant"),
        virkningsDato = sqlDateOrNull("datovirkfom")?.toLocalDate(),
        vedtakStatus = stringOrNull("vedtakstatus")?.let { VedtakStatus.valueOf(it) },
        sakType = SakType.valueOf(string("saktype")),
        behandlingType = BehandlingType.valueOf(string("behandlingtype"))
    )

    fun hentUtbetalingsPerioder(vedtakId: Long): List<Utbetalingsperiode> = repositoryProxy.hentListeMedKotliquery(
        query = "SELECT * FROM utbetalingsperiode WHERE vedtakid = :vedtakid",
        params = { mapOf("vedtakid" to vedtakId) }
    ) {
        Utbetalingsperiode(
            it.long("id"),
            Periode(
                YearMonth.from(it.localDate("datoFom")),
                it.localDateOrNull("datoTom")?.let(YearMonth::from)
            ),
            it.bigDecimalOrNull("beloep"),
            UtbetalingsperiodeType.valueOf(it.string("type"))
        )
    }

    fun fattVedtak(saksbehandlerId: String, behandlingsId: UUID) = repositoryProxy.oppdater(
        query = "UPDATE vedtak SET saksbehandlerId = :saksbehandlerId, vedtakfattet = :vedtakfattet, datoFattet = now(), vedtakstatus = :vedtakstatus  WHERE behandlingId = :behandlingId", // ktlint-disable max-line-lengt
        params = mapOf(
            "saksbehandlerId" to saksbehandlerId,
            "vedtakfattet" to true,
            "vedtakstatus" to VedtakStatus.FATTET_VEDTAK.name,
            "behandlingId" to behandlingsId
        ),
        loggtekst = "Fatter vedtok for behandling $behandlingsId"
    )

    fun attesterVedtak(
        saksbehandlerId: String,
        saksbehandlerEnhet: String,
        behandlingsId: UUID,
        vedtakId: Long,
        utbetalingsperioder: List<Utbetalingsperiode>
    ) {
        utbetalingsperioder.map {
            mapOf<String, Serializable?>(
                "vedtakid" to vedtakId,
                "datofom" to it.periode.fom.atDay(1).let(Date::valueOf),
                "datotom" to it.periode.tom?.atEndOfMonth()?.let(Date::valueOf),
                "type" to it.type.name,
                "beloep" to it.beloep
            )
        }.let {
            repositoryProxy.opprettFlere(
                query = "INSERT INTO utbetalingsperiode(vedtakid, datofom, datotom, type, beloep) " +
                    "VALUES (:vedtakid, :datofom, :datotom, :type, :beloep)",
                params = it,
                loggtekst = "Attesterer vedtak"
            )
        }
        repositoryProxy.oppdater(
            query = "UPDATE vedtak SET attestant = :attestant, datoAttestert = now(), vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId", // ktlint-disable max-line-length
            params = mapOf(
                "attestant" to saksbehandlerId,
                "vedtakstatus" to VedtakStatus.ATTESTERT.name,
                "behandlingId" to behandlingsId
            ),
            loggtekst = "Attesterer vedtak $vedtakId"
        ).also { require(it == 1) }
    }

    fun underkjennVedtak(behandlingsId: UUID) =
        repositoryProxy.oppdater(
            "UPDATE vedtak SET attestant = null, datoAttestert = null, saksbehandlerId = null, vedtakfattet = false, datoFattet = null, vedtakstatus = :vedtakstatus WHERE behandlingId = :behandlingId", // ktlint-disable max-line-length
            params = mapOf("vedtakstatus" to VedtakStatus.RETURNERT.name, "behandlingId" to behandlingsId),
            loggtekst = "Underkjenner vedtak for behandling $behandlingsId"
        )
}