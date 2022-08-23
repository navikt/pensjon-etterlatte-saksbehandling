import { Saksliste } from './saksliste'
import styled from 'styled-components'
import { Next } from '@navikt/ds-icons'
import { IBehandlingsammendrag } from './typer'
import { useNavigate } from 'react-router-dom'
import { formaterEnumTilLesbarString } from '../../utils/formattering'

export const Saksoversikt = ({ behandlingliste }: { behandlingliste: IBehandlingsammendrag[] | undefined }) => {
  const navigate = useNavigate()
  const behandlinger = behandlingliste ? behandlingliste : []

  const sortertListe = behandlinger.sort((a, b) =>
    new Date(b.behandlingOpprettet!) > new Date(a.behandlingOpprettet!) ? 1 : -1
  )
  const sisteBehandling = sortertListe[0]

  const goToBehandling = (behandlingsId: string) => {
    navigate(`/behandling/${behandlingsId}/soeknadsoversikt`)
  }

  return (
    <>
      <SaksoversiktWrapper>
        <h1>Barnepensjon</h1>

        <InfoWrapper>
          <div>
            <Col>Sakstype</Col>
            <Value>Nasjonal</Value>
          </div>

          <div>
            <Col>Gjelder</Col>
            <Value>{formaterEnumTilLesbarString(sisteBehandling?.behandlingType)}</Value>
          </div>

          <div>
            <Col>Status</Col>
            <Value>{formaterEnumTilLesbarString(sisteBehandling.status)}</Value>
          </div>

          <IconButton onClick={() => goToBehandling(sisteBehandling.id.toString())}>
            <Next fontSize={30} />
          </IconButton>
        </InfoWrapper>

        <div className="behandlinger">
          <h2>Behandlinger</h2>
          <Saksliste behandlinger={behandlinger} goToBehandling={goToBehandling} />
        </div>
      </SaksoversiktWrapper>
    </>
  )
}

export const IconButton = styled.div`
  padding-top: 1em;
  color: #000000;
  :hover {
    cursor: pointer;
  }
`

export const SaksoversiktWrapper = styled.div`
  min-width: 40em;
  max-width: 70%;

  margin: 3em 1em;
  .behandlinger {
    margin-top: 5em;
  }

  h1 {
    margin-bottom: 1em;
    text-transform: capitalize;
  }

  .button {
    margin-top: 4em;
    padding-left: 2em;
    padding-right: 2em;
  }
`

export const InfoWrapper = styled.div`
  border: 1px solid #000000;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  padding: 3em;
`

export const Col = styled.div`
  font-weight: 400;
  font-size: 20px;
  line-height: 28px;
  margin-bottom: 10px;
`

export const Value = styled.div`
  font-style: normal;
  font-weight: 600;
  font-size: 20px;
  line-height: 28px;
`
