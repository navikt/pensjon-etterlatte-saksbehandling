import { DetailWrapper, WarningIconWrapper } from '../styled'
import { WarningIcon } from '../../../../shared/icons/warningIcon'
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
    <DetailWrapper>
      <Label size="small" className={labelClassName}>
        {!erOppfylt && (
          <WarningIconWrapper>
            <WarningIcon />
          </WarningIconWrapper>
        )}
        {label}
      </Label>
      <span>{navn}</span>
      <div className={tekstClassName}>{tekst}</div>
    </DetailWrapper>
  )
}
