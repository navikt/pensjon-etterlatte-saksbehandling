//import { Textarea } from '@navikt/ds-react'
//import { useState } from 'react'
import { AttesterVedtak } from '../handinger/attesterVedtak'
import { BeslutningWrapper /*Text*/ } from '../styled'

export type PropsIverksett = {
  behandlingId: string
}
export const Godkjenn: React.FC<PropsIverksett> = ({ behandlingId }) => {
  //const [kommentarFraAttestant, setKommentarFraAttestant] = useState('')

  return (
    <BeslutningWrapper>
      {/**
      <div className="textareaWrapper">
        <Text>Kommentar fra attestant</Text>
           <Textarea
          style={{ padding: '10px' }}
          label="Tilbakemelding fra attestant"
          hideLabel={true}
          value={kommentarFraAttestant}
          onChange={(e) => setKommentarFraAttestant(e.target.value)}
          minRows={2}
          size="small"
          autoComplete="off"
        />

      </div>
       */}
      <AttesterVedtak behandlingId={behandlingId} />
    </BeslutningWrapper>
  )
}
