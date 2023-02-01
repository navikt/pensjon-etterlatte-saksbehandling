import { BodyShort, Label } from '@navikt/ds-react'
import { InfoElement } from './styled'

export const Info = ({ tekst, label, undertekst }: { tekst: string; label: string; undertekst?: string }) => {
  return (
    <InfoElement>
      <Label size="small" as={'p'}>
        {label}
      </Label>
      <BodyShort>{tekst}</BodyShort>
      {undertekst && <BodyShort size="small">{undertekst}</BodyShort>}
    </InfoElement>
  )
}
