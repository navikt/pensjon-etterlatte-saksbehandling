import { RelatertPersonsRolle } from '../../../types'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { ChildIcon } from '../../../../../shared/icons/childIcon'
import { PersonDetailWrapper, TypeStatusWrap } from '../../styled'
import { IPersoninfoSoeker } from '../../../../../store/reducers/BehandlingReducer'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import { hentAdresserEtterDoedsdato } from '../../../felles/utils'

type Props = {
  person: IPersoninfoSoeker
  alderVedDoedsdato: string
  doedsdato: string
}

export const Barn: React.FC<Props> = ({ person, alderVedDoedsdato, doedsdato }) => {
  const adresserEtterDoedsdato = hentAdresserEtterDoedsdato(person.bostedadresser, doedsdato)

  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <ChildIcon />
        </span>
        {person.navn} <span className="personRolle">({RelatertPersonsRolle.BARN})</span>
        <TypeStatusWrap type="barn">{alderVedDoedsdato} år på dødsdatoen</TypeStatusWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.fnr} />
        <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} />
        {person.soeknadAdresse && person.soeknadAdresse.adresseIUtlandet === 'JA' && (
          <PersonDetailWrapper adresse={true}>
            <div>
              <strong>Utlandsadresse fra søknad</strong>
            </div>
            <div>{person.soeknadAdresse.adresse}</div>
            <div>{person.soeknadAdresse.land}</div>
          </PersonDetailWrapper>
        )}
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
