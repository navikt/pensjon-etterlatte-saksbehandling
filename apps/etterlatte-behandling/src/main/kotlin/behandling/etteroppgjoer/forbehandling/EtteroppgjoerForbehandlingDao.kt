package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.SummerteInntekterAOrdningen
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.SummertePensjonsgivendeInntekter
import no.nav.etterlatte.behandling.hendelse.getLongOrNull
import no.nav.etterlatte.behandling.hendelse.setLong
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
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
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.setNullableBoolean
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

    fun hentForbehandlingerForSak(sakId: SakId): List<EtteroppgjoerForbehandling> =
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
                            aarsak_til_avbrytelse, kommentar_til_avbrytelse, har_vedtak_av_type_opphoer, opphoer_skyldes_doedsfall, opphoer_skyldes_doedsfall_i_etteroppgjoersaar, ikke_mottatt_skatteoppgjoer
                        ) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                        ON CONFLICT (id) DO UPDATE SET
                            status = excluded.status,
                            brev_id = excluded.brev_id,
                            varselbrev_sendt = excluded.varselbrev_sendt,
                            har_mottatt_ny_informasjon = excluded.har_mottatt_ny_informasjon,
                            endring_er_til_ugunst_for_bruker = excluded.endring_er_til_ugunst_for_bruker,
                            beskrivelse_av_ugunst = excluded.beskrivelse_av_ugunst,
                            etteroppgjoer_resultat_type = excluded.etteroppgjoer_resultat_type,
                            aarsak_til_avbrytelse = excluded.aarsak_til_avbrytelse,
                            kommentar_til_avbrytelse = excluded.kommentar_til_avbrytelse,
                            har_vedtak_av_type_opphoer = excluded.har_vedtak_av_type_opphoer,
                            opphoer_skyldes_doedsfall = excluded.opphoer_skyldes_doedsfall,
                            opphoer_skyldes_doedsfall_i_etteroppgjoersaar = excluded.opphoer_skyldes_doedsfall_i_etteroppgjoersaar
                            ikke_mottatt_skatteoppgjoer = excluded.ikke_mottatt_skatteoppgjoer
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
                statement.setNullableBoolean(18, forbehandling.harVedtakAvTypeOpphoer)
                statement.setString(19, forbehandling.opphoerSkyldesDoedsfall?.name)
                statement.setString(20, forbehandling.opphoerSkyldesDoedsfallIEtteroppgjoersaar?.name)
                statement.setBoolean(21, forbehandling.ikkeMottattSkatteoppgjoer)

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
            val statement =
                prepareStatement(
                    """
                        INSERT INTO etteroppgjoer_pensjonsgivendeinntekt (
                        forbehandling_id, loensinntekt, naeringsinntekt, tidspunkt_beregnet, regel_resultat
                    )
                    SELECT ?, loensinntekt, naeringsinntekt, tidspunkt_beregnet, regel_resultat 
                    FROM etteroppgjoer_pensjonsgivendeinntekt
                    WHERE forbehandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, nyForbehandlingId)
            statement.setObject(2, forbehandlingId)

            statement.executeUpdate().also { count ->
                krev(count == 1) {
                    "Kunne ikke kopiere pensjonsgivende inntekter fra behandling=$forbehandlingId til $nyForbehandlingId"
                }
            }
        }
    }

    fun lagrePensjonsgivendeInntekt(
        behandlingId: UUID,
        inntekterFraSkatt: SummertePensjonsgivendeInntekter,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val statement =
                prepareStatement(
                    """
                    INSERT INTO etteroppgjoer_pensjonsgivendeinntekt(
                        forbehandling_id, loensinntekt, naeringsinntekt, tidspunkt_beregnet, regel_resultat
                    ) 
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (forbehandling_id) DO UPDATE SET
                        loensinntekt = excluded.loensinntekt,
                        naeringsinntekt = excluded.naeringsinntekt,
                        tidspunkt_beregnet = excluded.tidspunkt_beregnet,
                        regel_resultat = excluded.regel_resultat
                    """.trimIndent(),
                )

            statement.setObject(1, behandlingId)
            statement.setInt(2, inntekterFraSkatt.loensinntekt)
            statement.setInt(3, inntekterFraSkatt.naeringsinntekt)
            statement.setTidspunkt(4, inntekterFraSkatt.tidspunktBeregnet)
            statement.setJsonb(5, inntekterFraSkatt.regelresultat)

            val result = statement.executeUpdate()
            krev(result == 1) {
                "Kunne ikke lagre pensjonsgivende inntekt for behandlingId=$behandlingId"
            }
        }
    }

    fun hentPensjonsgivendeInntekt(forbehandlingId: UUID): SummertePensjonsgivendeInntekter? =
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

                statement.executeQuery().singleOrNull {
                    toSummertePensjonsgivendeInntekter()
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

    fun hentSummerteInntekterNonNull(forbehandlingId: UUID) =
        krevIkkeNull(hentSummerteInntekter(forbehandlingId)) {
            "Fant ikke summerte inntekter for forbehandling med id=$forbehandlingId"
        }

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

            statement.executeUpdate().also { count ->
                krev(count == 1) {
                    "Kunne ikke kopiere summerter inntekter fra behandling=$forbehandlingId til $nyForbehandlingId"
                }
            }
        }
    }

    private fun ResultSet.toForbehandling(): EtteroppgjoerForbehandling =
        EtteroppgjoerForbehandling(
            id = getString("id").let { UUID.fromString(it) },
            sak =
                Sak(
                    id = SakId(getLong("sak_id")),
                    sakType = enumValueOf(getString("saktype")),
                    ident = getString("fnr"),
                    enhet = Enhetsnummer(getString("enhet")),
                    adressebeskyttelse = getString("adressebeskyttelse")?.let { enumValueOf<AdressebeskyttelseGradering>(it) },
                    erSkjermet = getBoolean("erSkjermet"),
                ),
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
            harVedtakAvTypeOpphoer = getBoolean("har_vedtak_av_type_opphoer"),
            opphoerSkyldesDoedsfall = getString("opphoer_skyldes_doedsfall")?.let { enumValueOf<JaNei>(it) },
            opphoerSkyldesDoedsfallIEtteroppgjoersaar =
                getString(
                    "opphoer_skyldes_doedsfall_i_etteroppgjoersaar",
                )?.let { enumValueOf<JaNei>(it) },
            ikkeMottattSkatteoppgjoer = getBoolean("ikke_mottatt_skatteoppgjoer"),
        )

    private fun ResultSet.toSummertePensjonsgivendeInntekter(): SummertePensjonsgivendeInntekter =
        SummertePensjonsgivendeInntekter(
            loensinntekt = getInt("loensinntekt"),
            naeringsinntekt = getInt("naeringsinntekt"),
            tidspunktBeregnet = getTidspunktOrNull("tidspunkt_beregnet"),
            regelresultat = null,
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
