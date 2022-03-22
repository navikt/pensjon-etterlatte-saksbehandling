import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { OmSoeknad } from './oversikt/OmSoeknad'
import { Familieforhold } from './oversikt/Familieforhold'
import { SoeknadGyldigFremsatt } from './oversikt/SoeknadGyldigFremsatt'
import { usePersonInfoFromBehandling } from './usePersonInfoFromBehandling'
import {
  IKriterie,
  Kriterietype,
  VilkaarsType,
  VilkaarVurderingsResultat,
} from '../../../store/reducers/BehandlingReducer'

export const Soeknadsoversikt = () => {
  const {
    soekerPdl,
    avdoedPersonPdl,
    soekerSoknad,
    avdodPersonSoknad,
    innsender,
    mottattDato,
    gjenlevendePdl,
    gjenlevendeSoknad,
  } = usePersonInfoFromBehandling()

  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving
  const doedsfallVilkaar: any = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)

  return (
    <Content>
      <ContentHeader>
        <h1>Søknadsoversikt</h1>
        <OmSoeknad
          soekerPdl={soekerPdl}
          avdoedPersonPdl={avdoedPersonPdl}
          soekerSoknad={soekerSoknad}
          avdodPersonSoknad={avdodPersonSoknad}
          innsender={innsender}
          mottattDato={mottattDato}
          avdoedErForelderVilkaar={
            doedsfallVilkaar &&
            doedsfallVilkaar.kriterier.find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER)
              .resultat === VilkaarVurderingsResultat.OPPFYLT
          }
        />
        <Familieforhold
          soekerPdl={soekerPdl}
          soekerSoknad={soekerSoknad}
          avdoedPersonPdl={avdoedPersonPdl}
          avdodPersonSoknad={avdodPersonSoknad}
          gjenlevendePdl={gjenlevendePdl}
          gjenlevendeSoknad={gjenlevendeSoknad}
        />
        <SoeknadGyldigFremsatt />
      </ContentHeader>
    </Content>
  )
}
