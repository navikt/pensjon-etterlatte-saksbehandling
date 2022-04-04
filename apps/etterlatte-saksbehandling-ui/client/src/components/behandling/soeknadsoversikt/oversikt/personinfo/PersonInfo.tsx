import { PersonInfoWrapper, PersonDetailWrapper, PersonInfoBorder, IconWrapper } from '../../styled'
import { IAdresse, IPersonFraSak, PersonStatus } from '../../../types'
import { sjekkAdresseGjenlevendeISoeknadMotPdl, sjekkDataFraSoeknadMotPdl } from '../utils'
import { PersonInfoAdresse } from './PersonInfoAdresse'
import { PersonInfoHeader } from './PersonInfoHeader'
import { hentAdresserEtterDoedsdato } from '../../../felles/utils'
import { useEffect, useState } from 'react'
import { useContext } from 'react'
import { AppContext } from '../../../../../store/AppContext'
import {
  IKriterie,
  Kriterietype,
  VilkaarsType,
  GyldighetVurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { AlertVarsel } from '../AlertVarsel'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const ctx = useContext(AppContext)
  const gjeldendeAdresse: IAdresse | undefined =
    person.adresser && person.adresser.find((adresse: IAdresse) => adresse.aktiv === true)
  const bostedEtterDoedsdato = hentAdresserEtterDoedsdato(person.adresser, new Date(person.datoForDoedsfall))

  const [feilForelderOppgittSomAvdoed, setFeilForelderOppgittSomAvdoed] = useState<boolean>()
  const [forelderErDoed, setForelderErDoed] = useState<boolean>()
  const [gjenlevendeAdresselLikSoeknadOgPdl, setGjenlevendeAdresselLikSoeknadOgPdl] = useState<boolean>()

  useEffect(() => {
    if (person.personStatus === PersonStatus.AVDOED) {
      const vilkaar = ctx.state.behandlingReducer.vilkårsprøving.vilkaar
      const doedsfallVilkaar: any = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)

      const avdoedErForelderVilkaar =
        doedsfallVilkaar &&
        doedsfallVilkaar.kriterier.find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER).resultat ===
          GyldighetVurderingsResultat.OPPFYLT

      const avdoedErLikISoeknad = person?.fnrFraSoeknad === person?.fnr

      setFeilForelderOppgittSomAvdoed(avdoedErForelderVilkaar && !avdoedErLikISoeknad)
      setForelderErDoed(avdoedErForelderVilkaar)
    }

    if (person.personStatus === PersonStatus.GJENLEVENDE_FORELDER && person.adresseFraSoeknad && gjeldendeAdresse) {
      setGjenlevendeAdresselLikSoeknadOgPdl(
        sjekkAdresseGjenlevendeISoeknadMotPdl(
          person.adresseFraSoeknad,
          `${gjeldendeAdresse.adresseLinje1}, ${gjeldendeAdresse.postnr} ${gjeldendeAdresse.poststed}`
        )
      )
    }
  }, [])

  return (
    <PersonInfoBorder>
      {person.personStatus === PersonStatus.AVDOED && (feilForelderOppgittSomAvdoed || !forelderErDoed) && (
        <IconWrapper>
          <WarningIcon />
        </IconWrapper>
      )}
      {person.personStatus === PersonStatus.GJENLEVENDE_FORELDER && gjenlevendeAdresselLikSoeknadOgPdl && (
        <IconWrapper>
          <WarningIcon />
        </IconWrapper>
      )}
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
        {person.personStatus === PersonStatus.AVDOED && (
          <div>
            {feilForelderOppgittSomAvdoed && <AlertVarsel varselType="ikke riktig oppgitt avdød i søknad" />}
            {!forelderErDoed && <AlertVarsel varselType="forelder ikke død" />}
          </div>
        )}
        {person.personStatus === PersonStatus.GJENLEVENDE_FORELDER && (
          <div> {gjenlevendeAdresselLikSoeknadOgPdl && <AlertVarsel varselType="ikke lik adresse" />}</div>
        )}
      </PersonInfoWrapper>
    </PersonInfoBorder>
  )
}
