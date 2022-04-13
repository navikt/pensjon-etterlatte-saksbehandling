import { useEffect, useState } from 'react'
import { AlertVarsel } from '../../AlertVarsel'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { IconWrapper, PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PersonInfo } from './personinfo/PersonInfo'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'
import { ForelderWrap, TypeStatusWrap } from '../../styled'
import { IAdresse, IGjenlevendeFraSak, PersonStatus, RelatertPersonsRolle } from '../../../types'
import { getStatsborgerskapTekst, sjekkAdresseGjenlevendeISoeknadMotPdl } from '../../utils'

type Props = {
  person: IGjenlevendeFraSak
  innsenderErGjenlevende: boolean
}

export const GjenlevendeForelder: React.FC<Props> = ({ person, innsenderErGjenlevende }) => {
  const gjeldendeAdresse: IAdresse | undefined =
    person.adresser && person.adresser.find((adresse: IAdresse) => adresse.aktiv === true)

  const [gjenlevendeAdresselLikSoeknadOgPdl, setGjenlevendeAdresselLikSoeknadOgPdl] = useState<boolean>()

  useEffect(() => {
    if (person.adresseFraSoeknad && gjeldendeAdresse) {
      setGjenlevendeAdresselLikSoeknadOgPdl(
        sjekkAdresseGjenlevendeISoeknadMotPdl(
          person.adresseFraSoeknad,
          `${gjeldendeAdresse.adresseLinje1}, ${gjeldendeAdresse.postnr} ${gjeldendeAdresse.poststed}`
        )
      )
    }
  }, [])

  return (
    <PersonBorder>
      {gjenlevendeAdresselLikSoeknadOgPdl && (
        <IconWrapper>
          <WarningIcon />
        </IconWrapper>
      )}
      <PersonHeader>
        <span className="icon">
          <PeopleIcon />
        </span>
        {person.navn}
        <span className="personRolle">
          ({PersonStatus.GJENLEVENDE_FORELDER} {RelatertPersonsRolle.FORELDER})
        </span>
        {innsenderErGjenlevende && <ForelderWrap>Innsender av søknad</ForelderWrap>}

        <TypeStatusWrap type="statsborgerskap">{getStatsborgerskapTekst(person.statsborgerskap)}</TypeStatusWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfo
          fnr={person.fnr}
          fnrFraSoeknad={person.fnrFraSoeknad}
          bostedEtterDoedsdato={person.adresser}
          adresseFraSoeknad={person.adresseFraSoeknad}
          avdoedPerson={false}
        />
        {gjenlevendeAdresselLikSoeknadOgPdl && <AlertVarsel varselType="ikke lik adresse" />}
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
