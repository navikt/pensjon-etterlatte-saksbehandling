import { Saksliste } from './saksliste'
import styled from 'styled-components'
import { Next } from '@navikt/ds-icons'
import { Button } from '@navikt/ds-react'
import { Behandling, Sak, SakslisteProps } from './typer'

export const Saksoversikt = ({
  saksliste,
  opprettBehandling,
  goToBehandling,
}: {
  saksliste: SakslisteProps
  opprettBehandling: any
  goToBehandling: (behandlingsId: string) => void
}) => {
  const sisteBehandlingISak = (sak: Sak): Behandling => {
    return sak.behandlinger.reduce((a, b) => (a.opprettet > b.opprettet ? a : b))
  }

  return (
    <>
      {saksliste.saker.map((sak) => (
        <SaksoversiktWrapper key={sak.sakId}>
          <h1>{sak.type}</h1>

          <InfoWrapper>
            <div>
              <Col>Sakstype</Col>
              <Value>{sak.sakstype}</Value>
            </div>

            <div>
              <Col>Gjelder</Col>
              <Value>{sisteBehandlingISak(sak).type}</Value>
            </div>

            <div>
              <Col>Status</Col>
              <Value>{sisteBehandlingISak(sak).status}</Value>
            </div>

            <IconButton onClick={() => goToBehandling(sisteBehandlingISak(sak).id.toString())}>
              <Next fontSize={30} />
            </IconButton>
          </InfoWrapper>

          <div className="behandlinger">
            <h2>Behandlinger</h2>
            <Saksliste saksliste={sak.behandlinger} goToBehandling={goToBehandling} />
          </div>
          <Button variant="secondary" size="medium" className="button" onClick={opprettBehandling}>
            Opprett ny behandling
          </Button>
        </SaksoversiktWrapper>
      ))}
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
