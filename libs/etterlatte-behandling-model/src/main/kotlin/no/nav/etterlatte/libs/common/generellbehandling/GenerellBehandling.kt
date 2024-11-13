package no.nav.etterlatte.libs.common.generellbehandling

import no.nav.etterlatte.libs.common.feilhaandtering.checkInternFeil
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class Behandler(
    val saksbehandler: String,
    val tidspunkt: Tidspunkt,
)

data class Attestant(
    val attestant: String,
    val tidspunkt: Tidspunkt,
)

data class GenerellBehandling(
    val id: UUID,
    val sakId: SakId,
    val opprettet: Tidspunkt,
    val type: GenerellBehandlingType,
    val innhold: Innhold?,
    val tilknyttetBehandling: UUID? = null,
    val status: Status,
    val behandler: Behandler? = null,
    val attestant: Attestant? = null,
    val returnertKommenar: String? = null,
) {
    enum class Status {
        OPPRETTET,
        FATTET,
        RETURNERT,
        ATTESTERT,
        AVBRUTT,
    }

    fun kanEndres() =
        when (this.status) {
            Status.OPPRETTET, Status.RETURNERT -> true
            Status.AVBRUTT, Status.ATTESTERT, Status.FATTET -> false
        }

    fun erFattet() = this.status === Status.FATTET

    init {
        if (innhold !== null) {
            when (type) {
                GenerellBehandlingType.ANNEN ->
                    checkInternFeil(innhold is Innhold.Annen) {
                        "Type $type matcher " +
                            "ikke innhold navn: ${innhold.javaClass.simpleName}"
                    }
                GenerellBehandlingType.KRAVPAKKE_UTLAND ->
                    checkInternFeil(innhold is Innhold.KravpakkeUtland) {
                        "Type $type matcher ikke innhold navn: ${innhold.javaClass.simpleName}"
                    }
            }
        }
    }

    companion object {
        fun opprettFraType(
            type: GenerellBehandlingType,
            sakId: SakId,
        ) = GenerellBehandling(UUID.randomUUID(), sakId, Tidspunkt.now(), type, null, null, Status.OPPRETTET)

        fun opprettUtland(
            sakId: SakId,
            behandlingreferanse: UUID?,
        ) = GenerellBehandling(
            UUID.randomUUID(),
            sakId,
            Tidspunkt.now(),
            GenerellBehandlingType.KRAVPAKKE_UTLAND,
            null,
            tilknyttetBehandling = behandlingreferanse,
            status = Status.OPPRETTET,
        )
    }

    enum class GenerellBehandlingType {
        ANNEN,
        KRAVPAKKE_UTLAND,
    }
}

enum class GenerellBehandlingHendelseType {
    OPPRETTET,
    FATTET,
    ATTESTERT,
    UNDERKJENT,
}
