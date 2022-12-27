import styled from 'styled-components'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { WarningColored } from '@navikt/ds-icons'
import { SuccessColored } from '@navikt/ds-icons'

export const GyldighetIcon = (props: { status: VurderingsResultat; large?: boolean }) => {
  return (
    <SvgWrapper status={props.status} large={props.large}>
      {props.status === VurderingsResultat.OPPFYLT ? (
        <SuccessColored fontSize={props.large ? 26 : 20} />
      ) : (
        <WarningColored fontSize={props.large ? 26 : 20} />
      )}
    </SvgWrapper>
  )
}

const SvgWrapper = styled.div<{ status: VurderingsResultat; large?: boolean }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
  margin-left: 1em;
`
