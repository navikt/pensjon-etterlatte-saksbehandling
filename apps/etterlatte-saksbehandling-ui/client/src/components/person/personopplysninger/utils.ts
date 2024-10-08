import { ILand } from '~utils/kodeverk'

export const finnLandSomTekst = (isoLandKode: string, landListe: ILand[]): string | undefined => {
  return landListe.find((val) => val.isoLandkode === isoLandKode)?.beskrivelse.tekst
}
