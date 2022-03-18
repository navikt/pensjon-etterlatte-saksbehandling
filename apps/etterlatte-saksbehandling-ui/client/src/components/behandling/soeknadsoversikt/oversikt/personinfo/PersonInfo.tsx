import { PersonInfoWrapper, PersonDetailWrapper, PersonInfoBorder } from '../../styled'
import { IAdresse, IPersonFraSak, PersonStatus } from '../../../types'
import { sjekkDataFraSoeknadMotPdl } from '../utils'
import { PersonInfoAdresse } from './PersonInfoAdresse'
import { PersonInfoHeader } from './PersonInfoHeader'
import { hentAdresserEtterDoedsdato } from '../../../felles/utils'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const gjeldendeAdresse: IAdresse | undefined =
    person.adresser && person.adresser.find((adresse: IAdresse) => adresse.aktiv === true)
  const bostedEtterDoedsdato = hentAdresserEtterDoedsdato(person.adresser, new Date(person.datoForDoedsfall))

  return (
    <PersonInfoBorder>
      <PersonInfoHeader person={person} />
      <PersonInfoWrapper>
        <PersonDetailWrapper adresse={false}>
          <div>
            <strong>Fødselsnummer</strong>
          </div>
          {sjekkDataFraSoeknadMotPdl(person?.fnr, person?.fnrFraSoeknad)}
        </PersonDetailWrapper>
        <PersonInfoAdresse
          adresser={bostedEtterDoedsdato}
          adresseFraSoeknadGjenlevende={person.adresseFraSoeknad}
          gjeldendeAdresse={gjeldendeAdresse}
          avodedPerson={person.personStatus === PersonStatus.AVDOED}
        />
      </PersonInfoWrapper>
    </PersonInfoBorder>
  )
}
