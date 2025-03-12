import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreFaktiskInntekt } from '~shared/api/etteroppgjoer'
import { useForm } from 'react-hook-form'
import { FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { BodyShort, Button, Heading, HStack, TextField, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { isPending } from '@reduxjs/toolkit'

export function FastsetteInntekt({ forbehandlingId }: { forbehandlingId: string }) {
  const [lagreFaktiskInntektResult, lagreFaktiskInntektRequest] = useApiCall(lagreFaktiskInntekt)

  const { watch, register, handleSubmit } = useForm<FaktiskInntekt>()

  const onSubmit = (data: FaktiskInntekt) => {
    const faktiskInntekt = {
      loennsinntekt: data.loennsinntekt,
      afp: data.afp,
      naeringsinntekt: data.naeringsinntekt,
      utland: data.utland,
    }
    lagreFaktiskInntektRequest(
      {
        forbehandlingId: forbehandlingId,
        faktiskInntekt: faktiskInntekt,
      },
      () => {}
    )
  }

  return (
    <>
      <Heading size="medium" level="2">
        Fastsette inntekt
      </Heading>
      <BodyShort>Her skal du fastsette faktisk inntekt for bruker i innvilget periode (april - desember)</BodyShort>
      <VStack width="fit-content" gap="4">
        <TextField {...register('loennsinntekt')} label="Lønnsinntekt (eksl. OMS)"></TextField>
        <TextField {...register('afp')} label="AFP"></TextField>
        <TextField {...register('naeringsinntekt')} label="Næringsinntekt"></TextField>
        <TextField {...register('utland')} label="Utlandsinntekt"></TextField>
      </VStack>

      <Heading size="small" level="3">
        Sum:
      </Heading>
      <BodyShort>
        {NOK(+watch('loennsinntekt') + +watch('afp') + +watch('naeringsinntekt') + +watch('utland'))}
      </BodyShort>
      <HStack width="fit-content">
        <Button onClick={handleSubmit(onSubmit)} loading={isPending(lagreFaktiskInntektResult)}>
          Lagre
        </Button>
      </HStack>
    </>
  )
}
