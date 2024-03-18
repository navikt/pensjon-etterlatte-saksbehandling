import { Select, Textarea } from '@navikt/ds-react'
import { useState } from 'react'
import { BeslutningWrapper, Text } from '../styled'
import { UnderkjennVedtak } from '~components/behandling/attestering/handinger/underkjennVedtak'
import { useVedtak } from '~components/vedtak/useVedtak'
import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'
import {
  IReturTypeBehandling,
  IReturTypeKlage,
  IReturTypeTilbakekreving,
} from '~components/behandling/attestering/types'

type velg = 'velg'
export const Underkjenn = () => {
  const vedtak = useVedtak()
  const aarsaktype = aarsaktypeFor(vedtak)
  type aarsakTyper = Array<keyof typeof aarsaktype>

  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')
  const [returType, setReturType] = useState<aarsakTyper[number] | velg>('velg')

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
          <option value="velg" disabled={true}>
            Velg
          </option>
          {(Object.keys(aarsaktype) as aarsakTyper).map((option) => (
            <option key={option} value={option}>
              {aarsaktype[option]}
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

function aarsaktypeFor(vedtak: VedtakSammendrag | null) {
  switch (vedtak?.vedtakType) {
    case VedtakType.TILBAKEKREVING:
      return IReturTypeTilbakekreving
    case VedtakType.AVVIST_KLAGE:
      return IReturTypeKlage
    default:
      return IReturTypeBehandling
  }
}
