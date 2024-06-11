package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException

class TilbakekrevingHarMangelException(
    message: String,
) : InternfeilException(detail = message)

class TilbakekrevingUnderBehandlingFinnesAlleredeException(
    message: String,
) : InternfeilException(detail = message)

class TilbakekrevingFinnesIkkeException(
    message: String,
) : IkkeFunnetException(code = "NOT_FOUND", detail = message)

class TilbakekrevingFeilTilstandException(
    message: String,
) : InternfeilException(detail = message)

class TilbakekrevingManglerBrevException(
    message: String,
) : InternfeilException(detail = message)
