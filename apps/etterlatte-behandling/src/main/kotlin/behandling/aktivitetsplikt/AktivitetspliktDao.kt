package no.nav.etterlatte.behandling.aktivitetsplikt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetType
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.database.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.SQLDate
import no.nav.etterlatte.libs.database.SQLLong
import no.nav.etterlatte.libs.database.SQLObject
import no.nav.etterlatte.libs.database.SQLString
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.opprett
import no.nav.etterlatte.libs.database.slett
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun finnSenesteAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? =
        connectionAutoclosing
            .hent(
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
                        """,
                listOf(SQLObject(behandlingId)),
            ) {
                AktivitetspliktOppfolging(
                    behandlingId = getUUID("behandling_id"),
                    aktivitet = getString("aktivitet"),
                    opprettet = getTidspunkt("opprettet"),
                    opprettetAv = getString("opprettet_av"),
                )
            }.firstOrNull()

    fun lagre(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ) = connectionAutoclosing.opprett(
        """
            |INSERT INTO aktivitetsplikt_oppfolging(behandling_id, aktivitet, opprettet_av) 
            |VALUES (?, ?, ?)
                    """,
        listOf(
            SQLObject(behandlingId),
            SQLString(nyOppfolging.aktivitet),
            SQLString(navIdent),
        ),
    )

    fun hentAktiviteterForBehandling(behandlingId: UUID): List<AktivitetspliktAktivitet> =
        connectionAutoclosing
            .hent(
                """
                        SELECT id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitet
                        WHERE behandling_id = ?
                        """,
                listOf(SQLObject(behandlingId)),
            ) { toAktivitet() }

    fun hentAktiviteterForSak(sakId: SakId): List<AktivitetspliktAktivitet> =
        connectionAutoclosing
            .hent(
                """
                        SELECT id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitet
                        WHERE sak_id = ?
                        """,
                listOf(SQLObject(sakId)),
            ) { toAktivitet() }

    fun opprettAktivitet(
        behandlingId: UUID,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.opprett(
        """
                        INSERT INTO aktivitetsplikt_aktivitet(id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
        listOf(
            SQLObject(UUID.randomUUID()),
            SQLLong(aktivitet.sakId),
            SQLObject(behandlingId),
            SQLString(aktivitet.type.name),
            SQLDate(Date.valueOf(aktivitet.fom)),
            SQLDate(aktivitet.tom?.let { tom -> Date.valueOf(tom) }),
            SQLString(kilde.toJson()),
            SQLString(kilde.toJson()),
            SQLString(aktivitet.beskrivelse),
        ),
    )

    fun opprettAktivitetForSak(
        sakId: SakId,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.opprett(
        """
                        INSERT INTO aktivitetsplikt_aktivitet(id, sak_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
        listOf(
            SQLObject(UUID.randomUUID()),
            SQLLong(sakId),
            SQLString(aktivitet.type.name),
            SQLDate(Date.valueOf(aktivitet.fom)),
            SQLDate(aktivitet.tom?.let { tom -> Date.valueOf(tom) }),
            SQLString(kilde.toJson()),
            SQLString(kilde.toJson()),
            SQLString(aktivitet.beskrivelse),
        ),
    )

    fun oppdaterAktivitet(
        behandlingId: UUID,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.oppdater(
        """
                        UPDATE aktivitetsplikt_aktivitet
                        SET aktivitet_type = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ?
                        WHERE id = ? AND behandling_id = ?
                    """,
        listOf(
            SQLString(aktivitet.type.name),
            SQLDate(Date.valueOf(aktivitet.fom)),
            SQLDate(aktivitet.tom?.let { tom -> Date.valueOf(tom) }),
            SQLString(kilde.toJson()),
            SQLString(aktivitet.beskrivelse),
            SQLObject(requireNotNull(aktivitet.id)),
            SQLObject(behandlingId),
        ),
    )

    fun oppdaterAktivitetForSak(
        sakId: SakId,
        aktivitet: LagreAktivitetspliktAktivitet,
        kilde: Grunnlagsopplysning.Kilde,
    ) = connectionAutoclosing.oppdater(
        statement = """UPDATE aktivitetsplikt_aktivitet
                        SET aktivitet_type = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ?
                        WHERE id = ? AND sak_id = ?""",
        params =
            listOf(
                SQLString(aktivitet.type.name),
                SQLDate(Date.valueOf(aktivitet.fom)),
                SQLDate(aktivitet.tom?.let { tom -> Date.valueOf(tom) }),
                SQLString(kilde.toJson()),
                SQLString(aktivitet.beskrivelse),
                SQLObject(requireNotNull(aktivitet.id)),
                SQLObject(sakId),
            ),
    )

    fun slettAktivitet(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.slett(
        """DELETE FROM aktivitetsplikt_aktivitet WHERE id = ? AND behandling_id = ?""",
        listOf(SQLObject(aktivitetId), SQLObject(behandlingId)),
    )

    fun slettAktivitetForSak(
        aktivitetId: UUID,
        sakId: SakId,
    ) = connectionAutoclosing.slett(
        "DELETE FROM aktivitetsplikt_aktivitet WHERE id = ? AND sak_id = ?",
        listOf(SQLObject(aktivitetId), SQLObject(sakId)),
    )

    fun kopierAktiviteter(
        forrigeBehandlingId: UUID,
        nyBehandlingId: UUID,
    ) = connectionAutoclosing.opprett(
        """
                        INSERT INTO aktivitetsplikt_aktivitet (id, sak_id, behandling_id, aktivitet_type, fom, tom, opprettet, endret, beskrivelse)
                        (SELECT gen_random_uuid(), sak_id, ?, aktivitet_type, fom, tom, opprettet, endret, beskrivelse FROM aktivitetsplikt_aktivitet WHERE behandling_id = ?)
                    """,
        listOf(SQLObject(nyBehandlingId), SQLObject(forrigeBehandlingId)),
    )

    private fun ResultSet.toAktivitet() =
        AktivitetspliktAktivitet(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
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
}
