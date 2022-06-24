import { Select, Textarea } from '@navikt/ds-react'
import { useState } from 'react'
import { UnderkjennVedtak } from './handinger/underkjennVedtak'
import { BeslutningWrapper, Text } from './styled'
import { IRetyrType } from './types'

export const Underkjenn = () => {
  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')

  const options: IRetyrType[] = [
    IRetyrType.inngangsvilkår_feilvurdert,
    IRetyrType.feil_i_beregning,
    IRetyrType.feil_i_brev,
    IRetyrType.dokumentasjon_mangler,
    IRetyrType.annet,
  ]

  return (
    <BeslutningWrapper>
      <div>
        <Text>Årsak til retur</Text>
        <Select label="Årsak til retur" hideLabel={true}>
          {options.map((option) => (
            <option key={option} value="">
              {option}
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
        />
      </div>
      <UnderkjennVedtak />
    </BeslutningWrapper>
  )
}
