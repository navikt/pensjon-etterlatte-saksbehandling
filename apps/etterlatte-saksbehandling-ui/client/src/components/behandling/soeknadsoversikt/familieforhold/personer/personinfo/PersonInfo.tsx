import { PersonDetailWrapper } from '../../../styled'
import { IAdresse } from '../../../../types'
import { sjekkDataFraSoeknadMotPdl } from '../../../utils'
import { PersonInfoAdresse } from './PersonInfoAdresse'

type Props = {
  fnr: string
  fnrFraSoeknad: string
  bostedEtterDoedsdato: IAdresse[]
  avdoedPerson: boolean
  adresseFraSoeknad?: string
}

export const PersonInfo: React.FC<Props> = ({
  fnr,
  fnrFraSoeknad,
  bostedEtterDoedsdato,
  avdoedPerson,
  adresseFraSoeknad,
}) => {
  const gjeldendeAdresse: IAdresse | undefined =
    bostedEtterDoedsdato && bostedEtterDoedsdato.find((adresse: IAdresse) => adresse.aktiv === true)

  return (
    <>
      <PersonDetailWrapper adresse={false}>
        <div>
          <strong>Fødselsnummer</strong>
        </div>
        {sjekkDataFraSoeknadMotPdl(fnr, fnrFraSoeknad)}
      </PersonDetailWrapper>
      <PersonInfoAdresse
        adresser={bostedEtterDoedsdato}
        adresseFraSoeknadGjenlevende={adresseFraSoeknad}
        gjeldendeAdresse={gjeldendeAdresse}
        avodedPerson={avdoedPerson}
      />
    </>
  )
}
