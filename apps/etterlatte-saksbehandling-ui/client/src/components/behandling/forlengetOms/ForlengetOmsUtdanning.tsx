import { Alert, BodyShort, Heading, Radio, RadioGroup, TextField, VStack } from '@navikt/ds-react'
import { useForlengetOms } from './ForlengetOmsContext'
import { ForlengetOmsPeriode } from './ForlengetOmsPeriode'
import { JaNei } from '~shared/types/ISvar'

export const ForlengetOmsUtdanning = () => {
  const { kanRedigere, aktivitetUtdanning, oppdaterUtdanning } = useForlengetOms()
  const { skole, studium, studererMinst50Prosent } = aktivitetUtdanning

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilken skole skal bruker studere ved?"
            value={skole}
            onChange={(event) => oppdaterUtdanning({ skole: event.target.value })}
            readOnly={!kanRedigere}
          />
        </VStack>
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hva skal bruker studere?"
            value={studium}
            onChange={(event) => oppdaterUtdanning({ studium: event.target.value })}
            readOnly={!kanRedigere}
          />
        </VStack>
      </VStack>
      <ForlengetOmsPeriode>
        <VStack gap="space-16">
          <RadioGroup
            legend="Studerer bruker minst 50 prosent?"
            value={studererMinst50Prosent}
            onChange={(verdi: JaNei) => oppdaterUtdanning({ studererMinst50Prosent: verdi })}
            disabled={!kanRedigere}
          >
            <Radio value="JA">Ja, bruker studerer minst 50 prosent</Radio>
            <Radio value="NEI">Nei, prosenten er lavere</Radio>
          </RadioGroup>
          {studererMinst50Prosent === 'NEI' && (
            <Alert variant="error">
              <Heading size="small" level="3" spacing>
                Ikke rett til forlenget omstillingstønad
              </Heading>
              <BodyShort>Personen må studere minst 50% for å få stønad</BodyShort>
            </Alert>
          )}
        </VStack>
      </ForlengetOmsPeriode>
    </>
  )
}
