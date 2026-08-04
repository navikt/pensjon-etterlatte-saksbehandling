import { Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { ReactNode } from 'react'
import { StatusIcon, StatusIconProps } from '~shared/icons/statusIcon'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { Hjemmel } from '~components/behandling/virkningstidspunkt/utils'

interface Props {
  tittel: string
  hjemler: Hjemmel[]
  children: ReactNode
  status: StatusIconProps | null
}

export const SoeknadVurdering = (props: Props) => {
  return (
    <Box background="neutral-soft" borderRadius="12" padding="space-16" marginBlock="space-32">
      <VStack gap="space-8">
        <HStack gap="space-24" align="center">
          {props.status && <StatusIcon status={props.status} />}
          <Heading size="medium" level="2">
            {props.tittel}
          </Heading>
        </HStack>
        <HStack gap="space-16">
          {props.hjemler.map((hjemmel, idx) => (
            <HjemmelLenke key={`hjemmel-${idx}`} {...hjemmel} />
          ))}
        </HStack>
        <HStack justify="space-between" wrap={false}>
          {props.children}
        </HStack>
      </VStack>
    </Box>
  )
}
