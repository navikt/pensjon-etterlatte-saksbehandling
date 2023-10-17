import { Select, Textarea } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper, Text } from '../styled'
import { UnderkjennVedtak } from '~components/behandling/attestering/handinger/underkjennVedtak'
import { useVedtak } from '~components/vedtak/useVedtak'
import { VedtakType } from '~components/vedtak/typer'
import { IReturTypeBehandling, IReturTypeTilbakekreving } from '~components/behandling/attestering/types'

export const Underkjenn = () => {
  const vedtak = useVedtak()
  const aarsak = vedtak?.vedtakType === VedtakType.TILBAKEKREVING ? IReturTypeTilbakekreving : IReturTypeBehandling
  type aarsakTyper = Array<keyof typeof aarsak>

  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')
  const [returType, setReturType] = useState<aarsakTyper[number]>('velg')

  return (
    <BeslutningWrapper>
      <div>
        <Text>Årsak til retur</Text>
        <Select
          label="Årsak til retur"
          hideLabel={true}
          value={returType || ''}
          onChange={(e) => setReturType(e.target.value as aarsakTyper[number])}
        >
          {(Object.keys(aarsak) as aarsakTyper).map((option) => (
            <option key={option} value={option}>
              {aarsak[option]}
            </option>
          ))}
        </Select>
      </div>
      <div className="textareaWrapper">
        <Text>Tilbakemelding fra attestant</Text>
        <Textarea
          style={{ padding: '10px' }}
          label="Tilbakemelding fra attestant"
          hideLabel={true}
          placeholder="Forklar hvorfor vedtak er underkjent og hva som må rettes"
          value={tilbakemeldingFraAttestant}
          onChange={(e) => setTilbakemeldingFraAttestant(e.target.value)}
          minRows={3}
          size="small"
          autoComplete="off"
        />
      </div>
      <UnderkjennVedtak kommentar={tilbakemeldingFraAttestant} valgtBegrunnelse={returType} />
    </BeslutningWrapper>
  )
}
