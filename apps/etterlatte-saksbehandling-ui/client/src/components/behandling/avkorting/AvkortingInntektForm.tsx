import { AvkortingFlereInntekter, IAvkortingGrunnlag, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { useAppDispatch } from '~store/Store'
import { useBehandling } from '~components/behandling/useBehandling'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreInntektListe } from '~shared/api/avkorting'
import { useFieldArray, useForm } from 'react-hook-form'
import { oppdaterAvkorting } from '~store/reducers/BehandlingReducer'
import {
  aarFraDatoString,
  formaterKanskjeStringDato,
  kanskjeMaanedFraDatoString,
  maanedFraDatoString,
} from '~utils/formatering/dato'
import {
  BodyShort,
  Box,
  Button,
  Heading,
  HelpText,
  HStack,
  Label,
  ReadMore,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import OverstyrInnvilgaMaander from '~components/behandling/avkorting/OverstyrInnvilgaMaaneder'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { CogRotationIcon, TrashIcon } from '@navikt/aksel-icons'

export function AvkortingInntektForm({
  inntekterForRedigering,
  vedLagring,
  avbrytRedigering,
  alleInntektsgrunnlag,
}: {
  inntekterForRedigering: IAvkortingGrunnlagLagre[]
  vedLagring: (inntekter: IAvkortingGrunnlagLagre[]) => void
  alleInntektsgrunnlag: IAvkortingGrunnlag[]
  avbrytRedigering?: () => void
}) {
  const dispatch = useAppDispatch()
  const behandling = useBehandling()!
  const [skalOverstyreMaaneder, setSkalOverstyreMaaneder] = useState(false)
  const [statusLagreInntektListe, fetchLagreInntektListe] = useApiCall(lagreInntektListe)
  const {
    register,
    watch,
    control,
    handleSubmit,
    reset,
    formState: { errors },
    setValue,
  } = useForm<AvkortingFlereInntekter>({
    defaultValues: {
      inntekter: inntekterForRedigering,
    },
  })
  const { fields } = useFieldArray({ control, name: 'inntekter' })

  useEffect(() => {
    reset({
      inntekter: inntekterForRedigering,
    })
  }, [inntekterForRedigering])

  function lagreInntekter(formdata: AvkortingFlereInntekter) {
    fetchLagreInntektListe(
      {
        behandlingId: behandling.id,
        inntekter: formdata.inntekter,
      },
      (avkorting) => {
        dispatch(oppdaterAvkorting(avkorting))
        vedLagring(formdata.inntekter)
      }
    )
  }
  function toggleOverstyrMaaneder() {
    setSkalOverstyreMaaneder((verdi) => !verdi)
    setValue(`inntekter.${inntekterForRedigering.length - 1}.overstyrtInnvilgaMaaneder`, undefined)
  }

  return (
    <form onSubmit={handleSubmit(lagreInntekter)}>
      {fields.map((item, index) => {
        const inntektsAar = !!item.fom && aarFraDatoString(item.fom)
        const inntektGjelderFraStartAvAaret = kanskjeMaanedFraDatoString(item.fom) === 0

        // Vi har ikke fratrekk innår hvis inntekten gjelder fra januar, eller vi allerede har inntekt lagt inn
        // som gjelder for januar dette året
        const harIkkeFratrekkInnaar =
          inntektGjelderFraStartAvAaret ||
          alleInntektsgrunnlag.some(
            (inntekt) => aarFraDatoString(inntekt.fom) === inntektsAar && maanedFraDatoString(inntekt.fom) === 0
          )
        const kanOverstyreInnvilgedeMaaneder = index === inntekterForRedigering.length - 1

        return (
          <React.Fragment key={item.id}>
            <Heading spacing size="small" level="3">
              Inntekt for {inntektsAar}
            </Heading>
            <VStack>
              <HStack marginBlock="space-0 space-8" gap="space-2" align="start" wrap={false}>
                <Box maxWidth="14rem">
                  <TextField
                    {...register(`inntekter.${index}.inntektTom`, {
                      pattern: { value: /^\d+$/, message: 'Kun tall' },
                      required: { value: true, message: 'Må fylles ut' },
                    })}
                    label={
                      <HStack>
                        Forventet inntekt Norge
                        <HelpText title="Hva innebærer forventet inntekt totalt">
                          Registrer forventet norsk inntekt for det aktuelle året (jan-des). Hvis opphør er kjent for
                          dette året, registrer forventet inntekt fra januar til opphørsdato, og sjekk at innvilgede
                          måneder stemmer.
                        </HelpText>
                      </HStack>
                    }
                    error={errors.inntekter?.[index]?.inntektTom?.message}
                  />
                </Box>
                <Box maxWidth="14rem">
                  <TextField
                    {...register(`inntekter.${index}.fratrekkInnAar`, {
                      required: { value: !harIkkeFratrekkInnaar, message: 'Må fylles ut' },
                      max: {
                        value: watch(`inntekter.${index}.inntektTom`) || 0,
                        message: 'Kan ikke være høyere enn årsinntekt',
                      },
                      pattern: { value: /^\d+$/, message: 'Kun tall' },
                    })}
                    label="Fratrekk inn-år"
                    size="medium"
                    type="tel"
                    inputMode="numeric"
                    disabled={harIkkeFratrekkInnaar}
                    defaultValue={harIkkeFratrekkInnaar ? 0 : ''}
                    error={errors.inntekter?.[index]?.fratrekkInnAar?.message}
                  />
                </Box>
                <Box maxWidth="14rem">
                  <TextField
                    {...register(`inntekter.${index}.inntektUtlandTom`, {
                      required: { value: true, message: 'Må fylles ut' },
                      pattern: { value: /^\d+$/, message: 'Kun tall' },
                    })}
                    label={
                      <HStack>
                        Forventet inntekt utland
                        <HelpText title="Hva innebærer forventet inntekt totalt">
                          Registrer forventet utenlandsk inntekt for det aktuelle året (jan-des). Hvis opphør er kjent
                          for dette året, registrer forventet inntekt fra januar til opphørsdato, og sjekk at innvilgede
                          måneder stemmer.
                        </HelpText>
                      </HStack>
                    }
                    size="medium"
                    type="tel"
                    inputMode="numeric"
                    error={errors.inntekter?.[index]?.inntektUtlandTom?.message}
                  />
                </Box>
                <Box maxWidth="14rem">
                  <TextField
                    {...register(`inntekter.${index}.fratrekkInnAarUtland`, {
                      required: { value: !harIkkeFratrekkInnaar, message: 'Må fylles ut' },
                      max: {
                        value: watch(`inntekter.${index}.inntektUtlandTom`) || 0,
                        message: 'Kan ikke være høyere enn årsinntekt utland',
                      },
                      pattern: { value: /^\d+$/, message: 'Kun tall' },
                    })}
                    label="Fratrekk inn-år"
                    size="medium"
                    type="tel"
                    disabled={harIkkeFratrekkInnaar}
                    defaultValue={harIkkeFratrekkInnaar ? 0 : ''}
                    inputMode="numeric"
                    error={errors.inntekter?.[index]?.fratrekkInnAarUtland?.message}
                  />
                </Box>
                <VStack gap="space-4">
                  <Label>Fra og med dato</Label>
                  <BodyShort>{formaterKanskjeStringDato(item.fom)}</BodyShort>
                </VStack>
              </HStack>
              <Box width="39rem">
                <Textarea
                  {...register(`inntekter.${index}.spesifikasjon`)}
                  resize="vertical"
                  label="Spesifikasjon av inntekt"
                />
              </Box>

              <VStack marginBlock="space-2" gap="space-1">
                <ReadMore header="Hva regnes som inntekt?">
                  Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt med
                  arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre nærståendes
                  sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, svangerskapspenger og foreldrepenger etter
                  kap 14 og pensjonsytelser etter AFP tilskottloven kapitlene 2 og 3.
                </ReadMore>
                {kanOverstyreInnvilgedeMaaneder && (
                  <ReadMore header="Når du skal overstyre innvilga måneder">
                    Fyll inn riktig antall måneder med innvilget stønad i tilfeller der automatisk registrerte
                    innvilgede måneder ikke stemmer, for eksempel ved uforutsette opphør som tidlig uttak av
                    alderspensjon.
                  </ReadMore>
                )}
              </VStack>

              {skalOverstyreMaaneder && kanOverstyreInnvilgedeMaaneder && (
                <OverstyrInnvilgaMaander register={register} watch={watch} errors={errors} index={index} />
              )}
            </VStack>
          </React.Fragment>
        )
      })}
      {mapFailure(statusLagreInntektListe, (error) => (
        <ApiErrorAlert>Kunne ikke lagre inntekt(er), på grunn av feil: {error.detail}</ApiErrorAlert>
      ))}
      <HStack gap="space-2" marginBlock="space-4">
        <Button size="medium" type="submit" loading={isPending(statusLagreInntektListe)}>
          Lagre
        </Button>
        <Button
          size="medium"
          variant="secondary"
          type="button"
          onClick={toggleOverstyrMaaneder}
          disabled={isPending(statusLagreInntektListe)}
          icon={skalOverstyreMaaneder ? <TrashIcon aria-hidden /> : <CogRotationIcon aria-hidden />}
        >
          {skalOverstyreMaaneder ? 'Fjern overstyrt innvilga måneder' : 'Overstyr innvilga måneder'}
        </Button>
        {avbrytRedigering && (
          <Button
            type="button"
            variant="tertiary"
            disabled={isPending(statusLagreInntektListe)}
            onClick={avbrytRedigering}
          >
            Avbryt
          </Button>
        )}
      </HStack>
    </form>
  )
}
