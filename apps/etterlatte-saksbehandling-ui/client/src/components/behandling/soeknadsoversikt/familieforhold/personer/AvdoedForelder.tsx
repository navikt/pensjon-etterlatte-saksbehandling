import { useEffect, useState } from 'react'
import { useContext } from 'react'
import { AppContext } from '../../../../../store/AppContext'
import {
  IKriterie,
  Kriterietype,
  VilkaarsType,
  VurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { IAvdoedFraSak, PersonStatus, RelatertPersonsRolle } from '../../../types'
import { AlertVarsel } from '../../AlertVarsel'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { PersonInfo } from './personinfo/PersonInfo'
import { IconWrapper, PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'
import { ForelderWrap, TypeStatusWrap } from '../../styled'
import { format } from 'date-fns'
import { getStatsborgerskapTekst } from '../../utils'

type Props = {
  person: IAvdoedFraSak
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

    const avdoedErLikISoeknad = person?.fnrFraSoeknad === person?.fnr
    setFeilForelderOppgittSomAvdoed(avdoedErForelderVilkaar && !avdoedErLikISoeknad)
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
        <ForelderWrap avdoed={true}>Død {format(new Date(person.datoForDoedsfall), 'dd.MM.yyyy')}</ForelderWrap>
        <TypeStatusWrap type="statsborgerskap">{getStatsborgerskapTekst(person.statsborgerskap)}</TypeStatusWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfo
          fnr={person.fnr}
          fnrFraSoeknad={person.fnrFraSoeknad}
          bostedEtterDoedsdato={person.adresser}
          avdoedPerson={true}
        />
        <div>
          {feilForelderOppgittSomAvdoed && <AlertVarsel varselType="ikke riktig oppgitt avdød i søknad" />}
          {forelderErDoed === VurderingsResultat.IKKE_OPPFYLT && <AlertVarsel varselType="forelder ikke død" />}
          {forelderErDoed === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
            <AlertVarsel varselType="mangler" />
          )}
        </div>
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
