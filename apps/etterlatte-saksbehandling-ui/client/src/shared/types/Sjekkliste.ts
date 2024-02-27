export interface ISjekkliste {
  id: string
  kommentar: string | undefined
  kontonrRegistrert: string | undefined
  onsketSkattetrekk: number | undefined
  bekreftet: boolean
  versjon: string
  sjekklisteItems: ISjekklisteItem[]
}

export interface ISjekklisteItem {
  id: string
  beskrivelse: string
  avkrysset: boolean
  versjon: string
}
