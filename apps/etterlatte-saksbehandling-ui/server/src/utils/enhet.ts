import { IEnhet } from '../routers/innloggetBrukerRouter'

export const lagEnhetFraString = (enhet: string): IEnhet => {
  const enhetListe = enhet.split(' ')
  const enhetId = String(enhetListe.shift())
  const navn = enhetListe.join(' ')

  return { enhetId, navn }
}
