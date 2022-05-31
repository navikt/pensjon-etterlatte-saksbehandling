import { Saksliste, SakslisteProps } from './saksliste'
import styled from 'styled-components'
import { Next } from '@navikt/ds-icons'
import { Button } from '@navikt/ds-react'

export const Saksoversikt = (props: SakslisteProps) => {
  return (
    <SaksoversiktWrapper>
      <h1>Barnepensjon</h1>
      <InfoWrapper>
        <div>
          <Col>Sakstype</Col>
          <Value>Nasjonal</Value>
        </div>

        <div>
          <Col>Gjelder</Col>
          <Value>Førstegangsbehandling</Value>
        </div>

        <div>
          <Col>Status</Col>
          <Value>Løpende</Value>
        </div>

        <IconButton onClick={() => console.log('test')}>
          <Next fontSize={30} />
        </IconButton>
      </InfoWrapper>

      <div className="behandlinger">
        <h2>Behandlinger</h2>
        <Saksliste {...props} />
      </div>
      <Button variant="secondary" size="medium" className="button" onClick={() => console.log('test')}>
        Opprett ny behandling
      </Button>
    </SaksoversiktWrapper>
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
  margin: 3em 1em;
  .behandlinger {
    margin-top: 5em;
  }

  h1 {
    margin-bottom: 1em;
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
