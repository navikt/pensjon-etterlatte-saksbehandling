import { Label } from '@navikt/ds-react'
import { DetailWrapper, Infoboks } from '../../styled'

//TODO
export const Verge = () => {
  return (
    <Infoboks>
      <DetailWrapper>
        <Label size="small" className="headertext">
          Verge
        </Label>
        <span>-</span>
      </DetailWrapper>
    </Infoboks>
  )
}
