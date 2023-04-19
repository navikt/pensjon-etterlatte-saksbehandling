import { Alert, BodyShort, Button, Heading, Label } from '@navikt/ds-react'
import styled from 'styled-components'
import { Grunnlagsendringshendelse } from '~components/person/typer'
import {
  grunnlagsendringsBeskrivelse,
  grunnlagsendringsKilde,
  grunnlagsendringsTittel,
} from '~components/person/uhaandtereHendelser/utils'
import { formaterStringDato } from '~utils/formattering'

type Props = {
  hendelser: Array<Grunnlagsendringshendelse>
  startRevurdering: () => void
  disabled: boolean
}

const UhaandterteHendelser = (props: Props) => {
  const { hendelser, disabled, startRevurdering } = props
  if (hendelser.length === 0) return null

  return (
    <div>
      <StyledAlert>Ny hendelse som kan kreve revurdering. Vurder om det har konsekvens for ytelsen.</StyledAlert>
      <Heading size="medium">Hendelser</Heading>
      {hendelser.map((hendelse) => (
        <UhaandtertHendelse
          key={hendelse.id}
          hendelse={hendelse}
          disabled={disabled}
          startRevurdering={startRevurdering}
        />
      ))}
    </div>
  )
}

const StyledAlert = styled(Alert).attrs({ variant: 'warning' })`
  display: inline-flex;
  margin: 1em 0;
`

const UhaandtertHendelse = (props: {
  hendelse: Grunnlagsendringshendelse
  disabled: boolean
  startRevurdering: () => void
}) => {
  const { hendelse, disabled, startRevurdering } = props
  const { type, opprettet } = hendelse

  return (
    <Wrapper>
      <Label>{grunnlagsendringsTittel[type]}</Label>
      <Content>
        <Header>
          <BodyShort>Dato</BodyShort>
          <BodyShort size="small">{formaterStringDato(opprettet)}</BodyShort>
        </Header>
        <Header>
          <BodyShort size="small">Hendelse fra {grunnlagsendringsKilde(type)}</BodyShort>
          <BodyShort size="small" style={{ maxWidth: '25em' }}>
            {grunnlagsendringsBeskrivelse[type]}
          </BodyShort>
        </Header>
        <Button disabled={disabled} onClick={startRevurdering}>
          Start revurdering
        </Button>
      </Content>
    </Wrapper>
  )
}

const Wrapper = styled.div`
  display: 'flex';
  gap: 1em;
  margin-bottom: 1rem;
  max-width: 50rem;
`
const Header = styled.div`
  display: flex;
  flex-direction: column;
`

const Content = styled.div`
  display: grid;
  grid-template-columns: 5rem auto 13rem;
  gap: 2em;
  align-items: center;
`

export default UhaandterteHendelser
