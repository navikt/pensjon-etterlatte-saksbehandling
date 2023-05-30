package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.param
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.common.UUID30
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

data class UtbetalingNotFoundException(override val message: String) : RuntimeException(message)

class UtbetalingDao(private val dataSource: DataSource) {

    fun opprettUtbetaling(utbetaling: Utbetaling) = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            logger.info("Oppretter utbetaling for vedtakId=${utbetaling.vedtakId.value}")

            queryOf(
                statement = """
                        INSERT INTO utbetaling(id, vedtak_id, behandling_id, behandling_id_til_oppdrag, sak_id, oppdrag, 
                            vedtak, opprettet, avstemmingsnoekkel, endret, stoenadsmottaker, saksbehandler, 
                            saksbehandler_enhet, attestant, attestant_enhet, saktype)
                        VALUES(:id, :vedtakId, :behandlingId, :behandlingIdTilOppdrag, :sakId, :oppdrag,
                            :vedtak, :opprettet, :avstemmingsnoekkel, :endret, :stoenadsmottaker, :saksbehandler, 
                            :saksbehandlerEnhet, :attestant, :attestantEnhet, :saktype)
                        """,
                paramMap = mapOf(
                    "id" to utbetaling.id.param(),
                    "vedtakId" to utbetaling.vedtakId.value.param(),
                    "behandlingId" to utbetaling.behandlingId.value.param(),
                    "behandlingIdTilOppdrag" to utbetaling.behandlingId.shortValue.value.param(),
                    "sakId" to utbetaling.sakId.value.param(),
                    "vedtak" to utbetaling.vedtak.toJson().param(),
                    "opprettet" to utbetaling.opprettet.toTimestamp().param(),
                    "avstemmingsnoekkel" to utbetaling.avstemmingsnoekkel.toTimestamp().param(),
                    "endret" to utbetaling.endret.toTimestamp().param(),
                    "stoenadsmottaker" to utbetaling.stoenadsmottaker.value.param(),
                    "saksbehandler" to utbetaling.saksbehandler.value.param(),
                    "saksbehandlerEnhet" to utbetaling.saksbehandlerEnhet.param(),
                    "attestant" to utbetaling.attestant.value.param(),
                    "attestantEnhet" to utbetaling.attestantEnhet.param(),
                    "oppdrag" to utbetaling.oppdrag?.let { o -> OppdragJaxb.toXml(o) }.param(),
                    "saktype" to utbetaling.sakType.name.param()
                )
            ).let { tx.run(it.asUpdate) }

            utbetaling.utbetalingslinjer.forEach { utbetalingslinje ->
                opprettUtbetalingslinje(utbetalingslinje, tx)
            }
            utbetaling.utbetalingshendelser.forEach { utbetalingshendelse ->
                opprettUtbetalingshendelse(
                    utbetalingshendelse,
                    tx
                )
            }
        }
    }.let { hentUtbetalingNonNull(utbetaling.vedtakId.value) }

    private fun opprettUtbetalingslinje(
        utbetalingslinje: Utbetalingslinje,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
                INSERT INTO utbetalingslinje(id, type, utbetaling_id, erstatter_id, opprettet, periode_fra, periode_til, 
                    beloep, sak_id, klassifikasjonskode, kjoereplan)
                VALUES(:id, :type, :utbetaling_id, :erstatter_id, :opprettet, :periode_fra, :periode_til,  
                    :beloep, :sak_id, :klassifikasjonskode, :kjoereplan)
            """,
            paramMap = mapOf(
                "id" to utbetalingslinje.id.value.param(),
                "type" to utbetalingslinje.type.name.param(),
                "utbetaling_id" to utbetalingslinje.utbetalingId.param(),
                "erstatter_id" to utbetalingslinje.erstatterId?.value.param(),
                "opprettet" to utbetalingslinje.opprettet.toTimestamp().param(),
                "sak_id" to utbetalingslinje.sakId.value.param(),
                "periode_fra" to utbetalingslinje.periode.fra.param(),
                "periode_til" to utbetalingslinje.periode.til.param(),
                "beloep" to utbetalingslinje.beloep.param(),
                "klassifikasjonskode" to utbetalingslinje.klassifikasjonskode.toString().param(),
                "kjoereplan" to utbetalingslinje.kjoereplan.toString().param()
            )
        ).let { tx.run(it.asUpdate) }
    }

    fun nyUtbetalingshendelse(
        vedtakId: Long,
        utbetalingshendelse: Utbetalingshendelse
    ): Utbetaling = using(sessionOf(dataSource)) { session ->
        session.transaction { tx ->
            opprettUtbetalingshendelse(utbetalingshendelse, tx)
        }
    }.let {
        hentUtbetalingNonNull(vedtakId = vedtakId)
    }

    private fun opprettUtbetalingshendelse(
        utbetalingshendelse: Utbetalingshendelse,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
                INSERT INTO utbetalingshendelse(id, utbetaling_id, tidspunkt, status) 
                VALUES(:id, :utbetalingId, :tidspunkt, :status)
            """,
            paramMap = mapOf(
                "id" to utbetalingshendelse.id.param(),
                "utbetalingId" to utbetalingshendelse.utbetalingId.param(),
                "tidspunkt" to utbetalingshendelse.tidspunkt.toTimestamp().param(),
                "status" to utbetalingshendelse.status.name.param()
            )
        ).let { tx.run(it.asUpdate) }
    }

    fun hentUtbetaling(vedtakId: Long): Utbetaling? = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement = """
                    SELECT id, vedtak_id, behandling_id, behandling_id_til_oppdrag,  sak_id, vedtak, opprettet, 
                        avstemmingsnoekkel, endret, stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse, 
                        kvittering_alvorlighetsgrad, kvittering_kode, saksbehandler, saksbehandler_enhet, attestant, 
                        attestant_enhet, saktype 
                    FROM utbetaling 
                    WHERE vedtak_id = :vedtakId
                    """,
            paramMap = mapOf("vedtakId" to vedtakId.param())
        ).let {
            session.run(
                it.map { row ->
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.uuid("id"))
                    val utbetalingshendelser = hentUtbetalingsHendelserForUtbetaling(row.uuid("id"))
                    toUtbetaling(row, utbetalingslinjer, utbetalingshendelser)
                }.asSingle
            )
        }
    }

    private fun hentUtbetalingsHendelserForUtbetaling(utbetalingId: UUID): List<Utbetalingshendelse> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                  SELECT id, utbetaling_id, tidspunkt, status  
                  FROM utbetalingshendelse 
                  WHERE utbetaling_id = :utbetalingId
               """,
                paramMap = mapOf(
                    "utbetalingId" to utbetalingId.param()
                )
            ).let {
                session.run(it.map(::toUtbetalingshendelse).asList)
            }
        }

    private fun hentUtbetalingslinjerForUtbetaling(utbetalingId: UUID): List<Utbetalingslinje> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, type, utbetaling_id, erstatter_id, opprettet, periode_fra, periode_til, beloep, sak_id,
                        klassifikasjonskode, kjoereplan
                    FROM utbetalingslinje 
                    WHERE utbetaling_id = :utbetalingId
                    """,
                paramMap = mapOf("utbetalingId" to utbetalingId.param())
            ).let { session.run(it.map(::toUtbetalingslinje).asList) }
        }

    fun hentDupliserteUtbetalingslinjer(
        utbetalingslinjeIder: List<Utbetalingsperiode>,
        utbetalingId: Long
    ): List<Utbetalingslinje> = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement = """
                    SELECT ul.id, ul.type, ul.utbetaling_id, ul.erstatter_id, ul.opprettet, ul.periode_fra, ul.periode_til, ul.beloep, ul.sak_id,
                        ul.klassifikasjonskode, ul.kjoereplan
                    FROM utbetalingslinje as ul 
                    INNER JOIN utbetaling as u
                        ON ul.utbetaling_id = u.id
                    WHERE ul.id = ANY(:utbetalingId) AND 
                    u.vedtak_id != :vedtakId 
                    """,
            paramMap = mapOf(
                "utbetalingId" to session.createArrayOf("bigint", utbetalingslinjeIder.map { it.id }),
                "vedtakId" to utbetalingId.param()
            )
        ).let { session.run(it.map(::toUtbetalingslinje).asList) }
    }

    /**
     * Henter godkjente utbetalinger av en gitt sakType med utbetalingslinjer som er opprettet senest
     * opprettetFramTilOgMed og er aktive fra og med aktivFraOgMed
     */
    fun hentUtbetalingerForKonsistensavstemming(
        aktivFraOgMed: Tidspunkt,
        opprettetFramTilOgMed: Tidspunkt,
        saktype: Saktype
    ): List<Utbetaling> = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement = """
                SELECT DISTINCT (u.id), vedtak_id, behandling_id, behandling_id_til_oppdrag, u.sak_id, vedtak, u.opprettet,
                       avstemmingsnoekkel, endret, stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse,
                       kvittering_alvorlighetsgrad, kvittering_kode, saksbehandler, saksbehandler_enhet, attestant, 
                       attestant_enhet, u.saktype
                FROM utbetaling u
                         INNER JOIN utbetalingslinje ul on u.id = ul.utbetaling_id
                WHERE u.id in (SELECT utbetaling_id FROM utbetalingshendelse where status = 'GODKJENT')
                AND COALESCE(ul.periode_til, :loependeFom) >= :loependeFom AND ul.opprettet <= :opprettetTom
                AND u.saktype = :saktype
            """.trimIndent(),
            paramMap = mapOf(
                "loependeFom" to aktivFraOgMed.toTimestamp().param(),
                "opprettetTom" to opprettetFramTilOgMed.toTimestamp().param(),
                "saktype" to saktype.name.param()
            )
        ).let {
            session.run(
                it.map { row ->
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.uuid("id"))
                    val utbetalingshendelser = hentUtbetalingsHendelserForUtbetaling(row.uuid("id"))
                    toUtbetaling(row, utbetalingslinjer, utbetalingshendelser)
                }.asList
            )
        }
    }

    fun hentUtbetalingerForGrensesnittavstemming(
        fraOgMed: Tidspunkt,
        til: Tidspunkt,
        saktype: Saktype
    ): List<Utbetaling> = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement = """
                    SELECT id, vedtak_id, behandling_id, behandling_id_til_oppdrag, sak_id, vedtak, opprettet, 
                        avstemmingsnoekkel, endret, stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse, 
                        kvittering_alvorlighetsgrad, kvittering_kode, saksbehandler, saksbehandler_enhet, attestant, 
                        attestant_enhet, saktype 
                    FROM utbetaling
                    WHERE avstemmingsnoekkel >= :fraOgMed AND avstemmingsnoekkel < :til
                    AND saktype = :saktype
                    """,
            paramMap = mapOf(
                "fraOgMed" to fraOgMed.toTimestamp().param(),
                "til" to til.toTimestamp().param(),
                "saktype" to saktype.name.param()
            )
        ).let {
            session.run(
                it.map { row ->
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.uuid("id"))
                    val utbetalingshendelser = hentUtbetalingsHendelserForUtbetaling(row.uuid("id"))
                    toUtbetaling(row, utbetalingslinjer, utbetalingshendelser)
                }.asList
            )
        }
    }

    fun hentUtbetalinger(sakId: Long): List<Utbetaling> = using(sessionOf(dataSource)) { session ->
        queryOf(
            statement = """
                    SELECT id, vedtak_id, behandling_id, behandling_id_til_oppdrag, sak_id, vedtak, opprettet, 
                        avstemmingsnoekkel, endret, stoenadsmottaker, oppdrag, kvittering, kvittering_beskrivelse, 
                        kvittering_alvorlighetsgrad, kvittering_kode, saksbehandler, saksbehandler_enhet, attestant, 
                        attestant_enhet, saktype 
                    FROM utbetaling
                    WHERE sak_id = :sakId
                    """,
            paramMap = mapOf(
                "sakId" to sakId.param()
            )
        ).let {
            session.run(
                it.map { row ->
                    val utbetalingslinjer = hentUtbetalingslinjerForUtbetaling(row.uuid("id"))
                    val utbetalingshendelser = hentUtbetalingsHendelserForUtbetaling(row.uuid("id"))
                    toUtbetaling(row, utbetalingslinjer, utbetalingshendelser)
                }.asList
            )
        }
    }

    fun oppdaterKvittering(oppdragMedKvittering: Oppdrag, endret: Tidspunkt, utbetalingId: UUID) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                logger.info("Oppdaterer kvittering i utbetaling for vedtakId=${oppdragMedKvittering.vedtakId()}")

                queryOf(
                    statement = """
                    UPDATE utbetaling 
                    SET kvittering = :kvittering, kvittering_beskrivelse = :beskrivelse, 
                        kvittering_alvorlighetsgrad = :alvorlighetsgrad, kvittering_kode = :kode, 
                        endret = :endret 
                    WHERE vedtak_id = :vedtakId
                    """,
                    paramMap = mapOf(
                        "kvittering" to OppdragJaxb.toXml(oppdragMedKvittering).param(),
                        "beskrivelse" to oppdragMedKvittering.mmel.beskrMelding.param(),
                        "alvorlighetsgrad" to oppdragMedKvittering.mmel.alvorlighetsgrad.param(),
                        "kode" to oppdragMedKvittering.mmel.kodeMelding.param(),
                        "endret" to endret.toTimestamp().param(),
                        "vedtakId" to oppdragMedKvittering.vedtakId().param()
                    )
                ).let { tx.run(it.asUpdate) }
                    .also { require(it == 1) { "Kunne ikke oppdatere kvittering i utbetaling" } }

                opprettUtbetalingshendelse(
                    Utbetalingshendelse(
                        UUID.randomUUID(),
                        utbetalingId,
                        endret,
                        statusFraKvittering(oppdragMedKvittering.mmel.alvorlighetsgrad)
                    ),
                    tx
                )
            }.let { hentUtbetalingNonNull(oppdragMedKvittering.vedtakId()) }
        }

    private fun hentUtbetalingNonNull(vedtakId: Long): Utbetaling = hentUtbetaling(vedtakId)
        ?: throw UtbetalingNotFoundException("Utbetaling for vedtak med vedtakId=$vedtakId finnes ikke")

    private fun toUtbetaling(
        row: Row,
        utbetalingslinjer: List<Utbetalingslinje>,
        utbetalingshendelser: List<Utbetalingshendelse>
    ) = with(row) {
        Utbetaling(
            id = uuid("id"),
            sakId = SakId(long("sak_id")),
            sakType = Saktype.valueOf(string("saktype")),
            behandlingId = BehandlingId(
                value = uuid("behandling_id"),
                shortValue = UUID30(string("behandling_id_til_oppdrag"))
            ),
            vedtakId = VedtakId(long("vedtak_id")),
            opprettet = sqlTimestamp("opprettet").toTidspunkt(),
            endret = sqlTimestamp("endret").toTidspunkt(),
            avstemmingsnoekkel = sqlTimestamp("avstemmingsnoekkel").toTidspunkt(),
            stoenadsmottaker = Foedselsnummer(string("stoenadsmottaker")),
            saksbehandler = NavIdent(string("saksbehandler")),
            saksbehandlerEnhet = string("saksbehandler_enhet"),
            attestant = NavIdent(string("attestant")),
            attestantEnhet = string("attestant_enhet"),
            vedtak = string("vedtak").let { vedtak -> objectMapper.readValue(vedtak) },
            oppdrag = string("oppdrag").let(OppdragJaxb::toOppdrag),
            kvittering = stringOrNull("kvittering")?.let {
                Kvittering(
                    oppdrag = OppdragJaxb.toOppdrag(it),
                    alvorlighetsgrad = string("kvittering_alvorlighetsgrad"),
                    beskrivelse = stringOrNull("kvittering_beskrivelse"),
                    kode = stringOrNull("kvittering_kode")
                )
            },
            utbetalingslinjer = utbetalingslinjer,
            utbetalingshendelser = utbetalingshendelser
        )
    }

    private fun toUtbetalingslinje(row: Row) = with(row) {
        Utbetalingslinje(
            id = UtbetalingslinjeId(long("id")),
            type = string("type").let { Utbetalingslinjetype.valueOf(it) },
            utbetalingId = uuid("utbetaling_id"),
            erstatterId = longOrNull("erstatter_id")?.let { UtbetalingslinjeId(it) },
            opprettet = sqlTimestamp("opprettet").toTidspunkt(),
            sakId = SakId(long("sak_id")),
            periode = PeriodeForUtbetaling(
                fra = localDate("periode_fra"),
                til = localDateOrNull("periode_til")
            ),
            beloep = bigDecimalOrNull("beloep"),
            klassifikasjonskode = OppdragKlassifikasjonskode.fraString(string("klassifikasjonskode")),
            kjoereplan = string("kjoereplan").let { Kjoereplan.fraKode(it) }
        )
    }

    private fun toUtbetalingshendelse(row: Row) = with(row) {
        Utbetalingshendelse(
            id = uuid("id"),
            utbetalingId = uuid("utbetaling_id"),
            tidspunkt = sqlTimestamp("tidspunkt").toTidspunkt(),
            status = string("status").let(UtbetalingStatus::valueOf)
        )
    }

    private fun statusFraKvittering(alvorlighetsgrad: String) = when (alvorlighetsgrad) {
        "00" -> UtbetalingStatus.GODKJENT
        "04" -> UtbetalingStatus.GODKJENT_MED_FEIL
        "08" -> UtbetalingStatus.AVVIST
        "12" -> UtbetalingStatus.FEILET
        else -> UtbetalingStatus.FEILET
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingDao::class.java)
    }
}