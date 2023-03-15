import styled from 'styled-components'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
  behandling: IDetaljertBehandling
}

export const Attestering: React.FC<Props> = ({ setBeslutning, beslutning, behandling }) => {
  const { lastPage } = useBehandlingRoutes()

  return (
    <AttesteringWrapper>
      <div className="info">
        <Overskrift>Kontroller opplysninger og faglige vurderinger gjort under behandling.</Overskrift>
      </div>
      <TextWrapper>
        Beslutning
        {lastPage ? (
          <Beslutningsvalg beslutning={beslutning} setBeslutning={setBeslutning} behandling={behandling} />
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
