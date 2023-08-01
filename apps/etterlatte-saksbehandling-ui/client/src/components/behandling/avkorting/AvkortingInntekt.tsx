import { BodyShort, Button, ErrorMessage, Heading, Label, ReadMore, Table, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import React, { FormEvent, useState } from 'react'
import { IAvkorting, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreAvkortingGrunnlag } from '~shared/api/avkorting'
import { formaterStringDato, NOK } from '~utils/formattering'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { PencilIcon } from '@navikt/aksel-icons'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { ApiErrorAlert } from '~ErrorBoundary'

export const AvkortingInntekt = (props: {
  behandling: IBehandlingReducer
  avkortingGrunnlag?: IAvkortingGrunnlag[]
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  const behandling = props.behandling
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  if (!behandling) throw new Error('Mangler behandling')

  const virkningstidspunkt = () => {
    if (!behandling.virkningstidspunkt) throw new Error('Mangler virkningstidspunkt')
    return behandling.virkningstidspunkt.dato
  }
  const finnesRedigerbartGrunnlag = () => {
    if (props.avkortingGrunnlag) {
      const siste = props.avkortingGrunnlag[props.avkortingGrunnlag.length - 1]
      return siste.fom === behandling.virkningstidspunkt?.dato
    }
  }
  const finnRedigerbartGrunnlag = (): IAvkortingGrunnlag => {
    if (props.avkortingGrunnlag) {
      if (finnesRedigerbartGrunnlag()) {
        return props.avkortingGrunnlag[props.avkortingGrunnlag.length - 1]
      }
      if (props.avkortingGrunnlag.length > 0) {
        const siste = props.avkortingGrunnlag[props.avkortingGrunnlag.length - 1]
        return {
          fom: virkningstidspunkt(),
          fratrekkInnAar: siste.fratrekkInnAar,
          relevanteMaanederInnAar: siste.relevanteMaanederInnAar,
        }
      }
    }
    return {
      fom: virkningstidspunkt(),
      fratrekkInnAar: 0,
    }
  }

  const [inntektGrunnlagForm, setInntektGrunnlagForm] = useState<IAvkortingGrunnlag>(finnRedigerbartGrunnlag())
  const [inntektGrunnlagStatus, requestLagreAvkortingGrunnlag] = useApiCall(lagreAvkortingGrunnlag)
  const [errorTekst, setErrorTekst] = useState<string | null>(null)

  const [formToggle, setFormToggle] = useState(false)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()

    setErrorTekst('')
    if (inntektGrunnlagForm.aarsinntekt == null) return setErrorTekst('Årsinntekt må fylles ut')
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
        props.setAvkorting(respons)
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
        tittel={'Folketrygdloven § 17-9 (mangler lenke)'}
        lenke={'https://lovdata.no/lov/'} // TODO lenke finnes ikke enda
      />
      <BodyShort>
        Omstillingsstønaden reduseres med 45 prosent av den gjenlevende sin inntekt som på årsbasis overstiger et halvt
        grunnbeløp. Inntekt rundes ned til nærmeste tusen. Det er forventet årsinntekt for hvert kalenderår som skal
        legges til grunn.
      </BodyShort>
      <BodyShort>
        I innvilgelsesåret skal inntekt opptjent før innvilgelse trekkes fra, og resterende forventet inntekt omgjøres
        til årsinntekt. På samme måte skal inntekt etter opphør holdes utenfor i opphørsåret.
      </BodyShort>

      {props.avkortingGrunnlag && props.avkortingGrunnlag.length > 0 && (
        <InntektAvkortingTabell>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.HeaderCell>Forventet inntekt</Table.HeaderCell>
              <Table.HeaderCell>Fratrekk inn år</Table.HeaderCell>
              <Table.HeaderCell>Periode</Table.HeaderCell>
              <Table.HeaderCell>Spesifikasjon av inntekt</Table.HeaderCell>
              <Table.HeaderCell>Kilde</Table.HeaderCell>
            </Table.Header>
            <Table.Body>
              {(props.avkortingGrunnlag ? props.avkortingGrunnlag : []).map((inntektsgrunnlag, index) => (
                <Table.Row key={index}>
                  <Table.DataCell key="Inntekt">{NOK(inntektsgrunnlag.aarsinntekt)}</Table.DataCell>
                  <Table.DataCell key="FratrekkInnUt">{NOK(inntektsgrunnlag.fratrekkInnAar)}</Table.DataCell>
                  <Table.DataCell key="Periode">
                    {inntektsgrunnlag.fom && formaterStringDato(inntektsgrunnlag.fom)} -
                    {inntektsgrunnlag.tom && formaterStringDato(inntektsgrunnlag.tom)}
                  </Table.DataCell>
                  <Table.DataCell key="InntektSpesifikasjon">{inntektsgrunnlag.spesifikasjon}</Table.DataCell>
                  <Table.DataCell key="InntektKilde">
                    {inntektsgrunnlag.kilde && (
                      <Info
                        tekst={inntektsgrunnlag.kilde.ident}
                        label={''}
                        undertekst={`saksbehandler: ${formaterStringDato(inntektsgrunnlag.kilde.tidspunkt)}`}
                      />
                    )}
                  </Table.DataCell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        </InntektAvkortingTabell>
      )}
      {redigerbar && (
        <InntektAvkortingForm onSubmit={onSubmit}>
          <Rows>
            {formToggle && (
              <>
                <FormWrapper>
                  <TextField
                    label={'Forventet årsinnekt'}
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
                    label={'Fratrekk inn/ut'}
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
                  <DatoSection>
                    <Label>F.o.m dato</Label>
                    <Info label={''} tekst={formaterStringDato(inntektGrunnlagForm.fom!)} />
                  </DatoSection>
                </FormWrapper>
                <TextAreaWrapper>
                  <SpesifikasjonLabel>
                    <Label>Spesifikasjon av inntekt</Label>
                    <ReadMore header={'Hva regnes som inntekt?'}>
                      Med inntekt menes all arbeidsinntekt og ytelser som likestilles med arbeidsinntekt. Likestilt med
                      arbeidsinntekt er dagpenger etter kap 4, sykepenger etter kap 8, stønad ved barns og andre
                      nærståendes sykdom etter kap 9, arbeidsavklaringspenger etter kap 11, uføretrygd etter kap 12 der
                      uføregraden er under 100 prosent, svangerskapspenger og foreldrepenger etter kap 14 og
                      pensjonsytelser etter AFP tilskottloven kapitlene 2 og 3.
                    </ReadMore>
                  </SpesifikasjonLabel>
                  <textarea
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
      {isFailure(inntektGrunnlagStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
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
    width: 46em;
    height: 98px;
    text-indent: 4px;
    resize: none;
  }
`

const SpesifikasjonLabel = styled.div``

const DatoSection = styled.section`
  display: grid;
  gap: 0.5em;
`
const Rows = styled.div`
  flex-direction: column;
`
