package efterlatte.prosessering

/**
 * Typed nøkkel som knytter en task-type til payload-typen [P] og bærer sin egen
 * (de)serialisering. Ved å legge serialiseringen her — som funksjoner — slipper
 * core å binde seg til Jackson eller kotlinx.serialization; hver task-type velger
 * selv, og core forblir framework-agnostisk.
 *
 * `navn` er den stabile strengen som lagres i `task.type`-kolonnen og brukes til å
 * finne riktig [TaskStep] i motorens register.
 */
class TaskType<P : Any>(
    val navn: String,
    val serialiser: (P) -> String,
    val deserialiser: (String) -> P,
)

/** Praktisk task-type for steg som allerede har payload som en ferdig streng (f.eks. rå JSON). */
fun strengType(navn: String): TaskType<String> =
    TaskType(
        navn = navn,
        serialiser = { it },
        deserialiser = { it },
    )
