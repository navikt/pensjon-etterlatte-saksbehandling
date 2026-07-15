package efterlatte.prosessering

/**
 * Ett stykke arbeid motoren kan kjøre for en gitt [TaskType]. Steget er
 * **blokkerende**, ikke `suspend`: det kjøres inne i én transaksjon på én tråd
 * (se [TaskKontekst.transaksjon]), og en JDBC-transaksjon er trådbundet. Motoren
 * gir samtidighet på tvers av tasker via coroutines; det enkelte steget holder seg
 * til én tråd.
 */
interface TaskStep<P : Any> {
    val type: TaskType<P>

    fun utfor(kontekst: TaskKontekst<P>)
}

/** Bygger et [TaskStep] fra en lambda — praktisk for enkle steg og tester. */
fun <P : Any> taskSteg(
    type: TaskType<P>,
    utfor: (TaskKontekst<P>) -> Unit,
): TaskStep<P> =
    object : TaskStep<P> {
        override val type = type

        override fun utfor(kontekst: TaskKontekst<P>) = utfor(kontekst)
    }
