import { IPdlPerson } from '~shared/types/Person'
import { IAdresse } from '~shared/types/IAdresse'
import { BodyShort } from '@navikt/ds-react'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

export const BarnAddressePeriode = ({ barn }: { barn: IPdlPerson }) => {
  const aktivAdresse: IAdresse | undefined = barn.bostedsadresse?.find((adresse: IAdresse) => adresse.aktiv)

  return aktivAdresse ? (
    <BodyShort>
      {formaterDatoMedFallback(aktivAdresse.gyldigFraOgMed, '-')} til{' '}
      {formaterDatoMedFallback(aktivAdresse.gyldigTilOgMed, '-')}
    </BodyShort>
  ) : (
    <BodyShort>Mangler adresse</BodyShort>
  )
}
