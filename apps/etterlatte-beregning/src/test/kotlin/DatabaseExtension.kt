package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE beregningsgrunnlag;
    TRUNCATE overstyr_beregningsgrunnlag;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
