import {
  BodyShort,
  Box,
  Button,
  HelpText,
  HStack,
  Label,
  ReadMore,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import { FormProvider, useForm } from 'react-hook-form'
import { IAvkortingGrunnlagFrontend, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { IBehandlingReducer, oppdaterAvkorting, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { aarFraDatoString, formaterDato, maanedFraDatoString } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { isPending } from '@reduxjs/toolkit'
import OverstyrInnvilgaMaander from '~components/behandling/avkorting/OverstyrInnvilgaMaaneder'
import React, { useState } from 'react'
import { CogRotationIcon, TrashIcon } from '@navikt/aksel-icons'
import { hentBehandlingstatus } from '~shared/api/behandling'

export const AvkortingInntektForm = ({
  behandling,
  erInnevaerendeAar,
  avkortingGrunnlagFrontend,
  setVisForm,
}: {
  behandling: IBehandlingReducer
  avkortingGrunnlagFrontend: IAvkortingGrunnlagFrontend | undefined
  erInnevaerendeAar: boolean
  setVisForm: (visForm: boolean) => void
}) => {
  const dispatch = useAppDispatch()

  const [lagreAvkortingGrunnlagResult, lagreAvkortingGrunnlagRequest] = useApiCall(lagreAvkortingGrunnlag)
  const [, hentBehandlingstatusRequest] = useApiCall(hentBehandlingstatus)

  const virk = virkningstidspunkt(behandling).dato
  const inntektFom = erInnevaerendeAar ? virk : `${aarFraDatoString(virk) + 1}-01`

  /*
   * Utlede om opptjent før innvilgelse er relevant.
   * Hvis alle måneder i året er innvilget er det ikke det.
   * Det er kun innvilgelsesåret som kan være færre enn 12 måneder.
   * Vi tar ikke stilling til opphør.
   */
  const alleMaanederIAaretErInnvilget = () => {
    if (!erInnevaerendeAar) {
      return true
    }

    const fomJanuar = maanedFraDatoString(inntektFom) === 0
    if (fomJanuar) {
      return true
    }

    const tidligereAvkortingGrunnlag = avkortingGrunnlagFrontend?.fraVirk ?? avkortingGrunnlagFrontend?.historikk[0]
    return tidligereAvkortingGrunnlag ? tidligereAvkortingGrunnlag.innvilgaMaaneder === 12 : false
  }

  /*
   * Hvis det finnes avkortingGrunnlag med fom samme som virkningstidspunkt fylles den i form med id for å kunne oppdatere.
   * Hvis det er førstegangsbehandling og avkortingGrunnlag for neste år og det allerede finens avkortingGrunnlag fylles den i form med id for å kunne oppdatere.
   * Hvis ikke så er det form uten id slik at det opprettes nytt avkortingGrunnlag. Finnes det tidligere avkortingGrunnlag i samme år
   * vil beløp preutfylles.
   */
  const finnRedigerbartGrunnlagEllerOpprettNytt = (): IAvkortingGrunnlagLagre => {
    if (avkortingGrunnlagFrontend?.fraVirk != null) {
      return avkortingGrunnlagFrontend.fraVirk
    }
    if (!erInnevaerendeAar) {
      const grunnlagNesteAar = avkortingGrunnlagFrontend?.historikk[0]
      if (grunnlagNesteAar !== undefined) {
        return grunnlagNesteAar
      }
    }
    if (avkortingGrunnlagFrontend && avkortingGrunnlagFrontend.historikk.length > 0) {
      const nyligste = avkortingGrunnlagFrontend.historikk[0]
      // Preutfyller uten id
      return {
        inntektTom: nyligste.inntektTom,
        fratrekkInnAar: nyligste.fratrekkInnAar,
        inntektUtlandTom: nyligste.inntektUtlandTom,
        fratrekkInnAarUtland: nyligste.fratrekkInnAarUtland,
        spesifikasjon: '',
        overstyrtInnvilgaMaaneder: nyligste.overstyrtInnvilgaMaaneder,
      }
    }
    return {
      spesifikasjon: '',
    }
  }

  const methods = useForm<IAvkortingGrunnlagLagre>({
    defaultValues: finnRedigerbartGrunnlagEllerOpprettNytt(),
  })
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    getValues,
  } = methods

  const onSubmit = (data: IAvkortingGrunnlagLagre) => {
    lagreAvkortingGrunnlagRequest(
      {
        behandlingId: behandling.id,
        avkortingGrunnlag: {
          ...data,
          fratrekkInnAar: data.fratrekkInnAar ?? 0,
          fratrekkInnAarUtland: data.fratrekkInnAarUtland ?? 0,
          fom: inntektFom,
        },
      },
      (nyAvkorting) => {
        hentBehandlingstatusRequest(behandling.id, (status) => {
          dispatch(oppdaterBehandlingsstatus(status))
          const nyttAvkortingGrunnlag = nyAvkorting.avkortingGrunnlag[nyAvkorting.avkortingGrunnlag.length - 1]
          if (nyttAvkortingGrunnlag.fraVirk) reset(nyttAvkortingGrunnlag.fraVirk)
          dispatch(oppdaterAvkorting(nyAvkorting))
          setVisForm(false)
        })
      }
    )
  }

  const [skalOverstyreMaaneder, setSkalOverstyreMaaneder] = useState(!!getValues('overstyrtInnvilgaMaaneder'))
  const toggleOverstyrtInnvilgaMaaneder = () => {
    if (skalOverstyreMaaneder) {
      setValue('overstyrtInnvilgaMaaneder', undefined)
    }
    setSkalOverstyreMaaneder(!skalOverstyreMaaneder)
  }

  return (
    <FormProvider {...methods}>
      <VStack>
        <HStack marginBlock="8" gap="2" align="start" wrap={false}>
          <Box maxWidth="14rem">
            <TextField
              {...register('inntektTom', {
                pattern: { value: /^\d+$/, message: 'Kun tall' },
                required: { value: true, message: 'Må fylles ut' },
              })}
              label={
                <HStack>
                  Forventet inntekt Norge
                  <HelpText title="Hva innebærer forventet inntekt totalt">
                    Registrer forventet norsk inntekt for det aktuelle året (jan-des). Hvis opphør er kjent for dette
                    året, registrer forventet inntekt fra januar til opphørsdato, og sjekk at innvilgede måneder
                    stemmer.
                  </HelpText>
                </HStack>
              }
              size="medium"
              type="tel"
              inputMode="numeric"
              error={errors.inntektTom?.message}
            />
          </Box>
          <Box maxWidth="14rem">
            <TextField
              {...register('fratrekkInnAar', {
                required: { value: !alleMaanederIAaretErInnvilget(), message: 'Må fylles ut' },
                max: {
                  value: watch('inntektTom') || 0,
                  message: 'Kan ikke være høyere enn årsinntekt',
                },
                pattern: { value: /^\d+$/, message: 'Kun tall' },
              })}
              label="Fratrekk inn-år"
              size="medium"
              type="tel"
              inputMode="numeric"
              disabled={alleMaanederIAaretErInnvilget()}
              defaultValue={alleMaanederIAaretErInnvilget() ? 0 : undefined}
              error={errors.fratrekkInnAar?.message}
            />
          </Box>
          <Box maxWidth="14rem">
            <TextField
              {...register('inntektUtlandTom', {
                required: { value: true, message: 'Må fylles ut' },
                pattern: { value: /^\d+$/, message: 'Kun tall' },
              })}
              label={
                <HStack>
                  Forventet inntekt utland
                  <HelpText title="Hva innebærer forventet inntekt totalt">
                    Registrer forventet utenlandsk inntekt for det aktuelle året (jan-des). Hvis opphør er kjent for
                    dette året, registrer forventet inntekt fra januar til opphørsdato, og sjekk at innvilgede måneder
                    stemmer.
                  </HelpText>
                </HStack>
              }
              size="medium"
              type="tel"
              inputMode="numeric"
              error={errors.inntektUtlandTom?.message}
            />
          </Box>
          <Box maxWidth="14rem">
            <TextField
              {...register('fratrekkInnAarUtland', {
                required: { value: !alleMaanederIAaretErInnvilget(), message: 'Må fylles ut' },
                max: {
                  value: watch('inntektUtlandTom') || 0,
                  message: 'Kan ikke være høyere enn årsinntekt utland',
                },
                pattern: { value: /^\d+$/, message: 'Kun tall' },
              })}
              label="Fratrekk inn-år"
              size="medium"
              type="tel"
              disabled={alleMaanederIAaretErInnvilget()}
              defaultValue={alleMaanederIAaretErInnvilget() ? 0 : undefined}
              inputMode="numeric"
              error={errors.fratrekkInnAarUtland?.message}
            />
          </Box>
          <VStack gap="4">
            <Label>Fra og med dato</Label>
            <BodyShort>{formaterDato(inntektFom)}</BodyShort>
          </VStack>
        </HStack>
        <Box width="39rem">
          <Textarea {...register('spesifikasjon')} resize="vertical" label="Spesifikasjon av inntekt" />
        </Box>

        <VStack marginBlock="2" gap="1">
          <ReadMore header="Hva regnes som inntekt?">
            Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt med
            arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre nærståendes
            sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, svangerskapspenger og foreldrepenger etter kap 14
            og pensjonsytelser etter AFP tilskottloven kapitlene 2 og 3.
          </ReadMore>
          <ReadMore header="Når du skal overstyre innvilga måneder">
            Fyll inn riktig antall måneder med innvilget stønad i tilfeller der automatisk registrerte innvilgede
            måneder ikke stemmer, for eksempel ved uforutsette opphør som tidlig uttak av alderspensjon.
          </ReadMore>
        </VStack>

        {skalOverstyreMaaneder && <OverstyrInnvilgaMaander />}
        <HStack gap="3" marginBlock="4">
          <Button size="medium" loading={isPending(lagreAvkortingGrunnlagResult)} onClick={handleSubmit(onSubmit)}>
            Lagre
          </Button>
          <Button
            size="medium"
            variant="secondary"
            onClick={toggleOverstyrtInnvilgaMaaneder}
            icon={skalOverstyreMaaneder ? <TrashIcon aria-hidden /> : <CogRotationIcon aria-hidden />}
          >
            {skalOverstyreMaaneder ? 'Fjern overstyrt innvilga måneder' : 'Overstyr innvilga måneder'}
          </Button>
          <Button
            size="medium"
            variant="tertiary"
            onClick={() => {
              setVisForm(false)
            }}
          >
            Avbryt
          </Button>
        </HStack>
      </VStack>
      {isFailureHandler({
        apiResult: lagreAvkortingGrunnlagResult,
        errorMessage: 'En feil har oppstått',
      })}
    </FormProvider>
  )
}
