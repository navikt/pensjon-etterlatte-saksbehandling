package no.nav.etterlatte.common

import no.nav.etterlatte.ktortokenexchange.ThreadBoundSecCtx
import no.nav.etterlatte.libs.common.person.Foedselsnummer

fun innloggetBrukerFnr() = Foedselsnummer.of(ThreadBoundSecCtx.get().user())
