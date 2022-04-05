import styled from 'styled-components'
import { Alert } from '@navikt/ds-react'
import { usePersonInfoFromBehandling } from './usePersonInfoFromBehandling'

interface Props {
  varselType: string
}
export const AlertVarsel: React.FC<Props> = ({ varselType }) => {
  const { avdoedPersonPdl, avdodPersonSoknad } = usePersonInfoFromBehandling()

  const varsel = () => {
    switch (varselType) {
      case 'ikke riktig oppgitt avdød i søknad':
        return `I PDL er det oppgitt ${avdoedPersonPdl?.fornavn} ${avdoedPersonPdl?.etternavn} som avdød forelder, men i søknad er det oppgitt ${avdodPersonSoknad?.fornavn} ${avdodPersonSoknad?.etternavn}. Må avklares.`
      case 'forelder ikke død':
        return 'Oppgitt avdød i søknad er ikke forelder til barnet. Må avklares.'
      case 'ikke lik adresse':
        return 'Adresse til gjenlevende foreldre er ulik fra oppgitt i søknad og PDL. Orienter innsender og avklar hvilken adresse som stemmer.'
      case 'dødsfall 3 år':
        return 'Dato for dødsfall er mer enn 3 år tilbake i tid. Første mulig virkningstidspunkt er senere enn første måned etter dødsfall.'
      case 'mangler':
        return 'Mangler info om avdød. Må avklares'
    }
  }

  return (
    <AlertWrapper>
      <Alert variant="warning" className="alert" size="small">
        {varsel()}
      </Alert>
    </AlertWrapper>
  )
}

export const AlertWrapper = styled.div`
  min-width: 200px;
  max-width: 350px;

  .alert {
    font-size: 10px;
    padding: 1em;
  }
`
