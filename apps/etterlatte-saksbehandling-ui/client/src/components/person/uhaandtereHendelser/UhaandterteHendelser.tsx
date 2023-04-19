import { Alert, BodyShort, Button, Heading, Label } from '@navikt/ds-react'
import styled from 'styled-components'
type Hendelse = {
  id: string
  tittel: string
  dato: string
  kilde: string
  beskrivelse: string
}

type Props = {
  hendelser: Array<Hendelse>
  startRevurdering: () => void
  disabled: boolean
}

const UhaandterteHendelser = (props: Props) => {
  return (
    <div>
      <StyledAlert>Ny hendelse som kan kreve revurdering. Vurder om det har konsekvens for ytelsen.</StyledAlert>
      <Heading size="medium">Hendelser</Heading>
      {props.hendelser.map((hendelse) => (
        <UhaandtertHendelse
          key={hendelse.id}
          hendelse={hendelse}
          disabled={props.disabled}
          startRevurdering={props.startRevurdering}
        />
      ))}
    </div>
  )
}

const StyledAlert = styled(Alert).attrs({ variant: 'warning' })`
  display: inline-flex;
  margin: 1em 0;
`

const UhaandtertHendelse = (props: { hendelse: Hendelse; disabled: boolean; startRevurdering: () => void }) => {
  return (
    <Wrapper>
      <Label>{props.hendelse.tittel}</Label>
      <Foo>
        <Header>
          <BodyShort>Dato</BodyShort>
          <BodyShort size="small">{props.hendelse.dato}</BodyShort>
        </Header>
        <Header>
          <BodyShort size="small">Hendelse fra {props.hendelse.kilde}</BodyShort>
          <BodyShort size="small" style={{ maxWidth: '25em' }}>
            {props.hendelse.beskrivelse}
          </BodyShort>
        </Header>
        <Button disabled={props.disabled} onClick={props.startRevurdering}>
          Start revurdering
        </Button>
      </Foo>
    </Wrapper>
  )
}

const Wrapper = styled.div`
  display: 'flex';
  gap: 1em;
`
const Header = styled.div`
  display: inline-flex;
  flex-direction: column;
`

const Foo = styled.div`
  display: flex;
  gap: 2em;
  align-items: center;
`

export default UhaandterteHendelser
