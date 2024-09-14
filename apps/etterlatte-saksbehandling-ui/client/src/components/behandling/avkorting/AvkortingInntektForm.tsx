import styled from 'styled-components'
import { BodyShort, Button, HStack, Label, ReadMore, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { IAvkortingGrunnlagForm, IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { IBehandlingStatus, IBehandlingsType, virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { IBehandlingReducer, oppdaterAvkorting, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { formaterDato } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { isPending } from '@reduxjs/toolkit'

export const AvkortingInntektForm = ({
  behandling,
  innevaerendeAar,
  avkortingGrunnlagForm,
  setVisForm,
}: {
  behandling: IBehandlingReducer
  avkortingGrunnlagForm: IAvkortingGrunnlagForm | undefined
  innevaerendeAar: boolean
  setVisForm: (visForm: boolean) => void
}) => {
  const dispatch = useAppDispatch()

  const [inntektGrunnlagStatus, requestLagreAvkortingGrunnlag] = useApiCall(lagreAvkortingGrunnlag)

  const virkningstidspunktDate = new Date(virkningstidspunkt(behandling).dato)
  const inntektsAar = innevaerendeAar ? virkningstidspunktDate.getFullYear() : virkningstidspunktDate.getFullYear() + 1

  const fulltAar = () => {
    // TODO forenkle? unittest? backend?
    if (!innevaerendeAar) {
      return true
    }
    if (behandling.behandlingType == IBehandlingsType.FØRSTEGANGSBEHANDLING) {
      const innvilgelseFraJanuar = virkningstidspunktDate.getMonth() === 0
      return innvilgelseFraJanuar
    } else {
      const siste = avkortingGrunnlagForm!.fraVirk ?? avkortingGrunnlagForm!.historikk[0] // TODO

      const revurderingIFulltAar = siste.relevanteMaanederInnAar === 12

      const revurderingINyttAar =
        new Date(siste.fom).getFullYear() < new Date(virkningstidspunkt(behandling).dato).getFullYear()

      return revurderingINyttAar || revurderingIFulltAar
    }
  }

  const finnRedigerbartGrunnlagEllerOpprettNytt = (): IAvkortingGrunnlagLagre => {
    if (avkortingGrunnlagForm?.fraVirk !== undefined) {
      return avkortingGrunnlagForm.fraVirk
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
    requestLagreAvkortingGrunnlag(
      {
        behandlingId: behandling.id,
        avkortingGrunnlag: {
          ...data,
          fratrekkInnAar: data.fratrekkInnAar ?? 0,
          fratrekkInnAarUtland: data.fratrekkInnAarUtland ?? 0,
          fom: innevaerendeAar ? virkningstidspunkt(behandling).dato : `${inntektsAar}-01`, // TODO forenkle?
        },
      },
      (respons) => {
        // TODO vil dette fungere ?!!
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.AVKORTET))
        const nyttAvkortingGrunnlag = respons.avkortingGrunnlag[respons.avkortingGrunnlag.length - 1]
        nyttAvkortingGrunnlag && reset(nyttAvkortingGrunnlag)
        dispatch(oppdaterAvkorting(respons))
        setVisForm(false)
      }
    )
  }

  return (
    <>
      <Rows>
        <>
          <FormWrapper>
            <HStack gap="2" align="start" wrap={false}>
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
                  required: { value: !fulltAar(), message: 'Må fylles ut' },
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
                disabled={fulltAar()}
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
                  required: { value: !fulltAar(), message: 'Må fylles ut' },
                  max: {
                    value: watch('inntektUtland') || 0,
                    message: 'Kan ikke være høyere enn årsinntekt utland',
                  },
                  pattern: { value: /^\d+$/, message: 'Kun tall' },
                })}
                label="Fratrekk inn-år"
                size="medium"
                type="tel"
                disabled={fulltAar()}
                inputMode="numeric"
                error={errors.fratrekkInnAarUtland?.message}
              />
              <VStack gap="4">
                <Label>Fra og med dato</Label>
                <BodyShort>{formaterDato(virkningstidspunkt(behandling).dato)}</BodyShort>
              </VStack>
            </HStack>
          </FormWrapper>
          <TextAreaWrapper>
            <Textarea
              {...register('spesifikasjon')}
              resize="vertical"
              label={
                <SpesifikasjonLabel>
                  <Label>Spesifikasjon av inntekt</Label>
                  <ReadMore header="Hva regnes som inntekt?">
                    Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt med
                    arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre
                    nærståendes sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, svangerskapspenger og
                    foreldrepenger etter kap 14 og pensjonsytelser etter AFP tilskottloven kapitlene 2 og 3.
                  </ReadMore>
                </SpesifikasjonLabel>
              }
            />
          </TextAreaWrapper>
          <FormKnapper>
            <Button size="small" loading={isPending(inntektGrunnlagStatus)} onClick={handleSubmit(onSubmit)}>
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
          </FormKnapper>
        </>
      </Rows>
      {isFailureHandler({
        apiResult: inntektGrunnlagStatus,
        errorMessage: 'En feil har oppstått',
      })}
    </>
  )
}

const FormWrapper = styled.div`
  display: flex;
  gap: 1rem;
  margin-top: 2em;
  margin-bottom: 2em;
`

const FormKnapper = styled.div`
  margin-top: 1rem;
  margin-right: 1em;
  gap: 1rem;
`

const TekstFelt = styled(TextField)`
  max-width: 11.85em;
`

const TextAreaWrapper = styled.div`
  display: grid;
  align-items: flex-end;
  margin-top: 1em;
  margin-bottom: 1em;

  textArea {
    margin-top: 1em;
    border-width: 1px;
    border-radius: 4px 4px 0 4px;
    width: 47em;
    height: 98px;
    text-indent: 4px;
    resize: none;
  }
`

const SpesifikasjonLabel = styled.div``

const Rows = styled.div`
  flex-direction: column;
`
