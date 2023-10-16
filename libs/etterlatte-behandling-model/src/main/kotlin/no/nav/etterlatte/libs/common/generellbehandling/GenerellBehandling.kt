package no.nav.etterlatte.libs.common.generellbehandling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class GenerellBehandling(
    val id: UUID,
    val sakId: Long,
    val opprettet: Tidspunkt,
    val type: GenerellBehandlingType,
    val innhold: Innhold?,
    val tilknyttetBehandling: UUID? = null,
    val status: Status,
) {
    enum class Status {
        OPPRETTET,
        FATTET,
        ATTESTERT,
        AVBRUTT,
    }

    init {
        if (innhold !== null) {
            when (type) {
                GenerellBehandlingType.ANNEN ->
                    require(innhold is Innhold.Annen) {
                        "Type $type matcher " +
                            "ikke innhold navn: ${innhold.javaClass.simpleName}"
                    }
                GenerellBehandlingType.UTLAND ->
                    require(innhold is Innhold.Utland) {
                        "Type $type matcher ikke innhold navn: ${innhold.javaClass.simpleName}"
                    }
            }
        }
    }

    companion object {
        fun opprettFraType(
            type: GenerellBehandlingType,
            sakId: Long,
        ) = GenerellBehandling(UUID.randomUUID(), sakId, Tidspunkt.now(), type, null, null, Status.OPPRETTET)

        fun opprettUtland(
            sakId: Long,
            behandlingreferanse: UUID?,
        ) = GenerellBehandling(
            UUID.randomUUID(),
            sakId,
            Tidspunkt.now(),
            GenerellBehandlingType.UTLAND,
            null,
            tilknyttetBehandling = behandlingreferanse,
            status = Status.OPPRETTET,
        )
    }

    enum class GenerellBehandlingType {
        ANNEN,
        UTLAND,
    }
}
