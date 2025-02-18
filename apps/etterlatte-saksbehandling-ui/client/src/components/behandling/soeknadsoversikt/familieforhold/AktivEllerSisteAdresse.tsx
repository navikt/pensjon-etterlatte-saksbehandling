import { Personopplysning } from '~shared/types/grunnlag'
import { IAdresse } from '~shared/types/IAdresse'
import { formaterAdresse } from '~shared/types/Person'
import { BodyShort } from '@navikt/ds-react'

export const AktivEllerSisteAdresse = ({ person }: { person: Personopplysning }) => {
  const finnAktiveEllerSisteAdresse = (): IAdresse | undefined => {
    const aktivAdresse = person?.opplysning.bostedsadresse?.find((adresse) => adresse.aktiv)

    if (!!aktivAdresse) {
      return aktivAdresse
    } else {
      const bostedadresser = !!person?.opplysning.bostedsadresse?.length ? [...person?.opplysning.bostedsadresse] : []
      return bostedadresser.sort((a, b) => (new Date(b.gyldigFraOgMed!) > new Date(a.gyldigFraOgMed!) ? 1 : -1))[0]
    }
  }

  const addresse = finnAktiveEllerSisteAdresse()

  return addresse ? <BodyShort>{formaterAdresse(addresse)}</BodyShort> : <BodyShort>Ingen adresse</BodyShort>
}
