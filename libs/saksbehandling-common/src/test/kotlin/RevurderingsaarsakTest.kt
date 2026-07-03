import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import org.junit.jupiter.api.Test

class RevurderingsaarsakTest {
    @Test
    fun name() {
        Revurderingaarsak.entries
            .sorted()
            .forEach { println(it.name) }
    }
}
