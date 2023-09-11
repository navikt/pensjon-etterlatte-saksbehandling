import { Button, Heading, Radio, RadioGroup, Select } from '@navikt/ds-react'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { useKlage } from '~components/klage/useKlage'
import { useNavigate } from 'react-router-dom'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { isFailure, isPending, isPendingOrInitial, mapSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterFormkravIKlage } from '~shared/api/klage'
import { JaNei } from '~shared/types/ISvar'
import React, { useEffect } from 'react'
import { Control, Controller, Path, useForm } from 'react-hook-form'
import { Formkrav, Klage, KlageStatus, VedtaketKlagenGjelder } from '~shared/types/Klage'
import styled from 'styled-components'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { hentIverksatteVedtakISak } from '~shared/api/vedtaksvurdering'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterKanskjeStringDato, formaterVedtakType } from '~utils/formattering'

// Vi bruker kun id'en til vedtaket i skjemadata, og transformerer fram / tilbake før sending / lasting
type FilledFormData = Omit<Formkrav, 'vedtaketKlagenGjelder'> & { vedtaketKlagenGjelderId: null | string }
type FormData = FieldOrNull<FilledFormData>

// På grunn av måten React håndterer controlled vs uncontrolled inputs tror React at input med value=undefined
// er uncontrolled input. Dette stemmer dårlig med hvordan vi vil bruke skjemaet med f.eks. Partial<FilledFormData>,
// så workarounden med FieldOrNull er brukt i stedet
type FieldOrNull<T> = {
  [P in keyof T]: T[P] | null
}

function isValidFormkrav(formdata: FormData): formdata is FilledFormData {
  return Object.values(formdata).every((value) => value !== null && value !== undefined)
}

const klageFormkravTilDefaultFormValues = (klage: Klage | null): FormData => {
  if (!klage || !klage.formkrav) {
    return {
      vedtaketKlagenGjelderId: null,
      erFormkraveneOppfylt: null,
      erKlagenFramsattInnenFrist: null,
      erKlagenSignert: null,
      erKlagerPartISaken: null,
      gjelderKlagenNoeKonkretIVedtaket: null,
    }
  } else {
    return {
      ...klage.formkrav.formkrav,
      vedtaketKlagenGjelderId: klage.formkrav.formkrav.vedtaketKlagenGjelder?.id ?? null,
    }
  }
}

function mapFormkrav(krav: FilledFormData, vedtakIKlagen: Array<VedtaketKlagenGjelder>): Formkrav {
  if (krav.vedtaketKlagenGjelderId !== '-1') {
    const vedtak = vedtakIKlagen.find((v) => v.id === krav.vedtaketKlagenGjelderId)

    // Vurder en mer kritisk feil her? Og ikke bare anta at de har valgt et tomt vedtak
    return { ...krav, vedtaketKlagenGjelder: vedtak ?? null }
  }
  return { ...krav, vedtaketKlagenGjelder: null }
}

export function KlageFormkrav() {
  const klage = useKlage()
  const [lagreFormkravStatus, lagreFormkrav] = useApiCall(oppdaterFormkravIKlage)
  const dispatch = useAppDispatch()
  const [iverksatteVedtak, hentIverksatteVedtak] = useApiCall(hentIverksatteVedtakISak)

  const { control, handleSubmit } = useForm<FormData>({ defaultValues: klageFormkravTilDefaultFormValues(klage) })

  const navigate = useNavigate()

  useEffect(() => {
    if (!klage?.sak.id) {
      return
    }
    void hentIverksatteVedtak(klage.sak.id)
  }, [klage?.sak.id])

  const kjenteVedtak = mapSuccess(iverksatteVedtak, (vedtak) => vedtak) ?? []

  function sendInnFormkrav(krav: FormData) {
    if (!klage) {
      return
    }
    if (!isValidFormkrav(krav)) {
      return
    }

    const formkrav = mapFormkrav(krav, kjenteVedtak)

    lagreFormkrav({ klageId: klage.id, formkrav }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      if (oppdatertKlage.status === KlageStatus.FORMKRAV_OPPFYLT) {
        navigate(`/klage/${klage.id}/vurdering`)
      } else if (oppdatertKlage.status === KlageStatus.FORMKRAV_IKKE_OPPFYLT) {
        navigate(`/klage/${klage.id}/oppsummering`)
      } else {
        // Noe rart har skjedd, tvinger en refresh
        window.location.reload()
      }

      // Feil i kallet fanges opp med visning av feilmelding i render
    })
  }

  if (isPendingOrInitial(iverksatteVedtak)) {
    return <Spinner visible={true} label={`Laster iverksatte vedtak på saken med sakId=${klage?.sak.id}`} />
  }

  if (isFailure(iverksatteVedtak)) {
    return (
      <ApiErrorAlert>
        Kunne ikke laste iverksatte vedtak på saken, så klagen kan ikke knyttes opp mot et konkret vedtak. Prøv å last
        siden på nytt om en liten stund. Meld sak hvis problemet vedvarer.
      </ApiErrorAlert>
    )
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurder formkrav
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <form onSubmit={handleSubmit(sendInnFormkrav)}>
        <Innhold>
          {/* Det er litt spesiell håndtering av akkurat hvilket vedtak klagen ligger på, relatert til hvordan React
            tolker controlled vs uncontrolled components. For å kunne håndtere både 1. Ikke valgt vedtak og 2. Valgt
            at det ikke er noe vedtak, tolkes null | undefined som 1), og vedtakId === "-1" som 2). Alle andre vedtakId
            tolkes som id'en til det vedtaket.*/}
          <VurderingWrapper>
            <Controller
              rules={{
                required: true,
              }}
              render={({ field, fieldState }) => {
                const { value, ...rest } = field
                return (
                  <>
                    <Select label="Hvilket vedtak klages det på?" value={value ?? undefined} {...rest}>
                      <option value={undefined}>Velg vedtak</option>
                      {kjenteVedtak.map((vedtak) => (
                        <option key={vedtak.id} value={vedtak.id}>
                          Vedtak {vedtak.id} om {formaterVedtakType(vedtak.vedtakType!!)} -{' '}
                          {formaterKanskjeStringDato(vedtak.datoAttestert)}
                        </option>
                      ))}
                      <option value="-1">Det klages ikke på et konkret vedtak</option>
                    </Select>
                    {fieldState.error ? (
                      <Feilmelding>
                        Du må velge vedtaket det klages på, eller svare at det ikke klages på et konkret vedtak
                      </Feilmelding>
                    ) : null}
                  </>
                )
              }}
              name="vedtaketKlagenGjelderId"
              control={control}
            />
          </VurderingWrapper>

          <JaNeiRadiogruppe name="erKlagerPartISaken" legend="Er klager part i saken?" control={control} />

          <JaNeiRadiogruppe name="erKlagenSignert" legend="Er klagen signert?" control={control} />

          <JaNeiRadiogruppe
            name="gjelderKlagenNoeKonkretIVedtaket"
            legend="Klages det på konkrete elementer i vedtaket?"
            control={control}
          />

          <JaNeiRadiogruppe
            name="erKlagenFramsattInnenFrist"
            legend="Er klagen framsatt innenfor klagefristen?"
            control={control}
          />

          <JaNeiRadiogruppe name="erFormkraveneOppfylt" control={control} legend="Er formkravene til klagen oppfylt?" />
        </Innhold>
        <KnapperWrapper>
          <Button type="submit" loading={isPending(lagreFormkravStatus)}>
            Lagre vurdering av formkrav
          </Button>
        </KnapperWrapper>

        {isFailure(lagreFormkravStatus) ? (
          <ApiErrorAlert>
            Kunne ikke lagre vurderingen av formkrav på grunn av en feil. Last siden på nytt og prøv igjen. Meld sak
            hvis problemet vedvarer.
          </ApiErrorAlert>
        ) : null}
      </form>
    </Content>
  )
}

function JaNeiRadiogruppe(props: {
  control: Control<FormData>
  name: Path<FormData>
  legend: string
  errorMessage?: string
}) {
  const { name, control, legend, errorMessage } = props
  return (
    <Controller
      name={name}
      rules={{
        required: true,
      }}
      render={({ field, fieldState }) => (
        <VurderingWrapper>
          <RadioGroup legend={legend} className="radioGroup" {...field}>
            <div className="flex">
              <Radio value={JaNei.JA}>Ja</Radio>
              <Radio value={JaNei.NEI}>Nei</Radio>
            </div>
          </RadioGroup>
          {fieldState.error ? (
            <Feilmelding>{errorMessage ?? 'Du må svare på spørsmålet: ' + legend}</Feilmelding>
          ) : null}
        </VurderingWrapper>
      )}
      control={control}
    />
  )
}

const VurderingWrapper = styled.div`
  margin-bottom: 2rem;

  width: 30rem;
`

const Feilmelding = styled.div`
  color: var(--a-text-danger);
`
