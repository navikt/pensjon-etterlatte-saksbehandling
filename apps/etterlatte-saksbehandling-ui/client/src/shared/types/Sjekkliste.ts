export interface ISjekkliste {
  id: string
  kommentar: string | null
  adresseForBrev: string | null
  kontonrRegistrert: string | null
  versjon: string
  sjekklisteItems: ISjekklisteItem[]
}

export interface ISjekklisteItem {
  id: string
  beskrivelse: string
  avkrysset: boolean
  versjon: string
}
