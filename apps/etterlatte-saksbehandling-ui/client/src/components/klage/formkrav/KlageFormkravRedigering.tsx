import {
  Alert,
  BodyLong,
  Box,
  Button,
  Heading,
  HStack,
  Radio,
  RadioGroup,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
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
import { hentAlleVedtakISak } from '~shared/api/vedtaksvurdering'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterKanskjeStringDato, formaterVedtakType } from '~utils/formattering'
import { FieldOrNull } from '~shared/types/util'
import { Feilmelding, VurderingWrapper } from '~components/klage/styled'
import { kanVurdereUtfall, nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { isFailure, isPending, isPendingOrInitial, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ButtonNavigerTilBrev } from '~components/klage/vurdering/KlageVurderingFelles'

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
  const [vedtakISak, hentVedtakISak] = useApiCall(hentAlleVedtakISak)
  const [redigerModus, setRedigerModus] = React.useState(!klage?.formkrav?.formkrav)

  const {
    control,
    handleSubmit,
    formState: { isDirty },
    register,
  } = useForm<FormDataFormkrav>({
    defaultValues: klageFormkravTilDefaultFormValues(klage),
  })

  const navigate = useNavigate()

  useEffect(() => {
    if (!klage?.sak.id) {
      return
    }
    void hentVedtakISak(klage.sak.id)
  }, [klage?.sak.id])

  const kjenteVedtak =
    mapSuccess(vedtakISak, (alleVedtak) => alleVedtak.filter((vedtak) => !!vedtak.datoAttestert)) ?? []

  function sendInnFormkrav(krav: FormDataFormkrav) {
    if (!klage) {
      return
    }
    if (!isValidFormkrav(krav)) {
      return
    }
    if (!isDirty) {
      if (kanVurdereUtfall(klage)) {
        navigate(`/klage/${klage.id}/vurdering`)
      } else {
        navigate(`/klage/${klage.id}/oppsummering`)
      }
    }

    const formkrav = mapFormkrav(krav, kjenteVedtak)

    lagreFormkrav({ klageId: klage.id, formkrav }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      if (kanVurdereUtfall(oppdatertKlage)) {
        navigate(nesteSteg(oppdatertKlage, 'formkrav'))
      }
      setRedigerModus(false)
    })
  }

  if (isPendingOrInitial(vedtakISak)) {
    return <Spinner visible label={`Laster fattede vedtak på saken med sakId=${klage?.sak.id}`} />
  }

  if (isFailure(vedtakISak)) {
    return (
      <ApiErrorAlert>
        Kunne ikke laste iverksatte vedtak på saken, så klagen kan ikke knyttes opp mot et konkret vedtak. Prøv å last
        siden på nytt om en liten stund. Meld sak hvis problemet vedvarer.
      </ApiErrorAlert>
    )
  }

  if (!klage) return null

  return (
    <>
      <Box paddingInline="16" paddingBlock="4">
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurder formkrav og klagefrist
          </Heading>
        </HeadingWrapper>
      </Box>
      <form onSubmit={handleSubmit(sendInnFormkrav)}>
        <Box paddingBlock="8" paddingInline="16 8">
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
                    <Select
                      label="Hvilket vedtak klages det på?"
                      value={value ?? ''}
                      {...rest}
                      readOnly={!redigerModus}
                    >
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

          <div>
            <JaNeiRadiogruppe
              name="erKlagenFramsattInnenFrist"
              legend="Er klagen framsatt innenfor klagefristen?"
              description="Vurder også ytre klagefrist jf forv.loven § 31"
              control={control}
            />
          </div>

          <JaNeiRadiogruppe name="erFormkraveneOppfylt" control={control} legend="Er formkravene til klagen oppfylt?" />

          <VurderingWrapper>
            <Textarea {...register('begrunnelse')} label="Totalvurdering (valgfritt)" readOnly={!redigerModus} />
          </VurderingWrapper>

          {redigerModus ? (
            <HStack gap="4" justify="center">
              <Button type="submit" loading={isPending(lagreFormkravStatus)}>
                Lagre vurdering av formkrav
              </Button>
            </HStack>
          ) : (
            <VStack gap="2">
              <div>
                <Button onClick={() => setRedigerModus(true)} variant="secondary">
                  Endre vurdering
                </Button>
              </div>
              {kanVurdereUtfall(klage) ? (
                <HStack justify="center">
                  <Button as="a" href={nesteSteg(klage, 'formkrav')}>
                    Neste side
                  </Button>
                </HStack>
              ) : (
                <BeOmInfoFraKlager klage={klage} />
              )}
            </VStack>
          )}
          {isFailureHandler({
            apiResult: lagreFormkravStatus,
            errorMessage:
              'Kunne ikke lagre vurderingen av formkrav og klagefrist på grunn av en feil. Last siden på nytt og prøv igjen. Meld sak\n' +
              '            hvis problemet vedvarer.',
          })}
        </Box>
      </form>
    </>
  )

  function BeOmInfoFraKlager(props: { klage: Klage }) {
    const klage = props.klage

    return (
      <VurderingWrapper>
        <Alert variant="info">
          <Heading level="2" size="small">
            Hent informasjon fra klager
          </Heading>
          <BodyLong spacing>
            Du må innhente mer informasjon fra klager for å avgjøre om formkravene kan oppfylles. Sett klagebehandlingen
            på vent og opprett et nytt brev til klager.
          </BodyLong>
          <ButtonNavigerTilBrev klage={klage} />
        </Alert>
      </VurderingWrapper>
    )
  }

  function JaNeiRadiogruppe(props: {
    control: Control<FormDataFormkrav>
    name: Path<FormDataFormkrav>
    legend: string
    description?: string
  }) {
    const { name, control, legend, description } = props
    return (
      <Controller
        name={name}
        rules={{
          required: true,
        }}
        render={({ field, fieldState: { error } }) => (
          <VurderingWrapper>
            <RadioGroup
              readOnly={!redigerModus}
              legend={legend}
              description={description}
              className="radioGroup"
              {...field}
              error={error && (error.message || 'Du må svare på spørsmålet: ' + legend)}
            >
              <div className="flex">
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </div>
            </RadioGroup>
          </VurderingWrapper>
        )}
        control={control}
      />
    )
  }
}
