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
) {
    init {
        if (innhold !== null) {
            when (type) {
                GenerellBehandlingType.ANNEN ->
                    assert(innhold is Innhold.Annen) {
                        throw IllegalArgumentException(
                            "Type $type matcher " +
                                "ikke innhold navn: ${innhold.javaClass.simpleName}",
                        )
                    }
                GenerellBehandlingType.UTLAND ->
                    assert(innhold is Innhold.Utland) {
                        throw IllegalArgumentException(
                            "Type $type matcher ikke innhold navn: ${innhold.javaClass.simpleName}",
                        )
                    }
            }
        }
    }

    companion object {
        fun opprettFraType(
            type: GenerellBehandlingType,
            sakId: Long,
        ) = GenerellBehandling(UUID.randomUUID(), sakId, Tidspunkt.now(), type, null)

        fun opprettUtland(
            type: GenerellBehandlingType,
            sakId: Long,
            behandlingreferanse: UUID,
        ) = GenerellBehandling(UUID.randomUUID(), sakId, Tidspunkt.now(), type, null, tilknyttetBehandling = behandlingreferanse)
    }

    enum class GenerellBehandlingType {
        ANNEN,
        UTLAND,
    }
}
