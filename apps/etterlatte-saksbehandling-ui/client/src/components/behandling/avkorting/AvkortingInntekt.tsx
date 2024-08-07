import {
  Alert,
  BodyShort,
  Button,
  Heading,
  HelpText,
  HStack,
  Label,
  ReadMore,
  Table,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import styled from 'styled-components'
import React, { useState } from 'react'
import { IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { NOK } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { IBehandlingReducer, oppdaterAvkorting, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { PencilIcon } from '@navikt/aksel-icons'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import { ToolTip } from '~components/behandling/felles/ToolTip'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useForm } from 'react-hook-form'
import { IBehandlingStatus, IBehandlingsType, virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { lastDayOfMonth } from 'date-fns'

export const AvkortingInntekt = ({
  behandling,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  if (!behandling) return <Alert variant="error">Manlge behandling</Alert>
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)
  const harInstitusjonsopphold = behandling?.beregning?.beregningsperioder.find((bp) => bp.institusjonsopphold)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erRedigerbar = redigerbar && enhetErSkrivbar(behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter)
  const dispatch = useAppDispatch()

  const avkortingGrunnlag = avkorting == null ? [] : [...avkorting.avkortingGrunnlag]
  avkortingGrunnlag?.sort((a, b) => new Date(b.fom!).getTime() - new Date(a.fom!).getTime())

  const [inntektGrunnlagStatus, requestLagreAvkortingGrunnlag] = useApiCall(lagreAvkortingGrunnlag)

  const [visForm, setVisForm] = useState(false)
  const [visHistorikk, setVisHistorikk] = useState(false)

  // Er det utregnet avkorting finnes det grunnlag lagt til i denne behandlingen
  const finnesRedigerbartGrunnlag = () =>
    avkorting?.avkortingGrunnlag && avkortingGrunnlag[0].fom === virkningstidspunkt(behandling).dato

  const fulltAar = () => {
    if (behandling.behandlingType == IBehandlingsType.FØRSTEGANGSBEHANDLING) {
      const innvilgelseFraJanuar = new Date(virkningstidspunkt(behandling).dato).getMonth() === 0
      return innvilgelseFraJanuar
    } else {
      const revurderingIFulltAar = avkortingGrunnlag[0].relevanteMaanederInnAar === 12

      const revurderingINyttAar =
        new Date(avkortingGrunnlag[0].fom).getFullYear() < new Date(virkningstidspunkt(behandling).dato).getFullYear()

      return revurderingINyttAar || revurderingIFulltAar
    }
  }

  const finnRedigerbartGrunnlagEllerOpprettNytt = (): IAvkortingGrunnlagLagre => {
    if (finnesRedigerbartGrunnlag()) {
      // Returnerer grunnlagsperiode som er opprettet i denne behandlingen
      return avkortingGrunnlag[0]
    }
    if (avkortingGrunnlag.length > 0) {
      // Setter disabla felter til forventet verdi
      if (fulltAar()) {
        return { spesifikasjon: '', fratrekkInnAar: 0, fratrekkInnAarUtland: 0 }
      }
      // Preutfyller ny grunnlagsperiode med tidligere verdier
      const nyeste = avkortingGrunnlag[0]
      return { ...nyeste, id: undefined, spesifikasjon: '' }
    }
    // Første grunnlagsperiode
    return {
      spesifikasjon: '',
    }
  }

  const aktivtGrunnlag = () => {
    return avkortingGrunnlag.length > 0 ? [avkortingGrunnlag[0]] : []
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
        },
      },
      (respons) => {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.AVKORTET))
        const nyttAvkortingGrunnlag = respons.avkortingGrunnlag[respons.avkortingGrunnlag.length - 1]
        nyttAvkortingGrunnlag && reset(nyttAvkortingGrunnlag)
        dispatch(oppdaterAvkorting(respons))
        setVisForm(false)
      }
    )
  }

  return (
    <AvkortingInntektWrapper>
      <Heading spacing size="small" level="2">
        Inntektsavkorting
      </Heading>
      {harInstitusjonsopphold && (
        <Alert variant="error">
          Obs! Det er registrert institusjonsopphold i beregningen og dette er ikke støttet sammen med
          inntektsavkorting, bruk manuel overstyring.
        </Alert>
      )}
      <HjemmelLenke tittel="Folketrygdloven § 17-9" lenke="https://lovdata.no/pro/lov/1997-02-28-19/§17-9" />
      <BodyShort spacing>
        Omstillingsstønaden reduseres med 45 prosent av den gjenlevende sin inntekt som på årsbasis overstiger et halvt
        grunnbeløp. Inntekt rundes ned til nærmeste tusen. Det er forventet årsinntekt for hvert kalenderår som skal
        legges til grunn.
      </BodyShort>
      <BodyShort>
        I innvilgelsesåret skal inntekt opptjent før innvilgelse trekkes fra, og resterende forventet inntekt fordeles
        på gjenværende måneder. På samme måte skal inntekt etter opphør holdes utenfor i opphørsåret.
      </BodyShort>
      {avkortingGrunnlag.length > 0 && (
        <InntektAvkortingTabell>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>Forventet inntekt Norge</Table.HeaderCell>
                <Table.HeaderCell>Forventet inntekt utland</Table.HeaderCell>
                <Table.HeaderCell>
                  Forventet inntekt totalt
                  <HelpText title="Hva innebærer forventet inntekt totalt">
                    Forventet inntekt totalt er registrert inntekt Norge pluss inntekt utland minus eventuelt fratrekk
                    for inn-år. Beløpet vil automatisk avrundes ned til nærmeste tusen når avkorting beregnes.
                  </HelpText>
                </Table.HeaderCell>
                <Table.HeaderCell>
                  Innvilgede måneder
                  <HelpText title="Hva betyr innvilgede måneder">
                    Her vises antall måneder med innvilget stønad i gjeldende inntektsår. Registrert forventet inntekt,
                    med eventuelt fratrekk for inntekt opptjent før/etter innvilgelse, blir fordelt på de innvilgede
                    månedene. Antallet vil ikke endres selv om man tar en inntektsendring i løpet av året.
                  </HelpText>
                </Table.HeaderCell>
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Spesifikasjon av inntekt</Table.HeaderCell>
                <Table.HeaderCell>Kilde</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {(visHistorikk ? avkortingGrunnlag : aktivtGrunnlag()).map((inntektsgrunnlag, index) => {
                const aarsinntekt = inntektsgrunnlag.aarsinntekt ?? 0
                const fratrekkInnAar = inntektsgrunnlag.fratrekkInnAar ?? 0
                const forventetInntekt = aarsinntekt - fratrekkInnAar
                const inntektutland = inntektsgrunnlag.inntektUtland ?? 0
                const fratrekkUtland = inntektsgrunnlag.fratrekkInnAarUtland ?? 0
                const forventetInntektUtland = inntektutland - fratrekkUtland
                return (
                  <Table.Row key={index}>
                    <Table.DataCell key="Inntekt">
                      {NOK(forventetInntekt)}
                      <ToolTip title="Se hva forventet inntekt består av">
                        Forventet inntekt beregnes utfra forventet årsinntekt med fratrekk for måneder før innvilgelse.
                        <br />
                        Forventet inntekt Norge = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
                        {` ${NOK(aarsinntekt)} - ${NOK(fratrekkInnAar)} = ${NOK(forventetInntekt)}`}).
                      </ToolTip>
                    </Table.DataCell>
                    <Table.DataCell key="InntektUtland">
                      {NOK(forventetInntektUtland)}
                      <ToolTip title="Se hva forventet inntekt består av">
                        Forventet inntekt utland beregnes utfra inntekt utland med fratrekk for måneder før innvilgelse.
                        <br />
                        Forventet inntekt utland = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
                        {` ${NOK(inntektutland)} - ${NOK(fratrekkUtland)} = ${NOK(forventetInntektUtland)}`}).
                      </ToolTip>
                    </Table.DataCell>
                    <Table.DataCell key="InntektTotalt">
                      {NOK(forventetInntekt + forventetInntektUtland)}
                    </Table.DataCell>
                    <Table.DataCell>{inntektsgrunnlag.relevanteMaanederInnAar}</Table.DataCell>
                    <Table.DataCell key="Periode">
                      {inntektsgrunnlag.fom && formaterDato(inntektsgrunnlag.fom)} -{' '}
                      {inntektsgrunnlag.tom && formaterDato(lastDayOfMonth(new Date(inntektsgrunnlag.tom)))}
                    </Table.DataCell>
                    <Table.DataCell key="InntektSpesifikasjon">{inntektsgrunnlag.spesifikasjon}</Table.DataCell>
                    <Table.DataCell key="InntektKilde">
                      {inntektsgrunnlag.kilde && (
                        <Info
                          tekst={inntektsgrunnlag.kilde.ident}
                          label=""
                          undertekst={`saksbehandler: ${formaterDato(inntektsgrunnlag.kilde.tidspunkt)}`}
                        />
                      )}
                    </Table.DataCell>
                  </Table.Row>
                )
              })}
            </Table.Body>
          </Table>
        </InntektAvkortingTabell>
      )}
      {avkortingGrunnlag.length > 1 && <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />}
      {erRedigerbar && (
        <InntektAvkortingForm>
          <Rows>
            {visForm && (
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
                        max: { value: watch('aarsinntekt') || 0, message: 'Kan ikke være høyere enn årsinntekt' },
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
                          Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt
                          med arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre
                          nærståendes sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, svangerskapspenger og
                          foreldrepenger etter kap 14 og pensjonsytelser etter AFP tilskottloven kapitlene 2 og 3.
                        </ReadMore>
                      </SpesifikasjonLabel>
                    }
                  />
                </TextAreaWrapper>
              </>
            )}
            <FormKnapper>
              {visForm ? (
                <>
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
                </>
              ) : (
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
                  {finnesRedigerbartGrunnlag() ? 'Rediger' : 'Legg til'}
                </Button>
              )}
            </FormKnapper>
          </Rows>
        </InntektAvkortingForm>
      )}
      {isFailureHandler({
        apiResult: inntektGrunnlagStatus,
        errorMessage: 'En feil har oppstått',
      })}
    </AvkortingInntektWrapper>
  )
}

const AvkortingInntektWrapper = styled.div`
  max-width: 70rem;
`

const InntektAvkortingTabell = styled.div`
  margin: 1em 0 1em 0;
`

const InntektAvkortingForm = styled.form`
  display: flex;
  margin: 1em 0 0 0;
`

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
