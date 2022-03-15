import { Alert } from '@navikt/ds-react'
import { AlertWrapper } from '../styled'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'

interface Props {
  varselType: string
}
export const AlertVarsel: React.FC<Props> = ({ varselType }) => {
  const { avdodPersonPdl, avdodPersonSoknad } = usePersonInfoFromBehandling()

  const varsel = () => {
    switch (varselType) {
      case 'ikke riktig oppgitt avdød i søknad':
        return `I PDL er det oppgitt ${avdodPersonPdl?.fornavn} ${avdodPersonPdl?.etternavn} som avdød forelder, men i søknad er det oppgitt ${avdodPersonSoknad?.fornavn} ${avdodPersonSoknad?.etternavn}. Må avklares.`
      case 'forelder ikke død':
        return 'Oppgitt avdød i søknad er ikke forelder til barnet. Må avklares.'
      case 'ikke lik adresse':
        return 'Adresse til gjenlevende foreldre er ulik fra oppgitt i søknad og PDL. Orienter innsender og avklar hvilken adresse som stemmer.'
      case 'dødsfall 3 år':
        return 'Dato for dødsfall er mer enn 3 år tilbake i tid. Første mulig virkningstidspunkt er senere enn første måned etter dødsfall.'
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
