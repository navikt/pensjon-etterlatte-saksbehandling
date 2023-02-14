import { ExternalLink } from '@navikt/ds-icons'
import { BodyLong, Heading, Link } from '@navikt/ds-react'
import { ReactNode } from 'react'
import styled from 'styled-components'
import { SoeknadOversiktWrapper } from '../styled'
import { StatusIconProps, StatusIcon } from '~shared/icons/statusIcon'

interface HjemmelLenkeProps {
  tittel: string
  lenke: string
}

const HjemmelLenke = (props: HjemmelLenkeProps) => {
  return (
    <Link href={props.lenke} target="_blank" rel="noopener noreferrer">
      {props.tittel}
      <ExternalLink title={props.tittel} />
    </Link>
  )
}

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
