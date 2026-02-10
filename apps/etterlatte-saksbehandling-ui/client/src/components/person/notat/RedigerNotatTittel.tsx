import React, { useEffect, useState } from 'react'
import { Alert, HStack, TextField } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterNotatTittel } from '~shared/api/notat'
import { isSuccess } from '~shared/api/apiUtils'

export const RedigerNotatTittel = ({ id, tittel }: { id: number; tittel: string }) => {
  const [nyTittel, setNyTittel] = useState(tittel)

  const [oppdaterTittelStatus, apiOppdaterTittel, reset] = useApiCall(oppdaterNotatTittel)

  const oppdater = () => {
    if (tittel !== nyTittel) {
      apiOppdaterTittel({ id, tittel: nyTittel }, () => {
        setTimeout(reset, 3000)
      })
    }
  }

  useEffect(() => {
    const delay = setTimeout(oppdater, 1000)
    return () => clearTimeout(delay)
  }, [nyTittel])

  return (
    <HStack gap="space-4" align="center">
      <TextField label="Tittel" value={nyTittel} onChange={(e) => setNyTittel(e.target.value)} htmlSize={100} />
      {isSuccess(oppdaterTittelStatus) && (
        <Alert variant="success" size="small" inline>
          Tittel lagret!
        </Alert>
      )}
    </HStack>
  )
}
