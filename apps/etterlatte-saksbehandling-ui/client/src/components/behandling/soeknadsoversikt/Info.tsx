import { BodyShort, Label } from '@navikt/ds-react'
import { InfoElement } from './styled'

export const Info = ({ tekst, label }: { tekst: string; label: string }) => {
  return (
    <InfoElement>
      <Label size="small" as={'p'}>
        {label}
      </Label>
      <BodyShort>{tekst}</BodyShort>
    </InfoElement>
  )
}
