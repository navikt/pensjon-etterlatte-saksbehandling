import { Heading, HStack, VStack } from '@navikt/ds-react'
import { ReactNode } from 'react'
import styled from 'styled-components'
import { StatusIcon, StatusIconProps } from '~shared/icons/statusIcon'
import { HjemmelLenke, HjemmelLenkeProps } from '~components/behandling/felles/HjemmelLenke'

interface LovtekstMedLenkeProps {
  tittel: string
  hjemler: HjemmelLenkeProps[]
  children: ReactNode
  status: StatusIconProps | null
}

export const LovtekstMedLenke = (props: LovtekstMedLenkeProps) => {
  return (
    <VurderingWrapper>
      <VStack gap="2">
        <HeadingMedIkon size="medium" level="2">
          {props.status && <StatusIcon status={props.status} />} {props.tittel}
        </HeadingMedIkon>
        <HStack gap="4">
          {props.hjemler.map((hjemmel, idx) => (
            <HjemmelLenke key={`hjemmel-${idx}`} {...hjemmel} />
          ))}
        </HStack>

        <HStack justify="space-between" wrap={false}>
          {props.children}
        </HStack>
      </VStack>
    </VurderingWrapper>
  )
}

const HeadingMedIkon = styled(Heading)`
  display: flex;
  gap: 12px;
`

const VurderingWrapper = styled.div`
  margin-top: 3em;
`
