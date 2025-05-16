import { BodyShort, HStack, Label, VStack } from '@navikt/ds-react'
import { EnvelopeClosedIcon } from '@navikt/aksel-icons'

type Props = {
  tekst: string
  body?: string
}

const EtteroppgjoerResultatVisning = ({ tekst, body }: Props) => (
  <HStack gap="2" maxWidth="fit-content">
    <EnvelopeClosedIcon fontSize="1.5rem" aria-hidden />
    <VStack gap="2" maxWidth="42.5rem" marginBlock="05 0">
      <Label>{tekst}</Label>
      {body && <BodyShort>{body}</BodyShort>}
    </VStack>
  </HStack>
)

export default EtteroppgjoerResultatVisning
