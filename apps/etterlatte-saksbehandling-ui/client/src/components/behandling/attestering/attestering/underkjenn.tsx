import { Select, Textarea } from '@navikt/ds-react'
import { useState } from 'react'
import { UnderkjennVedtak } from '../handinger/underkjennVedtak'
import { BeslutningWrapper, Text } from '../styled'
import { IReturType } from '../types'

export const Underkjenn = ({ behandlingId }: { behandlingId: string }) => {
  const [tilbakemeldingFraAttestant, setTilbakemeldingFraAttestant] = useState('')
  const [returType, setReturType] = useState<IReturType>(IReturType.inngangsvilkår_feilvurdert)

  const options: IReturType[] = [
    IReturType.inngangsvilkår_feilvurdert,
    IReturType.feil_i_beregning,
    IReturType.feil_i_brev,
    IReturType.dokumentasjon_mangler,
    IReturType.annet,
  ]

  return (
    <BeslutningWrapper>
      <div>
        <Text>Årsak til retur</Text>
        <Select
          label="Årsak til retur"
          value={returType || ''}
          onChange={(e) => setReturType(e.target.value as IReturType)}
        >
          {options.map((option) => (
            <option key={option} value={option}>
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
      <UnderkjennVedtak
        behandlingId={behandlingId}
        kommentar={tilbakemeldingFraAttestant}
        valgtBegrunnelse={returType}
      />
    </BeslutningWrapper>
  )
}
