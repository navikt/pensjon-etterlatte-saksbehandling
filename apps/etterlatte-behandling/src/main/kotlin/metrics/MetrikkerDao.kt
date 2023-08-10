package no.nav.etterlatte.metrics

import java.sql.Connection

class MetrikkerDao(private val connection: () -> Connection)