import { Box, Button, Heading, HStack, Label, TextField, VStack } from '@navikt/ds-react'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useState } from 'react'
import { SakType } from '~shared/types/sak'
import { EksportvurderingReadMore } from '~components/behandling/trygdetid/components/EksportvurderingReadMore'
import { useForm } from 'react-hook-form'
import { FloppydiskIcon, PencilIcon, PlusIcon, TrashIcon, TruckIcon, XMarkIcon } from '@navikt/aksel-icons'

interface Props {
  redigerbar: boolean
  sakType: SakType
  trygdetid: ITrygdetid
  virkningstidspunktEtterNyRegelDato: boolean
  overstyrTrygdetidPoengaar: (trygdetid: ITrygdetid) => void
}

export const OverstyrtTrygdetid = ({
  redigerbar,
  sakType,
  trygdetid,
  virkningstidspunktEtterNyRegelDato,
  overstyrTrygdetidPoengaar,
}: Props) => {
  const {
    register,
    getValues,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm<{ overstyrtNorskPoengaar: number | undefined }>({
    defaultValues: {
      overstyrtNorskPoengaar: trygdetid.overstyrtNorskPoengaar,
    },
  })

  const [redigerNorskPoengaar, setRedigerNorskPoengaar] = useState<boolean>(false)

  return (
    <form
      onSubmit={handleSubmit((data) => {
        overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: data.overstyrtNorskPoengaar })
        setRedigerNorskPoengaar(false)
      })}
    >
      <VStack gap="4">
        <HStack gap="2">
          <TruckIcon aria-hidden height="1.5rem" width="1.5rem" />
          <Heading size="small" level="3">
            Poengår i Norge - registreres kun ved eksportvurdering
          </Heading>
        </HStack>
        <EksportvurderingReadMore
          sakType={sakType}
          virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
        />
        {!redigerNorskPoengaar && (
          <>
            <Label>
              {getValues().overstyrtNorskPoengaar
                ? `Poengår: ${getValues().overstyrtNorskPoengaar} år`
                : 'Poengår ikke satt'}
            </Label>
            {redigerbar && (
              <HStack gap="4">
                <Button
                  type="button"
                  variant="secondary"
                  size="small"
                  icon={getValues().overstyrtNorskPoengaar ? <PencilIcon aria-hidden /> : <PlusIcon aria-hidden />}
                  onClick={() => setRedigerNorskPoengaar(true)}
                >
                  {getValues().overstyrtNorskPoengaar ? 'Rediger' : 'Legg til'}
                </Button>
                {getValues().overstyrtNorskPoengaar && (
                  <Button
                    type="button"
                    variant="danger"
                    size="small"
                    icon={<TrashIcon aria-hidden />}
                    onClick={() => {
                      reset({ overstyrtNorskPoengaar: undefined })
                      overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: undefined })
                    }}
                  >
                    Slett
                  </Button>
                )}
              </HStack>
            )}
          </>
        )}
        {redigerbar && redigerNorskPoengaar && (
          <>
            <Box width="15rem">
              <TextField
                {...register('overstyrtNorskPoengaar', { valueAsNumber: true })}
                inputMode="numeric"
                label="Antall år"
                error={errors.overstyrtNorskPoengaar?.message}
              />
            </Box>
            <HStack gap="4">
              <Button
                type="button"
                variant="secondary"
                size="small"
                icon={<XMarkIcon aria-hidden />}
                onClick={() => {
                  setRedigerNorskPoengaar(false)
                  reset()
                }}
              >
                Avbryt
              </Button>
              <Button size="small" icon={<FloppydiskIcon aria-hidden />}>
                Lagre
              </Button>
            </HStack>
          </>
        )}
      </VStack>
    </form>
  )
}
