package rapidsandrivers.migrering

data class MigreringRequest(val pesysId: PesysId)

data class PesysId(val id: String)