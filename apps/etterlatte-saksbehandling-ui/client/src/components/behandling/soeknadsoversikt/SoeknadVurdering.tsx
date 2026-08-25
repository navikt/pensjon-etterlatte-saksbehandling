import { Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { Children, ReactNode } from 'react'
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
  const [leftContent, rightContent] = Children.toArray(props.children)

  return (
    <Box background="neutral-soft" borderRadius="12" padding="space-16">
      <VStack gap="space-8">
        <HStack gap="space-24" align="center">
          {props.status && <StatusIcon status={props.status} />}
          <Heading size="medium" level="2">
            {props.tittel}
          </Heading>
        </HStack>
        <HStack justify="space-between" wrap={false} align="start">
          <VStack gap="space-4" paddingInline="space-0 space-16">
            {props.hjemler.length > 0 && (
              <HStack gap="space-16">
                {props.hjemler.map((hjemmel, idx) => (
                  <HjemmelLenke key={`hjemmel-${idx}`} {...hjemmel} />
                ))}
              </HStack>
            )}
            {leftContent}
          </VStack>
          <div style={{ width: '35%', flexShrink: 0 }}>{rightContent}</div>
        </HStack>
      </VStack>
    </Box>
  )
}
