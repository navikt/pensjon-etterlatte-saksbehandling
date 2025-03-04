import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Box, Heading } from '@navikt/ds-react'

export const EtteroppgjoerOversikt = (props: { behandling: IDetaljertBehandling }) => {
  const behandling = props.behandling
  return (
    <>
      <Box paddingInline="16" paddingBlock="12 4">
        <Heading spacing size="large" level="1">
          Etteroppgjør
        </Heading>
      </Box>
      <Box paddingInline="16" paddingBlock="12 4">
        TODO {behandling.id}
      </Box>
    </>
  )
}
