import { BodyShort, Detail, Label } from '@navikt/ds-react'
import { InfoElement } from './styled'
import { ReactNode } from 'react'

interface InfoProps {
  label: string
  tekst: ReactNode
  undertekst?: string
}

export const Info = ({ label, tekst, undertekst }: InfoProps) => {
  return (
    <InfoElement>
      <Label size="small" as="p">
        {label}
      </Label>
      <BodyShort>{tekst}</BodyShort>
      {undertekst && <Detail>{undertekst}</Detail>}
    </InfoElement>
  )
}
