import { DocPencilIcon } from '@navikt/aksel-icons'
import { Box, Button } from '@navikt/ds-react'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Notat, NotatMal, opprettNotatForSak } from '~shared/api/notat'
import { isPending } from '~shared/api/apiUtils'

/**
 * Modal blir lagt tilbake når andre maler er på plass.
 * Enkel løsning med umiddelbar opprettelse midlertidig.
 **/
export const NyttNotatModal = ({ sakId, leggTilNotat }: { sakId: number; leggTilNotat: (notat: Notat) => void }) => {
  const [opprettNotatStatus, opprettNotat] = useApiCall(opprettNotatForSak)

  const opprettNyttNotat = () => {
    opprettNotat({ sakId, mal: NotatMal.TOM_MAL }, (notat) => {
      leggTilNotat(notat)
    })
  }

  return (
    <Box>
      <Button
        variant="primary"
        icon={<DocPencilIcon />}
        iconPosition="right"
        onClick={opprettNyttNotat}
        loading={isPending(opprettNotatStatus)}
      >
        Nytt notat
      </Button>
    </Box>
  )
}
