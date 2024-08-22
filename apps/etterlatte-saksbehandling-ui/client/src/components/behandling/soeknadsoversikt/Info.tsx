import { BodyShort, Detail, Label } from '@navikt/ds-react'
import { ReactNode } from 'react'
import styled from 'styled-components'

interface InfoProps {
  label: string
  tekst: ReactNode
  undertekst?: string
  wide?: boolean
}

export const Info = ({ label, tekst, undertekst, wide }: InfoProps) => {
  return (
    <InfoElement $wide={wide ?? false}>
      <Label size="small" as="p">
        {label}
      </Label>
      <BodyShort as="div">{tekst}</BodyShort>
      {undertekst && <Detail>{undertekst}</Detail>}
    </InfoElement>
  )
}

const InfoElement = styled.div<{ $wide?: boolean }>`
  width: ${(props) => (props.$wide ? '100%' : '15em')};
`
