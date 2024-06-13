import React, { useState } from 'react'
import { Alert, Button, HStack, TextField, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterNotatTittel } from '~shared/api/notat'
import { CheckmarkIcon, PencilIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending, isSuccess } from '~shared/api/apiUtils'

export const RedigerNotatTittel = ({ id, tittel }: { id: number; tittel: string }) => {
  const [redigerer, setRedigerer] = useState(false)
  const [nyTittel, setNyTittel] = useState(tittel)

  const [oppdaterTittelStatus, apiOppdaterTittel, reset] = useApiCall(oppdaterNotatTittel)

  const oppdater = () => {
    if (tittel === nyTittel) {
      setRedigerer(false)
    } else {
      apiOppdaterTittel({ id, tittel: nyTittel }, () => {
        setRedigerer(false)
        setTimeout(reset, 3000)
      })
    }
  }

  const avbryt = () => {
    setRedigerer(false)
    setNyTittel(tittel)
  }

  return (
    <VStack gap="4">
      <HStack gap="4" justify="space-between" align="end">
        <TextField
          label="Tittel"
          value={nyTittel}
          onChange={(e) => setNyTittel(e.target.value)}
          htmlSize={100}
          readOnly={!redigerer}
        />

        {redigerer ? (
          <HStack gap="4">
            <Button variant="secondary" icon={<XMarkIcon />} onClick={avbryt} />
            <Button icon={<CheckmarkIcon />} onClick={oppdater} loading={isPending(oppdaterTittelStatus)} />
          </HStack>
        ) : (
          <Button variant="secondary" icon={<PencilIcon />} onClick={() => setRedigerer(true)} />
        )}
      </HStack>

      {isSuccess(oppdaterTittelStatus) && (
        <Alert variant="success" size="small" inline>
          Tittel lagret!
        </Alert>
      )}
    </VStack>
  )
}
