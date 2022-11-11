import styled from 'styled-components'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { SynchronizeIcon } from '~shared/icons/synchronizeIcon'
import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
}

export const Attestering: React.FC<Props> = ({ setBeslutning, beslutning }) => {
  const { currentRoute } = useBehandlingRoutes()

  return (
    <AttesteringWrapper>
      <div className="info">
        <Overskrift>Kontroller opplysninger og faglige vurderinger gjort under behandling.</Overskrift>
      </div>
      <VurderingTekstWrapper>
        <div className="icon">
          <SynchronizeIcon />
        </div>
        <div className="tekst">
          Hardkodet: Steget er vurdert automatisk
          {/** TODO: dato for når hvert steg i behabdlingen er vurdert skal vises her: 21.09.21 */}. Ingen faresignaler
          oppdaget.
        </div>
      </VurderingTekstWrapper>
      <TextWrapper>
        Beslutning
        {currentRoute === 'brev' ? (
          <Beslutningsvalg beslutning={beslutning} setBeslutning={setBeslutning} />
        ) : (
          <>
            <Tekst>Se gjennom alle steg før du tar en beslutning.</Tekst>
          </>
        )}
      </TextWrapper>
    </AttesteringWrapper>
  )
}

const AttesteringWrapper = styled.div`
  margin: 1em;

  .info {
    margin-top: 1em;
    margin-bottom: 1em;
    padding: 1em;
  }
`

const TextWrapper = styled.div`
  font-size: 18px;
  font-weight: 600;
  margin: 1em;
  color: #595959;
`

const Overskrift = styled.div`
  font-weight: 600;
  font-size: 16px;
  line-height: 22px;
  color: #3e3832;
`

const Tekst = styled.div`
  font-size: 18px;
  font-weight: 400;
  color: #3e3832;
  margin-top: 6px;
`

const VurderingTekstWrapper = styled.div`
  margin-top: 15px;
  color: #262626;
  display: flex;
  .icon {
    color: #262626;
    margin-right: 10px;
  }

  .tekst {
    font-style: normal;
    font-weight: 400;
    font-size: 14px;
    line-height: 18px;
  }
`
