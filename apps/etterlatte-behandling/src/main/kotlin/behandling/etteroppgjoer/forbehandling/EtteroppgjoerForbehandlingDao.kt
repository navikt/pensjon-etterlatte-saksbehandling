package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.SummerteInntekterAOrdningen
import no.nav.etterlatte.behandling.hendelse.getLongOrNull
import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.ResultSet
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerForbehandlingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger: Logger = LoggerFactory.getLogger(EtteroppgjoerForbehandlingDao::class.java)

    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer_behandling t INNER JOIN sak s on t.sak_id = s.id
                        WHERE t.id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().singleOrNull { toForbehandling() }
            }
        }

    fun lagreAvbruttAarsak(
        behandlingId: UUID,
        aarsakTilAvbrytelse: AarsakTilAvbryteForbehandling,
        kommentar: String,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement("UPDATE etteroppgjoer_behandling SET aarsak_til_avbrytelse = ?, kommentar_til_avbrytelse = ? WHERE id = ?")

            stmt.setString(1, aarsakTilAvbrytelse.name)
            stmt.setString(2, kommentar)
            stmt.setObject(3, behandlingId)
            krev(stmt.executeUpdate() == 1) {
                "Kunne ikke lagre avbrutt aarsak for forbehandling=$behandlingId"
            }
        }
    }

    fun hentForbehandlinger(sakId: SakId): List<EtteroppgjoerForbehandling> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *  
                        FROM etteroppgjoer_behandling t INNER JOIN sak s on t.sak_id = s.id
                        WHERE t.sak_id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, sakId.sakId)
                statement.executeQuery().toList { toForbehandling() }
            }
        }

    fun lagreForbehandling(forbehandling: EtteroppgjoerForbehandling) =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO etteroppgjoer_behandling(
                            id, status, sak_id, opprettet, aar, fom, tom, brev_id, kopiert_fra, siste_iverksatte_behandling, har_mottatt_ny_informasjon, endring_er_til_ugunst_for_bruker, beskrivelse_av_ugunst, varselbrev_sendt, etteroppgjoer_resultat_type,
                            aarsak_til_avbrytelse, kommentar_til_avbrytelse
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (id) DO UPDATE SET
                            status = excluded.status,
                            brev_id = excluded.brev_id,
                            varselbrev_sendt = excluded.varselbrev_sendt,
                            har_mottatt_ny_informasjon = excluded.har_mottatt_ny_informasjon,
                            endring_er_til_ugunst_for_bruker = excluded.endring_er_til_ugunst_for_bruker,
                            beskrivelse_av_ugunst = excluded.beskrivelse_av_ugunst,
                            etteroppgjoer_resultat_type = excluded.etteroppgjoer_resultat_type,
                            aarsak_til_avbrytelse = excluded.aarsak_til_avbrytelse,
                            kommentar_til_avbrytelse = excluded.kommentar_til_avbrytelse
                        """.trimIndent(),
                    )
                statement.setObject(1, forbehandling.id)
                statement.setString(2, forbehandling.status.name)
                statement.setSakId(3, forbehandling.sak.id)
                statement.setTidspunkt(4, forbehandling.opprettet)
                statement.setInt(5, forbehandling.aar)
                statement.setDate(6, Date.valueOf(forbehandling.innvilgetPeriode.fom.atDay(1)))
                statement.setDate(
                    7,
                    Date.valueOf(
                        forbehandling.innvilgetPeriode.tom?.atEndOfMonth()
                            ?: throw InternfeilException("Etteroppgjoer forbehandling mangler periode"),
                    ),
                )
                statement.setLong(8, forbehandling.brevId)
                statement.setObject(9, forbehandling.kopiertFra)
                statement.setObject(10, forbehandling.sisteIverksatteBehandlingId)
                statement.setString(11, forbehandling.harMottattNyInformasjon?.name)
                statement.setString(12, forbehandling.endringErTilUgunstForBruker?.name)
                statement.setString(13, forbehandling.beskrivelseAvUgunst)
                statement.setDate(14, forbehandling.varselbrevSendt?.let { Date.valueOf(it) })
                statement.setString(15, forbehandling.etteroppgjoerResultatType?.name)
                statement.setString(16, forbehandling.aarsakTilAvbrytelse?.name)
                statement.setString(17, forbehandling.aarsakTilAvbrytelseBeskrivelse.orEmpty())

                statement.executeUpdate().also {
                    krev(it == 1) {
                        "Kunne ikke lagre forbehandling etteroppgjør for sakId=${forbehandling.sak.id}"
                    }
                }
            }
        }

    fun kopierPensjonsgivendeInntekt(
        forbehandlingId: UUID,
        nyForbehandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val selectStatement =
                prepareStatement(
                    """
                    SELECT inntektsaar, skatteordning, loensinntekt, naeringsinntekt, fiske_fangst_familiebarnehage
                    FROM etteroppgjoer_pensjonsgivendeinntekt
                    WHERE forbehandling_id = ?
                    """.trimIndent(),
                )

            selectStatement.setObject(1, forbehandlingId)
            val resultSet = selectStatement.executeQuery()

            val insertStatement =
                prepareStatement(
                    """
                    INSERT INTO etteroppgjoer_pensjonsgivendeinntekt (
                        id, forbehandling_id, inntektsaar, skatteordning, loensinntekt, naeringsinntekt, fiske_fangst_familiebarnehage
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                )

            var count = 0
            while (resultSet.next()) {
                insertStatement.setObject(1, UUID.randomUUID())
                insertStatement.setObject(2, nyForbehandlingId)
                insertStatement.setInt(3, resultSet.getInt("inntektsaar"))
                insertStatement.setString(4, resultSet.getString("skatteordning"))
                insertStatement.setInt(5, resultSet.getInt("loensinntekt"))
                insertStatement.setInt(6, resultSet.getInt("naeringsinntekt"))
                insertStatement.setInt(7, resultSet.getInt("fiske_fangst_familiebarnehage"))

                insertStatement.addBatch()
                count++
            }

            val result = insertStatement.executeBatch()

            krev(result.size == count) {
                "Kunne ikke kopiere alle pensjonsgivende inntekter fra behandling=$forbehandlingId til $nyForbehandlingId"
            }
        }
    }

    fun lagrePensjonsgivendeInntekt(
        inntekterFraSkatt: PensjonsgivendeInntektFraSkatt,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO etteroppgjoer_pensjonsgivendeinntekt(
                        id, forbehandling_id, inntektsaar, skatteordning, loensinntekt, naeringsinntekt,fiske_fangst_familiebarnehage
                    ) 
                    VALUES (?, ?, ?, ?, ?, ?, ?) 
                    """.trimIndent(),
                )

            for (inntekt in inntekterFraSkatt.inntekter) {
                statement.setObject(1, UUID.randomUUID())
                statement.setObject(2, behandlingId)
                statement.setInt(3, inntekt.inntektsaar)
                statement.setString(4, inntekt.skatteordning)
                statement.setInt(5, inntekt.loensinntekt)
                statement.setInt(6, inntekt.naeringsinntekt)
                statement.setInt(7, inntekt.fiskeFangstFamiliebarnehage)

                statement.addBatch()
            }

            val result = statement.executeBatch()
            krev(result.size == inntekterFraSkatt.inntekter.size) {
                "Kunne ikke lagre alle pensjonsgivendeInntekter for behandlingId=$behandlingId"
            }
        }
    }

    fun hentPensjonsgivendeInntekt(forbehandlingId: UUID): PensjonsgivendeInntektFraSkatt? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM etteroppgjoer_pensjonsgivendeinntekt
                        WHERE forbehandling_id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, forbehandlingId)
                val pensjonsgivendeInntekter =
                    statement.executeQuery().toList {
                        toPensjonsgivendeInntekt()
                    }

                if (pensjonsgivendeInntekter.isEmpty()) {
                    null
                } else {
                    PensjonsgivendeInntektFraSkatt(
                        inntektsaar = pensjonsgivendeInntekter.first().inntektsaar,
                        inntekter = pensjonsgivendeInntekter,
                    )
                }
            }
        }

    fun lagreSummerteInntekter(
        forbehandlingId: UUID,
        summerteInntekterAOrdningen: SummerteInntekterAOrdningen,
    ) {
        krevIkkeNull(summerteInntekterAOrdningen.regelresultat) {
            "Kan ikke lagre inntekter for for behandling $forbehandlingId siden regelresultat mangler"
        }
        connectionAutoclosing.hentConnection { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO etteroppgjoer_summerte_inntekter (
                        forbehandling_id, afp, loenn, oms, tidspunkt_beregnet, regel_resultat
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (forbehandling_id) DO UPDATE SET
                        afp = excluded.afp,
                        loenn = excluded.loenn,
                        oms = excluded.oms,
                        tidspunkt_beregnet = excluded.tidspunkt_beregnet,
                        regel_resultat = excluded.regel_resultat
                    """.trimIndent(),
                )
            statement.setObject(1, forbehandlingId)
            statement.setJsonb(2, summerteInntekterAOrdningen.afp)
            statement.setJsonb(3, summerteInntekterAOrdningen.loenn)
            statement.setJsonb(4, summerteInntekterAOrdningen.oms)
            statement.setTidspunkt(5, summerteInntekterAOrdningen.tidspunktBeregnet)
            statement.setJsonb(6, summerteInntekterAOrdningen.regelresultat)
            statement.executeUpdate()
        }
        logger.info("Lagret inntekter for forbehandling $forbehandlingId")
    }

    fun hentSummerteInntekterNonNull(forbehandlingId: UUID) = krevIkkeNull(hentSummerteInntekter(forbehandlingId)) { "Må ha en verdi" }

    fun hentSummerteInntekter(forbehandlingId: UUID): SummerteInntekterAOrdningen? =
        connectionAutoclosing.hentConnection { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    SELECT afp, loenn, oms, tidspunkt_beregnet, forbehandling_id FROM etteroppgjoer_summerte_inntekter
                    WHERE forbehandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, forbehandlingId)
            statement.executeQuery().singleOrNull {
                toSummerteInntekter()
            }
        }

    fun kopierSummerteInntekter(
        forbehandlingId: UUID,
        nyForbehandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO etteroppgjoer_summerte_inntekter (
                        forbehandling_id, afp, loenn, oms, tidspunkt_beregnet, regel_resultat
                    )
                    SELECT ?, afp, loenn, oms, tidspunkt_beregnet, regel_resultat
                    FROM etteroppgjoer_summerte_inntekter
                    WHERE forbehandling_id = ?
                    """.trimIndent(),
                )

            statement.setObject(1, nyForbehandlingId)
            statement.setObject(2, forbehandlingId)

            statement.executeUpdate().also {
                krev(it == 1) {
                    "Kunne ikke kopiere summerter inntekter fra behandling=$forbehandlingId til $nyForbehandlingId"
                }
            }
        }
    }

    private fun ResultSet.toForbehandling(): EtteroppgjoerForbehandling =
        EtteroppgjoerForbehandling(
            id = getString("id").let { UUID.fromString(it) },
            hendelseId = UUID.randomUUID(), // TODO
            sak =
                Sak(
                    id = SakId(getLong("sak_id")),
                    sakType = enumValueOf(getString("saktype")),
                    ident = getString("fnr"),
                    enhet = Enhetsnummer(getString("enhet")),
                    adressebeskyttelse = getString("adressebeskyttelse")?.let { enumValueOf<AdressebeskyttelseGradering>(it) },
                    erSkjermet = getBoolean("erSkjermet"),
                ),
            // sekvensnummerSkatt = "123", // TODO
            opprettet = getTidspunkt("opprettet"),
            status = EtteroppgjoerForbehandlingStatus.valueOf(getString("status")),
            aar = getInt("aar"),
            innvilgetPeriode =
                Periode(
                    fom = getDate("fom").let { YearMonth.from(it.toLocalDate()) },
                    tom = getDate("tom").let { YearMonth.from(it.toLocalDate()) },
                ),
            brevId = getLongOrNull("brev_id"),
            kopiertFra = getString("kopiert_fra")?.let { UUID.fromString(it) },
            sisteIverksatteBehandlingId = getString("siste_iverksatte_behandling").let { UUID.fromString(it) },
            harMottattNyInformasjon = getString("har_mottatt_ny_informasjon")?.let { enumValueOf<JaNei>(it) },
            endringErTilUgunstForBruker = getString("endring_er_til_ugunst_for_bruker")?.let { enumValueOf<JaNei>(it) },
            beskrivelseAvUgunst = getString("beskrivelse_av_ugunst"),
            varselbrevSendt = getDate("varselbrev_sendt")?.toLocalDate(),
            etteroppgjoerResultatType = getString("etteroppgjoer_resultat_type")?.let { enumValueOf<EtteroppgjoerResultatType>(it) },
            aarsakTilAvbrytelse = getString("aarsak_til_avbrytelse")?.let { enumValueOf<AarsakTilAvbryteForbehandling>(it) },
            aarsakTilAvbrytelseBeskrivelse = getString("kommentar_til_avbrytelse"),
        )

    private fun ResultSet.toPensjonsgivendeInntekt(): PensjonsgivendeInntekt =
        PensjonsgivendeInntekt(
            skatteordning = getString("skatteordning"),
            loensinntekt = getInt("loensinntekt"),
            naeringsinntekt = getInt("naeringsinntekt"),
            fiskeFangstFamiliebarnehage = getInt("fiske_fangst_familiebarnehage"),
            inntektsaar = getInt("inntektsaar"),
        )
}

fun ResultSet.toSummerteInntekter(): SummerteInntekterAOrdningen =
    SummerteInntekterAOrdningen(
        afp = getString("afp").let { objectMapper.readValue(it) },
        loenn = getString("loenn").let { objectMapper.readValue(it) },
        oms = getString("oms").let { objectMapper.readValue(it) },
        tidspunktBeregnet = getTidspunkt("tidspunkt_beregnet"),
        regelresultat = null,
    )
