import React from 'react'
import { Alert, Box, Button, Heading, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettTrygdetidOverstyrtMigrering } from '~shared/api/trygdetid'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const OpprettManueltOverstyrtTrygdetid = ({
  behandlingId,
  redigerbar,
}: {
  behandlingId: string
  redigerbar: boolean
}) => {
  const [opprettOverstyrtTrygdetidStatus, opprettOverstyrtTrygdetid] = useApiCall(opprettTrygdetidOverstyrtMigrering)

  const overstyrTrygdetid = () => {
    opprettOverstyrtTrygdetid({ behandlingId: behandlingId, overskriv: true }, () => window.location.reload())
  }

  return (
    <>
      {redigerbar && (
        <>
          <Heading size="small" level="3">
            Endre til behandlingen til manuelt overstyrt trygdetid
          </Heading>
          <Box maxWidth="40rem">
            <VStack gap="5">
              <Alert variant="warning">
                OBS! Dette vil overstyre trygdetiden i behandlingen. Tidligere trygdetidsperioder blir slettet og
                saksbehandler må sette ny trygdetid manuelt.
              </Alert>
              <Box maxWidth="20rem">
                <Button
                  variant="danger"
                  size="small"
                  onClick={overstyrTrygdetid}
                  loading={isPending(opprettOverstyrtTrygdetidStatus)}
                >
                  Opprett overstyrt trygdetid
                </Button>
              </Box>
            </VStack>
          </Box>
          {mapResult(opprettOverstyrtTrygdetidStatus, {
            pending: <Spinner label="Overstyrer trygdetid" />,
            error: () => <ApiErrorAlert>En feil har oppstått ved overstyring av trygdetid</ApiErrorAlert>,
          })}
        </>
      )}
    </>
  )
}
