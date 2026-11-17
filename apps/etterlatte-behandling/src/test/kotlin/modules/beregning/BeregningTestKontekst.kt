package no.nav.etterlatte.modules.beregning

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.nyKontekstMedBruker
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class BeregningTestKontekst :
    BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        nyKontekstMedBruker(Self("beregning-test"))
    }

    override fun afterEach(context: ExtensionContext) {
        Kontekst.remove()
    }
}
