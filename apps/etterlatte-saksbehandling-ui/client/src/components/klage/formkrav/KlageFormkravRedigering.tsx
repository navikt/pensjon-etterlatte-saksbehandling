import { Button, Heading, HelpText, Radio, RadioGroup, Select, Textarea } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useKlage } from '~components/klage/useKlage'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterFormkravIKlage } from '~shared/api/klage'
import { JaNei } from '~shared/types/ISvar'
import React, { useEffect } from 'react'
import { Control, Controller, Path, useForm } from 'react-hook-form'
import { Formkrav, Klage, VedtaketKlagenGjelder } from '~shared/types/Klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { hentIverksatteVedtakISak } from '~shared/api/vedtaksvurdering'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterKanskjeStringDato, formaterVedtakType } from '~utils/formattering'
import { FieldOrNull } from '~shared/types/util'
import { Feilmelding, VurderingWrapper } from '~components/klage/styled'
import { kanVurdereUtfall, nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { isFailure, isPending, isPendingOrInitial, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

// Vi bruker kun id'en til vedtaket i skjemadata, og transformerer fram / tilbake før sending / lasting
type FilledFormDataFormkrav = Omit<Formkrav, 'vedtaketKlagenGjelder'> & { vedtaketKlagenGjelderId: null | string }
type FormDataFormkrav = FieldOrNull<FilledFormDataFormkrav>

function isValidFormkrav(formdata: FormDataFormkrav): formdata is FilledFormDataFormkrav {
  const {
    erKlagenFramsattInnenFrist,
    erKlagenSignert,
    erFormkraveneOppfylt,
    erKlagerPartISaken,
    vedtaketKlagenGjelderId,
    gjelderKlagenNoeKonkretIVedtaket,
  } = formdata
  return [
    erKlagerPartISaken,
    erKlagenSignert,
    erKlagenFramsattInnenFrist,
    erFormkraveneOppfylt,
    vedtaketKlagenGjelderId,
    gjelderKlagenNoeKonkretIVedtaket,
  ].every((value) => value !== null)
}

const klageFormkravTilDefaultFormValues = (klage: Klage | null): FormDataFormkrav => {
  if (!klage || !klage.formkrav) {
    return {
      vedtaketKlagenGjelderId: null,
      erFormkraveneOppfylt: null,
      erKlagenFramsattInnenFrist: null,
      erKlagenSignert: null,
      erKlagerPartISaken: null,
      gjelderKlagenNoeKonkretIVedtaket: null,
      begrunnelse: null,
    }
  } else {
    const { vedtaketKlagenGjelder, ...skjemafelter } = klage.formkrav.formkrav
    return {
      ...skjemafelter,
      vedtaketKlagenGjelderId: vedtaketKlagenGjelder?.id ?? '-1',
    }
  }
}

function mapFormkrav(krav: FilledFormDataFormkrav, vedtakIKlagen: Array<VedtaketKlagenGjelder>): Formkrav {
  if (krav.vedtaketKlagenGjelderId !== '-1') {
    const vedtak = vedtakIKlagen.find((v) => v.id === krav.vedtaketKlagenGjelderId)

    // Vurder en mer kritisk feil her? Og ikke bare anta at de har valgt et tomt vedtak
    return { ...krav, vedtaketKlagenGjelder: vedtak ?? null }
  }
  return { ...krav, vedtaketKlagenGjelder: null }
}

export function KlageFormkravRedigering() {
  const klage = useKlage()
  const [lagreFormkravStatus, lagreFormkrav] = useApiCall(oppdaterFormkravIKlage)
  const dispatch = useAppDispatch()
  const [iverksatteVedtak, hentIverksatteVedtak] = useApiCall(hentIverksatteVedtakISak)

  const {
    control,
    handleSubmit,
    formState: { isDirty },
    register,
    watch,
  } = useForm<FormDataFormkrav>({
    defaultValues: klageFormkravTilDefaultFormValues(klage),
  })

  const navigate = useNavigate()
  const erKlagenFramsattInnenFrist = watch('erKlagenFramsattInnenFrist')

  useEffect(() => {
    if (!klage?.sak.id) {
      return
    }
    void hentIverksatteVedtak(klage.sak.id)
  }, [klage?.sak.id])

  const kjenteVedtak = mapSuccess(iverksatteVedtak, (vedtak) => vedtak) ?? []

  function sendInnFormkrav(krav: FormDataFormkrav) {
    if (!klage) {
      return
    }
    if (!isValidFormkrav(krav)) {
      return
    }
    if (!isDirty) {
      // Skjemaet er fylt ut, men med samme info som innholdet i klagen fra backend. Dermed lagrer vi ikke på nytt og
      // bare går videre til neste riktige steg
      if (kanVurdereUtfall(klage)) {
        navigate(`/klage/${klage.id}/vurdering`)
      } else {
        navigate(`/klage/${klage.id}/oppsummering`)
      }
    }

    const formkrav = mapFormkrav(krav, kjenteVedtak)

    lagreFormkrav({ klageId: klage.id, formkrav }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      navigate(nesteSteg(klage, 'formkrav'))
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
            Vurder formkrav og klagefrist
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <form onSubmit={handleSubmit(sendInnFormkrav)}>
        <InnholdPadding>
          {/* Det er litt spesiell håndtering av akkurat hvilket vedtak klagen ligger på, relatert til hvordan React
            tolker controlled vs uncontrolled components. For å kunne håndtere både 1. Ikke valgt vedtak og 2. Valgt
            at det ikke er noe vedtak, tolkes null | undefined som ""), og vedtakId === "-1" som 2). Alle andre vedtakId
            tolkes som id'en til det vedtaket. */}
          <VurderingWrapper>
            <Controller
              rules={{
                required: true,
                minLength: 0,
              }}
              render={({ field, fieldState }) => {
                const { value, ...rest } = field
                return (
                  <>
                    <Select label="Hvilket vedtak klages det på?" value={value ?? ''} {...rest}>
                      <option value="">Velg vedtak</option>
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

          <FlexRow>
            <JaNeiRadiogruppe
              name="erKlagenFramsattInnenFrist"
              legend="Er klagen framsatt innenfor klagefristen?"
              control={control}
            />
            {erKlagenFramsattInnenFrist == JaNei.NEI && (
              <HelpText strategy="fixed" title="Avvisning ved utløpt klagefrist">
                Hvis klagefristen ikke er overholdt og dette sannsynligvis vil resultere i en avvisning av klagen bør du
                vurdere å direkte gå til vedtak om avvisning (velg formkrav oppfylt)
              </HelpText>
            )}
          </FlexRow>

          <JaNeiRadiogruppe name="erFormkraveneOppfylt" control={control} legend="Er formkravene til klagen oppfylt?" />

          <VurderingWrapper>
            <Textarea {...register('begrunnelse')} label="Begrunnelse (valgfritt)" />
          </VurderingWrapper>
        </InnholdPadding>
        <FlexRow justify="center">
          <Button style={{ marginBottom: '3em' }} type="submit" loading={isPending(lagreFormkravStatus)}>
            Lagre vurdering av formkrav
          </Button>
        </FlexRow>

        {isFailureHandler({
          apiResult: lagreFormkravStatus,
          errorMessage:
            'Kunne ikke lagre vurderingen av formkrav og klagefrist på grunn av en feil. Last siden på nytt og prøv igjen. Meld sak\n' +
            '            hvis problemet vedvarer.',
        })}
      </form>
    </Content>
  )
}

function JaNeiRadiogruppe(props: {
  control: Control<FormDataFormkrav>
  name: Path<FormDataFormkrav>
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
