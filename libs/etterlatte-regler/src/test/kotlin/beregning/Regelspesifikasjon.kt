package beregning

import regler.RegelReferanse

data class GenerellRegel(override val id: String, override val beskrivelse: String) : RegelReferanse
data class BarnepensjonGammeltRegelverk(override val id: String, override val beskrivelse: String) : RegelReferanse
data class BarnepensjonNyttRegelverk(override val id: String, override val beskrivelse: String) : RegelReferanse
data class ToDoRegelReferanse(
    override val id: String = "ToDo",
    override val beskrivelse: String = "ToDo: Legg til referanse"
) : RegelReferanse