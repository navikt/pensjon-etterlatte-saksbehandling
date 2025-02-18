import { Personopplysning } from '~shared/types/grunnlag'
import { IAdresse } from '~shared/types/IAdresse'
import { formaterAdresse, IPdlPerson } from '~shared/types/Person'
import { BodyShort } from '@navikt/ds-react'

export const PersonopplysningAktivEllerSisteAdresse = ({ person }: { person: Personopplysning }) => {
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

export const PdlPersonAktivEllerSisteAdresse = ({ person }: { person: IPdlPerson }) => {
  const finnAktiveEllerSisteAdresse = (): IAdresse | undefined => {
    const aktivAdresse = person?.bostedsadresse?.find((adresse) => adresse.aktiv)

    if (!!aktivAdresse) {
      return aktivAdresse
    } else {
      const bostedadresser = !!person?.bostedsadresse?.length ? [...person?.bostedsadresse] : []
      return bostedadresser.sort((a, b) => (new Date(b.gyldigFraOgMed!) > new Date(a.gyldigFraOgMed!) ? 1 : -1))[0]
    }
  }

  const addresse = finnAktiveEllerSisteAdresse()

  return addresse ? <BodyShort>{formaterAdresse(addresse)}</BodyShort> : <BodyShort>Ingen adresse</BodyShort>
}
