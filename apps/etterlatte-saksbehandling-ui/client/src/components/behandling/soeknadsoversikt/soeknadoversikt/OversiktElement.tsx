import { DetailWrapper, Infoboks } from '../styled'
import { Label } from '@navikt/ds-react'

interface Props {
  navn: string | undefined
  label: string
  tekst: string
  erOppfylt: boolean
}

export const OversiktElement = ({ navn, label, tekst, erOppfylt }: Props) => {
  const labelClassName = erOppfylt ? '' : 'labelWrapperWithIcon'
  const tekstClassName = erOppfylt ? '' : 'warningText'

  return (
    <Infoboks>
      <DetailWrapper>
        <Label size="small" as={"p"} className={labelClassName}>
          {label}
        </Label>
        <span>{navn}</span>
        <div className={tekstClassName}>{tekst}</div>
      </DetailWrapper>
    </Infoboks>
  )
}
