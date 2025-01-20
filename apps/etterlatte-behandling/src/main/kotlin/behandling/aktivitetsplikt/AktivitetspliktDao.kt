package no.nav.etterlatte.behandling.aktivitetsplikt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetType
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun finnSenesteAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                |SELECT id, 
                | behandling_id,
                | aktivitet,
                | opprettet,
                | opprettet_av
                |FROM aktivitetsplikt_oppfolging
                |WHERE behandling_id = ?
                |ORDER BY id DESC
                |LIMIT 1
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)
                stmt
                    .executeQuery()
                    .toList {
                        AktivitetspliktOppfolging(
                            behandlingId = getUUID("behandling_id"),
                            aktivitet = getString("aktivitet"),
                            opprettet = getTidspunkt("opprettet"),
                            opprettetAv = getString("opprettet_av"),
                        )
                    }.firstOrNull()
            }
        }

    fun lagre(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
            |INSERT INTO aktivitetsplikt_oppfolging(behandling_id, aktivitet, opprettet_av) 
            |VALUES (?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, behandlingId)
            stmt.setString(2, nyOppfolging.aktivitet)
            stmt.setString(3, navIdent)

            stmt.executeUpdate()
        }
    }

    fun hentAktiviteterForBehandling(behandlingId: UUID): List<AktivitetspliktAktivitet> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitet
                        WHERE behandling_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)

                stmt.executeQuery().toList { toAktivitet() }
            }
        }

    fun hentAktiviteterForSak(sakId: SakId): List<AktivitetspliktAktivitet> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitet
                        WHERE sak_id = ?
                        """.trimMargin(),
                    )
                stmt.setSakId(1, sakId)

                stmt.executeQuery().toList { toAktivitet() }
            }
        }

    fun opprettAktivitet(
        behandlingId: UUID,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitet(id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setSakId(2, aktivitet.sakId)
            stmt.setObject(3, behandlingId)
            stmt.setString(4, aktivitet.type.name)
            stmt.setDate(5, Date.valueOf(aktivitet.fom))
            stmt.setDate(6, aktivitet.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(7, kilde.toJson())
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, aktivitet.beskrivelse)

            stmt.executeUpdate()
        }
    }

    fun opprettAktivitetForSak(
        sakId: SakId,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitet(id, sak_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setSakId(2, sakId)
            stmt.setString(3, aktivitet.type.name)
            stmt.setDate(4, Date.valueOf(aktivitet.fom))
            stmt.setDate(5, aktivitet.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(6, kilde.toJson())
            stmt.setString(7, kilde.toJson())
            stmt.setString(8, aktivitet.beskrivelse)

            stmt.executeUpdate()
        }
    }

    fun oppdaterAktivitetForBehandling(
        behandlingId: UUID,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        UPDATE aktivitetsplikt_aktivitet
                        SET aktivitet_type = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ?
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )
            stmt.setString(1, aktivitet.type.name)
            stmt.setDate(2, Date.valueOf(aktivitet.fom))
            stmt.setDate(3, aktivitet.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(4, kilde.toJson())
            stmt.setString(5, aktivitet.beskrivelse)
            stmt.setObject(6, krevIkkeNull(aktivitet.id) { "Aktivitet id mangler" })
            stmt.setObject(7, behandlingId)

            val endret = stmt.executeUpdate()
            krev(endret == 1) {
                "Kunne ikke endre aktivitet for behandlingId: $behandlingId id: ${aktivitet.id}"
            }
            endret
        }
    }

    fun oppdaterAktivitetForSak(
        sakId: SakId,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        UPDATE aktivitetsplikt_aktivitet
                        SET aktivitet_type = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ?
                        WHERE id = ? AND sak_id = ?
                    """.trimMargin(),
                )
            stmt.setString(1, aktivitet.type.name)
            stmt.setDate(2, Date.valueOf(aktivitet.fom))
            stmt.setDate(3, aktivitet.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(4, kilde.toJson())
            stmt.setString(5, aktivitet.beskrivelse)
            stmt.setObject(6, krevIkkeNull(aktivitet.id) { "Aktivitet id mangler" })
            stmt.setSakId(7, sakId)

            val endret = stmt.executeUpdate()
            krev(endret == 1) {
                "Kunne ikke endre aktivitet for sakId: $sakId id: ${aktivitet.id}"
            }
        }
    }

    fun slettAktivitet(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        DELETE FROM aktivitetsplikt_aktivitet
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, aktivitetId)
            stmt.setObject(2, behandlingId)

            val slettet = stmt.executeUpdate()
            if (slettet != 1) {
                logger.warn("Kunne ikke slette aktivitetId: $aktivitetId for behandlingId: $behandlingId")
            }
        }
    }

    fun slettAktivitetForSak(
        aktivitetId: UUID,
        sakId: SakId,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        DELETE FROM aktivitetsplikt_aktivitet
                        WHERE id = ? AND sak_id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, aktivitetId)
            stmt.setSakId(2, sakId)

            val slettet = stmt.executeUpdate()
            if (slettet != 1) {
                logger.warn("Kunne ikke slette aktivitetId: $aktivitetId for sakId: $sakId")
            }
        }
    }

    fun kopierAktiviteter(
        forrigeBehandlingId: UUID,
        nyBehandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitet (id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse)
                        (SELECT gen_random_uuid(), sak_id, ?, aktivitet_type, fom, tom, opprettet, endret, beskrivelse FROM aktivitetsplikt_aktivitet WHERE behandling_id = ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, nyBehandlingId)
            stmt.setObject(2, forrigeBehandlingId)

            stmt.executeUpdate()
        }
    }

    private fun ResultSet.toAktivitet() =
        AktivitetspliktAktivitet(
            id = getUUID("id"),
            sakId = SakId(getLong("sak_id")),
            type = AktivitetspliktAktivitetType.valueOf(getString("aktivitet_type")),
            fom = getDate("fom").toLocalDate(),
            tom = getDate("tom")?.toLocalDate(),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
            beskrivelse = getString("beskrivelse"),
        )
}

data class AktivitetspliktAktivitet(
    val id: UUID,
    val sakId: SakId,
    val type: AktivitetspliktAktivitetType,
    val fom: LocalDate,
    val tom: LocalDate?,
    val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
) {
    fun toDto(): AktivitetDto =
        AktivitetDto(
            typeAktivitet =
                when (this.type) {
                    AktivitetspliktAktivitetType.ARBEIDSTAKER -> AktivitetType.ARBEIDSTAKER
                    AktivitetspliktAktivitetType.SELVSTENDIG_NAERINGSDRIVENDE -> AktivitetType.SELVSTENDIG_NAERINGSDRIVENDE
                    AktivitetspliktAktivitetType.ETABLERER_VIRKSOMHET -> AktivitetType.ETABLERER_VIRKSOMHET
                    AktivitetspliktAktivitetType.ARBEIDSSOEKER -> AktivitetType.ARBEIDSSOEKER
                    AktivitetspliktAktivitetType.UTDANNING -> AktivitetType.UTDANNING
                    AktivitetspliktAktivitetType.INGEN_AKTIVITET -> AktivitetType.INGEN_AKTIVITET
                    AktivitetspliktAktivitetType.OPPFOELGING_LOKALKONTOR -> AktivitetType.OPPFOELGING_LOKALKONTOR
                },
            fom = fom,
            tom = tom,
        )
}

data class LagreAktivitetspliktAktivitet(
    val id: UUID? = null,
    val sakId: SakId,
    val type: AktivitetspliktAktivitetType,
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beskrivelse: String,
)

enum class AktivitetspliktAktivitetType {
    ARBEIDSTAKER,
    SELVSTENDIG_NAERINGSDRIVENDE,
    ETABLERER_VIRKSOMHET,
    ARBEIDSSOEKER,
    UTDANNING,
    INGEN_AKTIVITET,
    OPPFOELGING_LOKALKONTOR,
}
