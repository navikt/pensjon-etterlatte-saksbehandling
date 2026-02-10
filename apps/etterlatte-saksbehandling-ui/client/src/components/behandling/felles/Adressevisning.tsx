import { IAdresse } from '~shared/types/IAdresse'
import { VStack } from '@navikt/ds-react'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'

export const Adressevisning = ({
  adresser,
  soeknadsoversikt,
}: {
  adresser: IAdresse[]
  soeknadsoversikt?: boolean
}) => {
  return (
    <VStack gap="space-1">
      {adresser?.length > 0 ? (
        adresser
          .sort((a, b) => (new Date(b.gyldigFraOgMed!) > new Date(a.gyldigFraOgMed!) ? 1 : -1))
          .map((adresse, index) => (
            <Adresse adresse={adresse} key={index} soeknadsoversikt={soeknadsoversikt ? soeknadsoversikt : false} />
          ))
      ) : (
        <div>Ingen adresser</div>
      )}
    </VStack>
  )
}

export const Adresse = ({ adresse, soeknadsoversikt }: { adresse: IAdresse; soeknadsoversikt: boolean }) => {
  /**
   * Både fra og til dato er nullable i [AdresseSamsvar]
   **/
  const fra = formaterKanskjeStringDatoMedFallback('-', adresse.gyldigFraOgMed)
  const til = adresse.aktiv ? 'nå' : formaterKanskjeStringDatoMedFallback('-', adresse.gyldigTilOgMed)

  return (
    <>
      {soeknadsoversikt ? (
        <VStack>
          <span className="date">
            {fra} - {til}:
          </span>
          <span>
            {adresse.adresseLinje1} {adresse.poststed}
          </span>
        </VStack>
      ) : (
        <div>
          <div>{adresse.adresseLinje1}</div>
          <div>
            {adresse.postnr} {adresse.poststed}
          </div>
          <div>{adresse.land}</div>
          <div>
            {fra} - {til}
          </div>
        </div>
      )}
    </>
  )
}
