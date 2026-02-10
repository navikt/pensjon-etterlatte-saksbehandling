import { Textarea, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { AttesterVedtak } from '~components/behandling/attestering/handinger/attesterVedtak'

export const Godkjenn = () => {
  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')

  return (
    <VStack gap="space-4">
      <Textarea
        style={{ padding: '10px' }}
        label="Tilbakemelding fra attestant"
        placeholder="Beskriv etterarbeid som er gjort. f.eks. overført oppgave til NØP om kontonummer."
        value={tilbakemeldingFraAttestant}
        onChange={(e) => setTilbakemeldingFraAttestant(e.target.value)}
        minRows={3}
        size="small"
        autoComplete="off"
      />

      <div>
        <AttesterVedtak kommentar={tilbakemeldingFraAttestant} />
      </div>
    </VStack>
  )
}
