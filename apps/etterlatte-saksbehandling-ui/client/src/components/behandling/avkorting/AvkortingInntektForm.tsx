import styled from 'styled-components'
import { BodyShort, Button, HStack, Label, ReadMore, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { IAvkortingGrunnlagFrontend, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { IBehandlingStatus, virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { IBehandlingReducer, oppdaterAvkorting, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { aarFraDatoString, formaterDato, maanedFraDatoString } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { isPending } from '@reduxjs/toolkit'

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
    return tidligereAvkortingGrunnlag ? tidligereAvkortingGrunnlag.relevanteMaanederInnAar === 12 : false
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
        aarsinntekt: nyligste.aarsinntekt,
        fratrekkInnAar: nyligste.fratrekkInnAar,
        inntektUtland: nyligste.inntektUtland,
        fratrekkInnAarUtland: nyligste.fratrekkInnAarUtland,
        spesifikasjon: '',
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
      (respons) => {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.AVKORTET))
        const nyttAvkortingGrunnlag = respons.avkortingGrunnlag[respons.avkortingGrunnlag.length - 1]
        nyttAvkortingGrunnlag.fraVirk && reset(nyttAvkortingGrunnlag.fraVirk)
        dispatch(oppdaterAvkorting(respons))
        setVisForm(false)
      }
    )
  }

  return (
    <>
      <VStack>
        <HStack marginBlock="8" gap="2" align="start" wrap={false}>
          <TekstFelt
            {...register('aarsinntekt', {
              pattern: { value: /^\d+$/, message: 'Kun tall' },
              required: { value: true, message: 'Må fylles ut' },
            })}
            label="Forventet årsinntekt Norge"
            size="medium"
            type="tel"
            inputMode="numeric"
            error={errors.aarsinntekt?.message}
          />
          <TekstFelt
            {...register('fratrekkInnAar', {
              required: { value: !alleMaanederIAaretErInnvilget(), message: 'Må fylles ut' },
              max: {
                value: watch('aarsinntekt') || 0,
                message: 'Kan ikke være høyere enn årsinntekt',
              },
              pattern: { value: /^\d+$/, message: 'Kun tall' },
            })}
            label="Fratrekk inn-år"
            size="medium"
            type="tel"
            inputMode="numeric"
            disabled={alleMaanederIAaretErInnvilget()}
            error={errors.fratrekkInnAar?.message}
          />
          <TekstFelt
            {...register('inntektUtland', {
              required: { value: true, message: 'Må fylles ut' },
              pattern: { value: /^\d+$/, message: 'Kun tall' },
            })}
            label="Forventet årsinntekt utland"
            size="medium"
            type="tel"
            inputMode="numeric"
            error={errors.inntektUtland?.message}
          />
          <TekstFelt
            {...register('fratrekkInnAarUtland', {
              required: { value: !alleMaanederIAaretErInnvilget(), message: 'Må fylles ut' },
              max: {
                value: watch('inntektUtland') || 0,
                message: 'Kan ikke være høyere enn årsinntekt utland',
              },
              pattern: { value: /^\d+$/, message: 'Kun tall' },
            })}
            label="Fratrekk inn-år"
            size="medium"
            type="tel"
            disabled={alleMaanederIAaretErInnvilget()}
            inputMode="numeric"
            error={errors.fratrekkInnAarUtland?.message}
          />
          <VStack gap="4">
            <Label>Fra og med dato</Label>
            <BodyShort>{formaterDato(inntektFom)}</BodyShort>
          </VStack>
        </HStack>
        <TextAreaWrapper
          {...register('spesifikasjon')}
          resize="vertical"
          label={
            <SpesifikasjonLabel>
              <Label>Spesifikasjon av inntekt</Label>
              <ReadMore header="Hva regnes som inntekt?">
                Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt med
                arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre nærståendes
                sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, svangerskapspenger og foreldrepenger etter kap
                14 og pensjonsytelser etter AFP tilskottloven kapitlene 2 og 3.
              </ReadMore>
            </SpesifikasjonLabel>
          }
        />
        <HStack gap="1" marginBlock="4">
          <Button size="small" loading={isPending(lagreAvkortingGrunnlagResult)} onClick={handleSubmit(onSubmit)}>
            Lagre
          </Button>
          <Button
            size="small"
            variant="tertiary"
            onClick={(e) => {
              e.preventDefault()
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
    </>
  )
}

const TekstFelt = styled(TextField)`
  max-width: 11.85em;
`

const TextAreaWrapper = styled(Textarea)`
  textArea {
    margin-top: 1em;
    width: 47em;
    height: 98px;
  }
`

const SpesifikasjonLabel = styled.div``
