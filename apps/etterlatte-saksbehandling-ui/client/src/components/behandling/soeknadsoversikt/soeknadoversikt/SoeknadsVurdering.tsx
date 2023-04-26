import { BodyLong, Heading } from '@navikt/ds-react'
import { ReactNode } from 'react'
import styled from 'styled-components'
import { SoeknadOversiktWrapper } from '../styled'
import { StatusIconProps, StatusIcon } from '~shared/icons/statusIcon'
import { HjemmelLenke, HjemmelLenkeProps } from '~components/behandling/felles/HjemmelLenke'

interface SoeknadsVurderingProps {
  tittel: string
  hjemler: HjemmelLenkeProps[]
  children: ReactNode
  status: StatusIconProps | null
}

export const Soeknadsvurdering = (props: SoeknadsVurderingProps) => {
  return (
    <VurderingWrapper>
      <HeadingMedIkon>
        {props.status && <StatusIcon status={props.status} />} {props.tittel}
      </HeadingMedIkon>

      <HjemmelWrapper>
        {props.hjemler.map((hjemmel, idx) => (
          <HjemmelLenke key={`hjemmel-${idx}`} {...hjemmel} />
        ))}
      </HjemmelWrapper>

      <SoeknadOversiktWrapper>{props.children}</SoeknadOversiktWrapper>
    </VurderingWrapper>
  )
}

const HeadingMedIkon = styled(Heading).attrs({ size: 'medium', level: '2' })`
  display: flex;
  gap: 12px;
`

const VurderingWrapper = styled.div`
  margin-top: 3em;
`

const HjemmelWrapper = styled(BodyLong)`
  margin-top: 6px;
  display: flex;
  gap: 24px;
  flex-grow: 1;
  flex-wrap: wrap;
  flex-direction: row;
`
