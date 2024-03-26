import {
  BodyShort,
  Button,
  ErrorMessage,
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
import React, { FormEvent, useState } from 'react'
import { IAvkorting, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { formaterStringDato, NOK } from '~utils/formattering'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { PencilIcon } from '@navikt/aksel-icons'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import { ToolTip } from '~components/behandling/felles/ToolTip'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppSelector } from '~store/Store'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'

export const AvkortingInntekt = ({
  behandling,
  avkorting,
  redigerbar,
  setAvkorting,
}: {
  behandling: IBehandlingReducer
  avkorting: IAvkorting | undefined
  redigerbar: boolean
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  if (!behandling) throw new Error('Mangler behandling')

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const erRedigerbar = redigerbar && enhetErSkrivbar(behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter)

  const avkortingGrunnlag = avkorting == null ? [] : [...avkorting.avkortingGrunnlag]
  avkortingGrunnlag?.sort((a, b) => new Date(b.fom!).getTime() - new Date(a.fom!).getTime())

  const virkningstidspunkt = () => {
    if (!behandling.virkningstidspunkt) throw new Error('Mangler virkningstidspunkt')
    return behandling.virkningstidspunkt.dato
  }

  // Er det utregnet avkorting finnes det grunnlag lagt til i denne behandlingen
  const finnesRedigerbartGrunnlag = () => avkorting?.avkortetYtelse && avkorting?.avkortetYtelse.length !== 0

  const finnRedigerbartGrunnlagEllerOpprettNytt = (): IAvkortingGrunnlag => {
    if (finnesRedigerbartGrunnlag()) {
      // Returnerer grunnlagsperiode som er opprettet i denne behandlingen
      const redigerbartGrunnlag = avkortingGrunnlag[0]
      if (redigerbartGrunnlag.fom !== behandling.virkningstidspunkt?.dato) {
        // Korrigerer fom hvis virkningstidspunkt er endret
        return {
          ...redigerbartGrunnlag,
          fom: virkningstidspunkt(),
        }
      }
      return redigerbartGrunnlag
    }
    if (avkortingGrunnlag.length > 0) {
      // Preutfyller ny grunnlagsperiode med tidligere verdier
      const nyligste = avkortingGrunnlag[0]
      return {
        fom: virkningstidspunkt(),
        fratrekkInnAar: nyligste.fratrekkInnAar,
        fratrekkInnAarUtland: nyligste.fratrekkInnAarUtland,
        relevanteMaanederInnAar: nyligste.relevanteMaanederInnAar,
        spesifikasjon: nyligste.spesifikasjon,
      }
    }
    // Første grunnlagsperiode
    return {
      fom: virkningstidspunkt(),
      spesifikasjon: '',
    }
  }

  const aktivtGrunnlag = () => {
    return avkortingGrunnlag.length > 0 ? [avkortingGrunnlag[0]] : []
  }

  const [inntektGrunnlagForm, setInntektGrunnlagForm] = useState<IAvkortingGrunnlag>(
    finnRedigerbartGrunnlagEllerOpprettNytt()
  )
  const [inntektGrunnlagStatus, requestLagreAvkortingGrunnlag] = useApiCall(lagreAvkortingGrunnlag)
  const [errorTekst, setErrorTekst] = useState<string | null>(null)

  const [formToggle, setFormToggle] = useState(false)
  const [visHistorikk, setVisHistorikk] = useState(false)
  const onSubmit = (e: FormEvent) => {
    e.preventDefault()

    setErrorTekst('')
    if (inntektGrunnlagForm.aarsinntekt == null) return setErrorTekst('Årsinntekt må fylles ut')
    if (inntektGrunnlagForm.fratrekkInnAar == null) return setErrorTekst('Fratrekk inn-år må fylles ut')
    if (inntektGrunnlagForm.inntektUtland == null) return setErrorTekst('Årsinntekt utland må fylles ut')
    if (inntektGrunnlagForm.fratrekkInnAarUtland == null) return setErrorTekst('Fratrekk inn-år utland må fylles ut')
    if (inntektGrunnlagForm.fom !== virkningstidspunkt())
      return setErrorTekst('Fra og med for forventet årsinntekt må være fra virkningstidspunkt')

    requestLagreAvkortingGrunnlag(
      {
        behandlingId: behandling.id,
        avkortingGrunnlag: inntektGrunnlagForm,
      },
      (respons) => {
        const nyttAvkortingGrunnlag = respons.avkortingGrunnlag[respons.avkortingGrunnlag.length - 1]
        nyttAvkortingGrunnlag && setInntektGrunnlagForm(nyttAvkortingGrunnlag)
        setAvkorting(respons)
        setFormToggle(false)
      }
    )
  }

  return (
    <AvkortingInntektWrapper>
      <Heading spacing size="small" level="2">
        Inntektsavkorting
      </Heading>
      <HjemmelLenke
        tittel="Folketrygdloven § 17-9 (mangler lenke)"
        lenke="https://lovdata.no/lov/" // TODO lenke finnes ikke enda
      />
      <BodyShort>
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
                <Table.HeaderCell>Forventet inntekt totalt</Table.HeaderCell>
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
                      {inntektsgrunnlag.fom && formaterStringDato(inntektsgrunnlag.fom)} -
                      {inntektsgrunnlag.tom && formaterStringDato(inntektsgrunnlag.tom)}
                    </Table.DataCell>
                    <Table.DataCell key="InntektSpesifikasjon">{inntektsgrunnlag.spesifikasjon}</Table.DataCell>
                    <Table.DataCell key="InntektKilde">
                      {inntektsgrunnlag.kilde && (
                        <Info
                          tekst={inntektsgrunnlag.kilde.ident}
                          label=""
                          undertekst={`saksbehandler: ${formaterStringDato(inntektsgrunnlag.kilde.tidspunkt)}`}
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
        <InntektAvkortingForm onSubmit={onSubmit}>
          <Rows>
            {formToggle && (
              <>
                <FormWrapper>
                  <HStack gap="4">
                    <TextField
                      label="Forventet årsinntekt Norge"
                      size="medium"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={inntektGrunnlagForm.aarsinntekt}
                      onChange={(e) =>
                        setInntektGrunnlagForm({
                          ...inntektGrunnlagForm,
                          aarsinntekt: e.target.value === '' ? undefined : Number(e.target.value),
                        })
                      }
                    />
                    <TextField
                      label="Fratrekk inn-år"
                      size="medium"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={inntektGrunnlagForm.fratrekkInnAar}
                      onChange={(e) =>
                        setInntektGrunnlagForm({
                          ...inntektGrunnlagForm,
                          fratrekkInnAar: Number(e.target.value),
                        })
                      }
                    />
                    <TextField
                      label="Forventet årsinntekt utland"
                      size="medium"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={inntektGrunnlagForm.inntektUtland}
                      onChange={(e) =>
                        setInntektGrunnlagForm({
                          ...inntektGrunnlagForm,
                          inntektUtland: Number(e.target.value),
                        })
                      }
                    />
                    <TextField
                      label="Fratrekk inn-år"
                      size="medium"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      value={inntektGrunnlagForm.fratrekkInnAarUtland}
                      onChange={(e) =>
                        setInntektGrunnlagForm({
                          ...inntektGrunnlagForm,
                          fratrekkInnAarUtland: Number(e.target.value),
                        })
                      }
                    />
                    <VStack gap="4">
                      <Label>Fra og med dato</Label>
                      <BodyShort>{formaterStringDato(inntektGrunnlagForm.fom!)}</BodyShort>
                    </VStack>
                  </HStack>
                </FormWrapper>
                <TextAreaWrapper>
                  <Textarea
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
                    value={inntektGrunnlagForm.spesifikasjon}
                    onChange={(e) =>
                      setInntektGrunnlagForm({
                        ...inntektGrunnlagForm,
                        spesifikasjon: e.target.value,
                      })
                    }
                  />
                </TextAreaWrapper>
                {errorTekst == null ? null : <ErrorMessage>{errorTekst}</ErrorMessage>}
              </>
            )}
            <FormKnapper>
              {formToggle ? (
                <>
                  <Button size="small" loading={isPending(inntektGrunnlagStatus)} type="submit">
                    Lagre
                  </Button>
                  <Button
                    size="small"
                    variant="tertiary"
                    onClick={(e) => {
                      e.preventDefault()
                      setFormToggle(false)
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
                    setFormToggle(true)
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
  width: 57em;
  margin-bottom: 3em;
`

const InntektAvkortingTabell = styled.div`
  margin: 1em 0 1em 0;
`

const InntektAvkortingForm = styled.form`
  display: flex;
  margin: 1em 0 1em 0;
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
