import { useState } from 'react'
import { RadioGroup, Radio, Textarea, Button } from '@navikt/ds-react'
import { RadioGroupWrapper } from '../styled'
import { useBehandlingRoutes } from '../../BehandlingRoutes'

export const SoeknadGyldigFremsatt = () => {
  const [soeknadGyldigBegrunnelse, setSoeknadGyldigBegrunnelse] = useState('')
  const { next } = useBehandlingRoutes()

  return (
    <RadioGroupWrapper>
      <RadioGroup legend="Er søknaden gyldig fremsatt?" size="small" className="radioGroup">
        <Radio value="10">Ja</Radio>
        <Radio value="20">Nei</Radio>
      </RadioGroup>
      <Textarea
        label="Begrunnelse (hvis aktuelt)"
        value={soeknadGyldigBegrunnelse}
        onChange={(e) => setSoeknadGyldigBegrunnelse(e.target.value)}
        minRows={2}
        maxLength={400}
        size="small"
      />
      <Button variant="primary" size="medium" className="button" onClick={next}>
        Lagre
      </Button>
    </RadioGroupWrapper>
  )
}
