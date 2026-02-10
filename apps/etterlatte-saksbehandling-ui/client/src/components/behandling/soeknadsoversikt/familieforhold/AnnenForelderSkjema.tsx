import { FloppydiskIcon, PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { BodyShort, Box, Button, Checkbox, Heading, HStack, ReadMore, Textarea, VStack } from '@navikt/ds-react'
import {
  AnnenForelder,
  AnnenForelderVurdering,
  Personopplysninger,
  teksterAnnenForelderVurdering,
} from '~shared/types/grunnlag'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { isPending } from '~shared/api/apiUtils'
import { redigerAnnenForelder, slettAnnenForelder } from '~shared/api/behandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { BP_BEREGNING_AV_BARNEPENSJON } from '~components/behandling/virkningstidspunkt/utils'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

type Props = {
  behandlingId: string
  personopplysninger: Personopplysninger
}

export const AnnenForelderSkjema = ({ behandlingId, personopplysninger }: Props) => {
  const [redigerModus, setRedigerModus] = useState<boolean>(false)
  const [redigerStatus, redigerAnnenForelderRequest] = useApiCall(redigerAnnenForelder)
  const [slettStatus, slettAnnenForelderRequest] = useApiCall(slettAnnenForelder)

  const {
    register,
    reset,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm<AnnenForelder>({
    defaultValues: personopplysninger?.annenForelder ?? { vurdering: null },
  })

  const onAvbryt = () => {
    reset()
    setRedigerModus(false)
  }

  const onLagreAnnenForelder = (annenForelder: AnnenForelder) => {
    redigerAnnenForelderRequest(
      {
        behandlingId: behandlingId,
        annenForelder: annenForelder,
      },
      () => {
        setTimeout(() => window.location.reload(), 1000)
      }
    )
  }

  const onSlettAnnenForelder = () => {
    slettAnnenForelderRequest(
      {
        behandlingId: behandlingId,
      },
      () => {
        setTimeout(() => window.location.reload(), 1000)
      }
    )
  }

  const tekstAnnenForelderVurdering = (vurdering: AnnenForelderVurdering | null) => {
    return vurdering && teksterAnnenForelderVurdering[vurdering]
  }

  return (
    <Box paddingBlock="space-4 space-0" maxWidth="25rem">
      <Heading size="small" level="3">
        Annen forelder
      </Heading>
      {!redigerModus && (
        <>
          {personopplysninger.annenForelder == null ? (
            <Button variant="secondary" onClick={() => setRedigerModus(true)}>
              Legg til vurdering
            </Button>
          ) : (
            <>
              {personopplysninger.annenForelder?.vurdering && (
                <Box paddingBlock="space-2 space-4">
                  <Heading size="xsmall" level="4">
                    Vurdering
                  </Heading>
                  <BodyShort>{teksterAnnenForelderVurdering[personopplysninger.annenForelder?.vurdering]}</BodyShort>
                </Box>
              )}
              {personopplysninger.annenForelder?.begrunnelse && (
                <Box paddingBlock="space-0 space-4">
                  <Heading size="xsmall" level="4">
                    Begrunnelse
                  </Heading>
                  <BodyShort>{personopplysninger.annenForelder?.begrunnelse}</BodyShort>
                </Box>
              )}
            </>
          )}
          {personopplysninger.annenForelder && (
            <>
              <Button
                type="button"
                variant="tertiary"
                size="small"
                icon={<PencilIcon aria-hidden />}
                disabled={redigerModus}
                onClick={() => setRedigerModus(true)}
              >
                Rediger
              </Button>
              <Button
                type="button"
                variant="tertiary"
                size="small"
                icon={<TrashIcon aria-hidden />}
                disabled={redigerModus}
                loading={isPending(slettStatus)}
                onClick={onSlettAnnenForelder}
              >
                Slett
              </Button>
            </>
          )}
        </>
      )}
      {redigerModus && (
        <form onSubmit={handleSubmit(onLagreAnnenForelder)}>
          <VStack gap="space-2">
            <Box>
              <Checkbox
                value={AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER}
                {...register('vurdering')}
                readOnly={!!watch('vurdering')}
                required={true}
              >
                {tekstAnnenForelderVurdering(AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER)}
              </Checkbox>
              <ReadMore header="Når skal du bruke Kun én registrert juridisk forelder?">
                Kryss av her hvis det er fastsatt kun én juridisk forelder (f.eks. assistert befruktning) og hen er død.
                Det gis da barnepensjon som foreldreløs. Dokumentasjon må foreligge. Ikke kryss av her hvis en eller
                begge foreldrene er ukjent (f.eks. at det ikke kan dokumenteres hvem som er mor eller far, og om hen er
                død).
                <HjemmelLenke {...BP_BEREGNING_AV_BARNEPENSJON} />
              </ReadMore>
            </Box>
            <Textarea
              {...register('begrunnelse', {
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Begrunnelse"
              error={errors.begrunnelse?.message}
            />
          </VStack>
          <HStack gap="space-4" paddingBlock="space-2 space-0">
            <Button size="small" icon={<FloppydiskIcon aria-hidden />} loading={isPending(redigerStatus)}>
              Lagre
            </Button>
            <Button size="small" variant="secondary" onClick={onAvbryt}>
              Avbryt
            </Button>
          </HStack>
        </form>
      )}
    </Box>
  )
}
