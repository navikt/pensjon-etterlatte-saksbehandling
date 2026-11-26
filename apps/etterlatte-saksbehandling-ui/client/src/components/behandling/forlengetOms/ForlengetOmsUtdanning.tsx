import { Alert, BodyShort, Heading, Radio, RadioGroup, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useForlengetOms } from './ForlengetOmsContext'
import { ForlengetOmsPeriode } from './ForlengetOmsPeriode'

export const ForlengetOmsUtdanning = () => {
  const { utkast, oppdaterFelt, sporsmaalRedigerbare } = useForlengetOms()
  const { skole, studium, studererMinst50Prosent, begrunnelse } = utkast

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilken skole skal bruker studere ved?"
            value={skole}
            onChange={(event) => oppdaterFelt('skole', event.target.value)}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hva skal bruker studere?"
            value={studium}
            onChange={(event) => oppdaterFelt('studium', event.target.value)}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
      </VStack>
      <ForlengetOmsPeriode>
        <VStack gap="space-16">
          <RadioGroup
            legend="Studerer bruker minst 50 prosent?"
            value={studererMinst50Prosent}
            onChange={(verdi) => oppdaterFelt('studererMinst50Prosent', verdi)}
            disabled={!sporsmaalRedigerbare}
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
        {studererMinst50Prosent && (
          <VStack gap="space-16" maxWidth="30rem">
            <Textarea
              label={studererMinst50Prosent === 'JA' ? 'Er utdanningen nødvendig og hensiktsmessig?' : 'Begrunnelse'}
              description={
                studererMinst50Prosent === 'JA'
                  ? 'Begrunn hvorfor utdanningen er nødvendig og hensiktsmessig eller ikke'
                  : undefined
              }
              value={begrunnelse}
              onChange={(event) => oppdaterFelt('begrunnelse', event.target.value)}
              minRows={6}
              readOnly={!sporsmaalRedigerbare}
            />
          </VStack>
        )}
      </ForlengetOmsPeriode>
    </>
  )
}
