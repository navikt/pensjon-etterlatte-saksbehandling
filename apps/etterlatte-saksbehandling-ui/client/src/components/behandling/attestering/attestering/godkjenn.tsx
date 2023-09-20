import { Textarea } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper, Text } from '../styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { AttesterVedtak } from '~components/behandling/attestering/handinger/attesterVedtak'

export const Godkjenn = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')

  return (
    <BeslutningWrapper>
      <div>
        <Text>Tilbakemelding fra attestant</Text>
        <Textarea
          style={{ padding: '10px' }}
          label="Kommentar fra attestant"
          hideLabel={true}
          placeholder="Beskriv etterarbeid som er gjort. f.eks. overført oppgave til NØP om kontonummer / skattetrekk."
          value={tilbakemeldingFraAttestant}
          onChange={(e) => setTilbakemeldingFraAttestant(e.target.value)}
          minRows={3}
          size="small"
          autoComplete="off"
        />
      </div>
      <br />
      <AttesterVedtak behandling={behandling} kommentar={tilbakemeldingFraAttestant} />
    </BeslutningWrapper>
  )
}
