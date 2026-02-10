import { Heading, HStack, VStack } from '@navikt/ds-react'
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
    <VStack gap="space-2" paddingBlock="space-12">
      <HStack gap="space-6" align="center">
        {props.status && <StatusIcon status={props.status} />}
        <Heading size="medium" level="2">
          {props.tittel}
        </Heading>
      </HStack>
      <HStack gap="space-4">
        {props.hjemler.map((hjemmel, idx) => (
          <HjemmelLenke key={`hjemmel-${idx}`} {...hjemmel} />
        ))}
      </HStack>

      <HStack justify="space-between" wrap={false}>
        {props.children}
      </HStack>
    </VStack>
  )
}
