import { useState } from 'react'
import { RadioGroup, Radio, Textarea, Button } from '@navikt/ds-react'
import { RadioGroupWrapper } from '../styled'
import { useBehandlingRoutes } from '../../BehandlingRoutes'

export const SoeknadGyldigFremsatt = () => {
  /*
  TODO Må være mulig å lagre svaret. Når det trykkes på lagre-knappen skal 
  informasjonsgrunnlaget som lå til grunn på det tidpunktet svaret ble avgitt
  lagres. Må være mulig (dette gjelder generelt for vilkårsvurdering) å kunne
  se tilbake på/vise frem informasjonsgrunnlaget i ettertid.
  */
  const [soeknadIkkeGyldig, setSoeknadIkkeGyldig] = useState<boolean>(false)
  const [soeknadGyldigBegrunnelse, setSoeknadGyldigBegrunnelse] = useState('')
  const { next } = useBehandlingRoutes()
  return (
    <RadioGroupWrapper>
      <RadioGroup
        legend="Er søknaden gyldig fremsatt?"
        size="small"
        className="radioGroup"
        onChange={(event) => (event === 'ja' ? setSoeknadIkkeGyldig(false) : setSoeknadIkkeGyldig(true))}
      >
        <Radio value="ja">Ja</Radio>
        <Radio value="nei">Nei</Radio>
      </RadioGroup>
      {soeknadIkkeGyldig && (
        <Textarea
          label="Begrunnelse (hvis aktuelt)"
          value={soeknadGyldigBegrunnelse}
          onChange={(e) => setSoeknadGyldigBegrunnelse(e.target.value)}
          minRows={2}
          maxLength={400}
          size="small"
        />
      )}
      <Button variant="primary" size="medium" className="button" onClick={next}>
        Lagre
      </Button>
    </RadioGroupWrapper>
  )
}
