import { useEffect, useState } from 'react'
import { useContext } from 'react'
import { AppContext } from '../../../../../store/AppContext'
import {
  IKriterie,
  IPersoninfoAvdoed,
  Kriterietype,
  VilkaarsType,
  VurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { PersonStatus, RelatertPersonsRolle } from '../../../types'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { IconWrapper, PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'
import { ForelderWrap } from '../../styled'
import { format } from 'date-fns'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'

type Props = {
  person: IPersoninfoAvdoed
}

export const AvdoedForelder: React.FC<Props> = ({ person }) => {
  const ctx = useContext(AppContext)

  const [feilForelderOppgittSomAvdoed, setFeilForelderOppgittSomAvdoed] = useState<boolean>()
  const [forelderErDoed, setForelderErDoed] = useState<any>()

  useEffect(() => {
    const vilkaar = ctx.state.behandlingReducer.vilkårsprøving.vilkaar
    const doedsfallVilkaar: any = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)

    const avdoedErForelderVilkaar =
      doedsfallVilkaar &&
      doedsfallVilkaar.kriterier.find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER).resultat

    setFeilForelderOppgittSomAvdoed(avdoedErForelderVilkaar)
    setForelderErDoed(avdoedErForelderVilkaar)
  }, [])

  return (
    <PersonBorder>
      {(feilForelderOppgittSomAvdoed || forelderErDoed !== VurderingsResultat.OPPFYLT) && (
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
          ({PersonStatus.AVDOED} {RelatertPersonsRolle.FORELDER})
        </span>
        <ForelderWrap avdoed={true}>Død {format(new Date(person.doedsdato), 'dd.MM.yyyy')}</ForelderWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.fnr} />
        <PersonInfoAdresse adresser={person.adresser} visHistorikk={false} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
