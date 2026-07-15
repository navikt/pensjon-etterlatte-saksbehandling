package no.nav.etterlatte.prosessering

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement("TRUNCATE prosessering.task CASCADE;")
class ProsesseringDatabaseExtension : GenerellDatabaseExtension()
