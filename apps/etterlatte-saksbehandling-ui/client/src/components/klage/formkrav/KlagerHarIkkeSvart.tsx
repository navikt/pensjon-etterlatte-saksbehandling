import { erKlageRedigerbar, Klage } from '~shared/types/Klage'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreKlagerIkkeSvart } from '~shared/api/klage'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { addKlage } from '~store/reducers/KlageReducer'
import { BodyShort, Button, Heading, Label, Link, Textarea, VStack } from '@navikt/ds-react'
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
  const vurdering = watch('begrunnelse') //klage.formkrav?.klagerHarIkkeSvart //|| { begrunnelse: 'De svarte ikke' }

  if (!redigerbar && !vurdering) {
    return null
  }

  function lagreVurdering(formdata: KlagerHarIkkeSvartBegrunnelse) {
    klagerIkkeSvartFetch(
      {
        klageId: klage.id,
        begrunnelse: formdata.begrunnelse,
      },
      (klage) => dispatch(addKlage(klage))
    )
  }

  return (
    <VStack gap="2">
      <Heading level="3" size="small">
        Klager har ikke kommet med nødvendig informasjon
      </Heading>
      <BodyShort>Hvis klager ikke kommer med nødvendig informasjon, skal klagen avvises.</BodyShort>
      {!!vurdering && !open && (
        <>
          <Label>Begrunnelse</Label>
          <BodyShort>{vurdering}</BodyShort>
          <Link as={Button} href={`/klage/${klage.id}/vurdering`}>
            Avvis klage
          </Link>
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
              <Textarea
                {...register('begrunnelse')}
                label="Begrunnelse"
                description="Begrunn hvorfor klagen skal avvises"
              />
              <div>
                <Button icon={<FileIcon />} type="submit" size="small" loading={isPending(klagerIkkeSvartResult)}>
                  Lagre vurdering
                </Button>
                <Button onClick={() => setOpen(false)} type="button" disabled={isPending(klagerIkkeSvartResult)}>
                  Avbryt
                </Button>
              </div>
            </form>
          )}
        </>
      )}
    </VStack>
  )
}
