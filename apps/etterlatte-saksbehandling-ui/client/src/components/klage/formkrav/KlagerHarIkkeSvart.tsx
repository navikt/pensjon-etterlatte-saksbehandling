import { erKlageRedigerbar, Klage } from '~shared/types/Klage'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreKlagerIkkeSvart } from '~shared/api/klage'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { addKlage } from '~store/reducers/KlageReducer'
import { BodyShort, Button, Heading, HStack, Label, Textarea, VStack } from '@navikt/ds-react'
import { FileIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'

interface KlagerHarIkkeSvartBegrunnelse {
  begrunnelse: string
}

export function KlagerHarIkkeSvart(props: { klage: Klage }) {
  const { klage } = props
  const dispatch = useAppDispatch()
  const [klagerIkkeSvartResult, klagerIkkeSvartFetch] = useApiCall(lagreKlagerIkkeSvart)
  const [open, setOpen] = useState(false)
  const redigerbar = erKlageRedigerbar(klage)
  const { watch, register, handleSubmit } = useForm<KlagerHarIkkeSvartBegrunnelse>({
    defaultValues: {
      begrunnelse: klage.formkrav?.klagerHarIkkeSvartVurdering?.begrunnelse || '',
    },
  })
  const vurdering = watch('begrunnelse')

  if (!redigerbar && !vurdering) {
    return null
  }

  function lagreVurdering(formdata: KlagerHarIkkeSvartBegrunnelse) {
    klagerIkkeSvartFetch(
      {
        klageId: klage.id,
        begrunnelse: formdata.begrunnelse,
      },
      (klage) => {
        setOpen(false)
        dispatch(addKlage(klage))
      }
    )
  }

  return (
    <VStack gap="space-2">
      <Heading level="3" size="small">
        Klager har ikke kommet med nødvendig informasjon
      </Heading>
      <BodyShort>Dersom nødvendig informasjon ikke kommer fra klager innen fristen, kan klagen avvises.</BodyShort>
      {!!vurdering && !open && (
        <>
          <Label>Begrunnelse</Label>
          <BodyShort>{vurdering}</BodyShort>
        </>
      )}
      {redigerbar && (
        <>
          {!open ? (
            <div>
              <Button size="small" variant="secondary" onClick={() => setOpen(true)}>
                {!!vurdering ? 'Endre vurdering' : 'Legg til vurdering'}
              </Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit(lagreVurdering)}>
              <VStack gap="space-4">
                <Textarea
                  {...register('begrunnelse')}
                  label="Begrunnelse"
                  description="Beskriv når informasjonbrev ble sendt og når svarfristen utløpte."
                />
                <HStack gap="space-4">
                  <Button icon={<FileIcon />} type="submit" size="small" loading={isPending(klagerIkkeSvartResult)}>
                    Lagre vurdering
                  </Button>
                  <Button
                    onClick={() => setOpen(false)}
                    size="small"
                    variant="secondary"
                    type="button"
                    disabled={isPending(klagerIkkeSvartResult)}
                  >
                    Avbryt
                  </Button>
                </HStack>
              </VStack>
            </form>
          )}
        </>
      )}
    </VStack>
  )
}
