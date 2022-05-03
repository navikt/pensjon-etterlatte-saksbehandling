import { PropsOmSoeknad } from '../props'
import styled from 'styled-components'
import { SoeknadOversiktDel1 } from './SoeknadsoversiktDel1'
import { SoeknadOversiktDel2 } from './SoeknadsoversiktDel2'
import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({
  gyldighet,
  avdoedPersonPdl,
  innsender,
  mottattDato,
  gjenlevendePdl,
  gjenlevendeHarForeldreansvar,
  gjenlevendeOgSoekerLikAdresse,
  innsenderHarForeldreAnsvar,
}) => {
  return (
    <SoeknadOversiktWrapper>
      <div>
        <SoeknadOversiktDel1
          innsender={innsender}
          gjenlevendePdl={gjenlevendePdl}
          gjenlevendeHarForeldreansvar={gjenlevendeHarForeldreansvar}
          innsenderHarForeldreAnsvar={innsenderHarForeldreAnsvar}
          gyldighet={gyldighet}
        />

        {gyldighet.resultat === VurderingsResultat.OPPFYLT && (
          <SoeknadOversiktDel2
            avdoedPersonPdl={avdoedPersonPdl}
            mottattDato={mottattDato}
            gjenlevendeOgSoekerLikAdresse={gjenlevendeOgSoekerLikAdresse}
            gyldighet={gyldighet}
          />
        )}
      </div>
    </SoeknadOversiktWrapper>
  )
}

export const SoeknadOversiktWrapper = styled.div`
  flex-wrap: wrap;
  margin-bottom: 2em;
  padding-left: 5em;
`

export const InfoWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);

  > * {
    width: 180px;
  }
  height: 120px;
  flex-grow: 1;
`
