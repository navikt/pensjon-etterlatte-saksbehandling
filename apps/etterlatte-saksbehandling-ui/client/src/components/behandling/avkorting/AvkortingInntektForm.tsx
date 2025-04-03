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
import { useForm } from 'react-hook-form'
import { IAvkortingGrunnlag, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { IBehandlingReducer, oppdaterAvkorting, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { aarFraDatoString, formaterDato, maanedFraDatoString } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { isPending } from '@reduxjs/toolkit'
import OverstyrInnvilgaMaander from '~components/behandling/avkorting/OverstyrInnvilgaMaaneder'
import React, { Dispatch, SetStateAction, useState } from 'react'
import { CogRotationIcon, PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { hentBehandlingstatus } from '~shared/api/behandling'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const AvkortingInntektForm = ({
  behandling,
  erInnevaerendeAar,
  redigerbartGrunnlag,
  historikk,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  redigerbartGrunnlag: IAvkortingGrunnlag | undefined
  historikk: IAvkortingGrunnlag[]
  erInnevaerendeAar: boolean
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  const erRedigerbar = redigerbar && enhetErSkrivbar(behandling.sakEnhetId, useInnloggetSaksbehandler().skriveEnheter)
  const [visForm, setVisForm] = useState(false)

  const knappTekst = () => {
    if (erInnevaerendeAar) {
      if (redigerbartGrunnlag != null) {
        return 'Rediger'
      }
      return 'Legg til'
    } else {
      if (redigerbartGrunnlag != null) {
        return 'Rediger for neste år'
      }
      return 'Legg til for neste år'
    }
  }

  return (
    <>
      {erRedigerbar && visForm && (
        <InntektForm
          behandling={behandling}
          redigerbartGrunnlag={redigerbartGrunnlag}
          alleGrunnlag={historikk}
          erInnevaerendeAar={erInnevaerendeAar}
          setVisForm={setVisForm}
        />
      )}
      {erRedigerbar && !visForm && (
        <HStack marginBlock="4 0">
          <Button
            size="small"
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            onClick={(e) => {
              e.preventDefault()
              setVisForm(true)
              resetInntektsavkortingValidering()
            }}
          >
            {knappTekst()}
          </Button>
        </HStack>
      )}
    </>
  )
}

const InntektForm = ({
  behandling,
  erInnevaerendeAar,
  redigerbartGrunnlag,
  alleGrunnlag,
  setVisForm,
}: {
  behandling: IBehandlingReducer
  redigerbartGrunnlag: IAvkortingGrunnlag | undefined
  alleGrunnlag: IAvkortingGrunnlag[]
  erInnevaerendeAar: boolean
  setVisForm: Dispatch<SetStateAction<boolean>>
}) => {
  const dispatch = useAppDispatch()

  const [lagreAvkortingGrunnlagResult, lagreAvkortingGrunnlagRequest] = useApiCall(lagreAvkortingGrunnlag)
  const [, hentBehandlingstatusRequest] = useApiCall(hentBehandlingstatus)

  const virk = virkningstidspunkt(behandling).dato
  const aar = erInnevaerendeAar ? aarFraDatoString(virk) : aarFraDatoString(virk) + 1
  const inntektFom = erInnevaerendeAar ? virk : `${aar}-01`

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

    if (!!redigerbartGrunnlag) {
      return redigerbartGrunnlag.innvilgaMaaneder === 12
    }
    if (behandling.revurderingsaarsak != null) {
      return alleGrunnlag[0].innvilgaMaaneder === 12
    }
    return false
  }

  /*
   * Hvis det finnes avkortingGrunnlag med fom samme som virkningstidspunkt fylles den i form med id for å kunne oppdatere.
   * Hvis det er førstegangsbehandling og avkortingGrunnlag for neste år og det allerede finens avkortingGrunnlag fylles den i form med id for å kunne oppdatere.
   * Hvis ikke så er det form uten id slik at det opprettes nytt avkortingGrunnlag. Finnes det tidligere avkortingGrunnlag i samme år
   * vil beløp preutfylles.
   */
  const finnRedigerbartGrunnlagEllerOpprettNytt = (): IAvkortingGrunnlagLagre => {
    if (!!redigerbartGrunnlag) {
      return redigerbartGrunnlag
    }

    // Inntektsendringer skjer kun frem i tid
    if (!!behandling.revurderingsaarsak && alleGrunnlag.length > 0) {
      const nyligste = alleGrunnlag[0]
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

  const {
    register,
    reset,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    getValues,
  } = useForm<IAvkortingGrunnlagLagre>({
    defaultValues: finnRedigerbartGrunnlagEllerOpprettNytt(),
  })

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
          if (nyttAvkortingGrunnlag) reset(nyttAvkortingGrunnlag)
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
    <form>
      <Heading spacing size="small" level="2">
        {aar}
      </Heading>
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
              name="inntektTom"
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
    </form>
  )
}
