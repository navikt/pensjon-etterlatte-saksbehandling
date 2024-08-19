export interface ILand {
  gyldigFra: string
  gyldigTil: string
  isoLandkode: string
  beskrivelse: {
    term: string
    tekst: string
  }
}

export const sorterLand = (landListe: ILand[]): ILand[] => {
  landListe.sort((a: ILand, b: ILand) => {
    if (a.beskrivelse.tekst > b.beskrivelse.tekst) {
      return 1
    }
    return -1
  })
  return landListe
}
