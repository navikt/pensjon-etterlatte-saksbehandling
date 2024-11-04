package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktVurderingOpprettetDato
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktAktivitetsgradDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.setSakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktAktivitetsgradDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun upsertAktivitetsgradForOppgave(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        sakId: SakId,
        kilde: Grunnlagsopplysning.Kilde,
        oppgaveId: UUID? = null,
        behandlingId: UUID? = null,
    ) = connectionAutoclosing.hentConnection {
        check(oppgaveId != null || behandlingId != null) {
            "Kan ikke opprette aktivitetsgrad som ikke er koblet på en behandling eller oppgave"
        }

        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET 
                            aktivitetsgrad = EXCLUDED.aktivitetsgrad,
                            endret = excluded.endret,
                            beskrivelse = excluded.beskrivelse,
                            skjoennsmessig_vurdering = excluded.skjoennsmessig_vurdering,
                            fom = excluded.fom,
                            tom = excluded.tom,
                            vurdert_fra_12_mnd = excluded.vurdert_fra_12_mnd
                            
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setSakId(2, sakId)
            stmt.setObject(3, behandlingId)
            stmt.setObject(4, oppgaveId)
            stmt.setString(5, aktivitetsgrad.aktivitetsgrad.name)
            stmt.setDate(6, Date.valueOf(aktivitetsgrad.fom))
            stmt.setDate(7, aktivitetsgrad.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, kilde.toJson())
            stmt.setString(10, aktivitetsgrad.beskrivelse)
            stmt.setString(11, aktivitetsgrad.skjoennsmessigVurdering?.name)
            stmt.setBoolean(12, aktivitetsgrad.vurdertFra12Mnd)

            stmt.executeUpdate()
        }
    }

    fun oppdaterAktivitetsgrad(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        kilde: Grunnlagsopplysning.Kilde,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        if (aktivitetsgrad.id == null) {
            throw InternfeilException(
                "Kan ikke oppdatere en aktivitetsgrad som ikke har en id. " +
                    "BehandlingId=$behandlingId",
            )
        }
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        UPDATE aktivitetsplikt_aktivitetsgrad
                        SET  aktivitetsgrad = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ?, skjoennsmessig_vurdering = ?, vurdert_fra_12_mnd = ?
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )

            stmt.setString(1, aktivitetsgrad.aktivitetsgrad.name)
            stmt.setDate(2, Date.valueOf(aktivitetsgrad.fom))
            stmt.setDate(3, aktivitetsgrad.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(4, kilde.toJson())
            stmt.setString(5, aktivitetsgrad.beskrivelse)
            stmt.setString(6, aktivitetsgrad.skjoennsmessigVurdering?.name)
            stmt.setBoolean(7, aktivitetsgrad.vurdertFra12Mnd)
            stmt.setObject(8, aktivitetsgrad.id)
            stmt.setObject(9, behandlingId)

            stmt.executeUpdate()
        }
    }

    fun hentAktivitetsgradForOppgave(oppgaveId: UUID): List<AktivitetspliktAktivitetsgrad> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE oppgave_id = ?
                        ORDER BY fom ASC NULLS FIRST
                        """.trimMargin(),
                    )
                stmt.setObject(1, oppgaveId)

                stmt.executeQuery().toList { toAktivitetsgrad() }
            }
        }

    fun hentNyesteAktivitetsgrad(sakId: SakId): List<AktivitetspliktAktivitetsgrad> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT behandling_id, oppgave_id FROM aktivitetsplikt_aktivitetsgrad
                        WHERE sak_id = ?
                        ORDER BY endret::jsonb ->> 'tidspunkt' DESC
                        LIMIT 1
                        """.trimIndent(),
                    )
                stmt.setSakId(1, sakId)

                val (behandlingId, oppgaveId) =
                    stmt.executeQuery().singleOrNull {
                        getString("behandling_id") to getString("oppgave_id")
                    } ?: return@hentConnection emptyList()

                if (behandlingId != null) {
                    hentAktivitetsgradForBehandling(UUID.fromString(behandlingId))
                } else {
                    requireNotNull(oppgaveId) {
                        "Har en vurdering av aktivitet som ikke er knyttet til en oppgave eller en behandling"
                    }
                    hentAktivitetsgradForOppgave(UUID.fromString(oppgaveId))
                }
            }
        }

    fun hentAktivitetsgradForBehandling(behandlingId: UUID): List<AktivitetspliktAktivitetsgrad> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE behandling_id = ?
                        ORDER BY fom ASC NULLS FIRST
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)

                stmt.executeQuery().toList { toAktivitetsgrad() }
            }
        }

    fun kopierAktivitetsgradTilBehandling(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, behandling_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd) 
                        SELECT gen_random_uuid(), sak_id, ?, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, behandlingId)
            stmt.setObject(2, aktivitetId)

            stmt.executeUpdate()
        }
    }

    fun kopierAktivitetsgradTilOppgave(
        aktivitetId: UUID,
        oppgaveId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                    INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd) 
                    SELECT gen_random_uuid(), sak_id, ?, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse, skjoennsmessig_vurdering, vurdert_fra_12_mnd
                    FROM aktivitetsplikt_aktivitetsgrad
                    WHERE id = ?
                    """.trimIndent(),
                )
            stmt.setObject(1, oppgaveId)
            stmt.setObject(2, aktivitetId)
            stmt.executeUpdate()
        }
    }

    fun slettAktivitetsgradForBehandling(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        DELETE FROM aktivitetsplikt_aktivitetsgrad
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, aktivitetId)
            stmt.setObject(2, behandlingId)

            stmt.executeUpdate()
        }
    }

    fun slettAktivitetsgradForOppgave(
        aktivitetId: UUID,
        oppgaveId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                    DELETE FROM aktivitetsplikt_aktivitetsgrad
                    WHERE id = ? AND oppgave_id = ?
                    """.trimIndent(),
                )
            stmt.setObject(1, aktivitetId)
            stmt.setObject(2, oppgaveId)
            val slettet = stmt.executeUpdate()
            logger.info("Slettet $slettet aktivitetsgrader for oppgave $oppgaveId")
        }
    }

    private fun ResultSet.toAktivitetsgrad() =
        AktivitetspliktAktivitetsgrad(
            id = getUUID("id"),
            sakId = SakId(getLong("sak_id")),
            behandlingId = getString("behandling_id")?.let { UUID.fromString(it) },
            oppgaveId = getString("oppgave_id")?.let { UUID.fromString(it) },
            aktivitetsgrad = AktivitetspliktAktivitetsgradType.valueOf(getString("aktivitetsgrad")),
            skjoennsmessigVurdering =
                getString("skjoennsmessig_vurdering")?.let {
                    AktivitetspliktSkjoennsmessigVurdering.valueOf(
                        it,
                    )
                },
            vurdertFra12Mnd = getBoolean("vurdert_fra_12_mnd"),
            fom = getDate("fom").toLocalDate(),
            tom = getDate("tom")?.toLocalDate(),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
            beskrivelse = getString("beskrivelse"),
        )
}

data class AktivitetspliktAktivitetsgrad(
    val id: UUID,
    val sakId: SakId,
    val behandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val aktivitetsgrad: AktivitetspliktAktivitetsgradType,
    val skjoennsmessigVurdering: AktivitetspliktSkjoennsmessigVurdering? = null,
    val fom: LocalDate,
    val tom: LocalDate?,
    override val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
    val vurdertFra12Mnd: Boolean = false,
) : AktivitetspliktVurderingOpprettetDato {
    fun toDto(): AktivitetspliktAktivitetsgradDto =
        AktivitetspliktAktivitetsgradDto(
            vurdering =
                when (this.aktivitetsgrad) {
                    AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50 -> VurdertAktivitetsgrad.AKTIVITET_UNDER_50
                    AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50 -> VurdertAktivitetsgrad.AKTIVITET_OVER_50
                    AktivitetspliktAktivitetsgradType.AKTIVITET_100 -> VurdertAktivitetsgrad.AKTIVITET_100
                },
            fom = this.fom,
            tom = this.tom,
        )
}

enum class AktivitetspliktSkjoennsmessigVurdering {
    JA,
    MED_OPPFOELGING,
    NEI,
}

data class LagreAktivitetspliktAktivitetsgrad(
    val id: UUID? = null,
    val aktivitetsgrad: AktivitetspliktAktivitetsgradType,
    val skjoennsmessigVurdering: AktivitetspliktSkjoennsmessigVurdering? = null,
    val vurdertFra12Mnd: Boolean = false,
    val fom: LocalDate = LocalDate.now(),
    val tom: LocalDate? = null,
    val beskrivelse: String,
) {
    fun erGyldigUtfylt(): Boolean =
        if (vurdertFra12Mnd) {
            when (aktivitetsgrad) {
                AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50, AktivitetspliktAktivitetsgradType.AKTIVITET_100 -> true
                AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50 -> skjoennsmessigVurdering != null
            }
        } else {
            true
        }
}

enum class AktivitetspliktAktivitetsgradType {
    AKTIVITET_UNDER_50,
    AKTIVITET_OVER_50,
    AKTIVITET_100,
}
