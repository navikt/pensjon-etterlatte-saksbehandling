import { IEnhet } from "../routers/modia";

export const lagEnhet = (enhet: string):IEnhet => {
    const enhetListe = enhet.split(" ")
    const enhetId = String(enhetListe.shift())
    const navn = enhetListe.join(' ')

    return {enhetId, navn}
}