import {
  BodyShort,
  Box,
  Button,
  ErrorMessage,
  Heading,
  HStack,
  Radio,
  ReadMore,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { useKlage } from '~components/klage/useKlage'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterFormkravIKlage } from '~shared/api/klage'
import { JaNei } from '~shared/types/ISvar'
import React, { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Formkrav, Klage, VedtaketKlagenGjelder } from '~shared/types/Klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { hentAlleVedtakISak } from '~shared/api/vedtaksvurdering'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterVedtakType } from '~utils/formatering/formatering'
import { formaterKanskjeStringDato } from '~utils/formatering/dato'
import { FieldOrNull } from '~shared/types/util'
import {
  kanVurdereUtfall,
  kanVurdereUtfallUtenKontaktMedKlager,
  nesteSteg,
} from '~components/klage/stegmeny/KlageStegmeny'
import { isFailure, isPending, isPendingOrInitial, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { BeOmInfoFraKlager } from '~components/klage/formkrav/components/BeOmInfoFraKlager'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { KlagerHarIkkeSvart } from './KlagerHarIkkeSvart'

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
      if (kanVurdereUtfallUtenKontaktMedKlager(klage)) {
        navigate(`/klage/${klage.id}/vurdering`)
      } else {
        navigate(`/klage/${klage.id}/oppsummering`)
      }
    }

    const formkrav = mapFormkrav(krav, kjenteVedtak)

    lagreFormkrav({ klageId: klage.id, formkrav }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      if (kanVurdereUtfallUtenKontaktMedKlager(oppdatertKlage)) {
        navigate(nesteSteg(oppdatertKlage, 'formkrav'))
      }
      setRedigerModus(false)
    })
  }

  if (isPendingOrInitial(vedtakISak)) {
    return <Spinner label={`Laster fattede vedtak på saken med sakId=${klage?.sak.id}`} />
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
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading level="1" size="large">
          Vurder formkrav og klagefrist
        </Heading>
      </Box>
      <form onSubmit={handleSubmit(sendInnFormkrav)}>
        <Box paddingBlock="space-8 space-0" paddingInline="space-16 space-8" maxWidth="42.5rem">
          {/* Det er litt spesiell håndtering av akkurat hvilket vedtak klagen ligger på, relatert til hvordan React
            tolker controlled vs uncontrolled components. For å kunne håndtere både 1. Ikke valgt vedtak og 2. Valgt
            at det ikke er noe vedtak, tolkes null | undefined som ""), og vedtakId === "-1" som 2). Alle andre vedtakId
            tolkes som id'en til det vedtaket. */}
          <VStack gap="space-4">
            <Controller
              rules={{
                required: true,
                minLength: 0,
              }}
              render={({ field, fieldState }) => {
                const { value, ...rest } = field
                return (
                  <>
                    <Box maxWidth="30rem">
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
                      {fieldState.error && (
                        <ErrorMessage>
                          Du må velge vedtaket det klages på, eller svare at det ikke klages på et konkret vedtak
                        </ErrorMessage>
                      )}
                    </Box>
                  </>
                )
              }}
              name="vedtaketKlagenGjelderId"
              control={control}
            />
            <Heading level="2" size="medium">
              Klagefrist
            </Heading>
            <ControlledRadioGruppe
              name="erKlagenFramsattInnenFrist"
              control={control}
              legend="Er klagefristen overholdt?"
              description={
                <ReadMore header="Hjelp til å vurdere kravet">
                  Etter folketrygdloven § 21-12 femte ledd er klagefristen seks uker. Etter forvaltningsloven § 29
                  begynner klagefristen å løpe fra det tidspunkt underretning om vedtaket er kommet frem til parten.
                  <br />
                  Hvis klagefristen ikke er overholdt, må det vurderes om klagen likevel kan behandles jamfør
                  forvaltningsloven § 31.
                </ReadMore>
              }
              errorVedTomInput="Du må sette om klagen er framsatt innenfor klagefristen"
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />

            <Heading level="2" size="medium">
              Formkrav
            </Heading>
            <BodyShort textColor="subtle">
              Etter forvaltningsloven § 32 må klagen oppfylle visse formkrav. Inneholder klagen feil eller mangler, skal
              det settes en kort frist for rettelse eller utfylling.
            </BodyShort>
            <ControlledRadioGruppe
              name="erKlagerPartISaken"
              control={control}
              legend="Er klagen fremsatt av parten eller annen med rettslig klageinteresse?"
              description={
                <ReadMore header="Hjelp til å vurdere kravet">
                  Etter forvaltningsloven § 28 er det bare part i saken eller annen med rettslig klageinteresse som kan
                  klage på vedtaket. Den som uten å være part i saken har en nær tilknytning til saken, kan ha rettslig
                  klageinteresse. Både parten og andre med rettslig klageinteresse kan etter forvaltningsloven § 12 la
                  seg representere av advokat eller annen fullmektig.
                </ReadMore>
              }
              errorVedTomInput="Du må sette om klager er part i saken"
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />

            <ControlledRadioGruppe
              name="erKlagenSignert"
              control={control}
              legend="Er klagen signert?"
              description={
                <ReadMore header="Hjelp til å vurdere kravet">
                  En klage skal være underskrevet av klageren eller hans fullmektig eller ha en kvalifisert elektronisk
                  signatur. Dette følger av forvaltningsloven § 32, lov om elektroniske tilleggstjenester § 1 og
                  eIDAS-forordningen artikkel 25 nr. 2.
                </ReadMore>
              }
              errorVedTomInput="Du må sette om klagen er signert"
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />

            <ControlledRadioGruppe
              name="gjelderKlagenNoeKonkretIVedtaket"
              control={control}
              legend="Klages det på konkrete elementer i vedtaket?"
              description="Etter forvaltningsloven § 32 skal klageren vise til vedtaket det klages over og hvilke endringer som ønskes. Klagen bør grunngis av klageren."
              errorVedTomInput="Du må sette om det klages på elementer i vedtaket"
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />

            <ControlledRadioGruppe
              name="erFormkraveneOppfylt"
              control={control}
              legend="Er formkravene til klagen oppfylt?"
              errorVedTomInput="Du må sette om formkravene til klagen er oppfylt"
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />
            <Box maxWidth="42.5rem">
              <Textarea
                {...register('begrunnelse')}
                label="Totalvurdering (valgfritt)"
                readOnly={!redigerModus}
                resize
              />
            </Box>
            {redigerModus ? (
              <HStack gap="space-4" justify="center">
                <Button type="submit" loading={isPending(lagreFormkravStatus)}>
                  Lagre vurdering av formkrav
                </Button>
              </HStack>
            ) : (
              <VStack gap="space-2">
                <div>
                  <Button onClick={() => setRedigerModus(true)} variant="secondary">
                    Endre vurdering
                  </Button>
                </div>
              </VStack>
            )}
          </VStack>

          {isFailureHandler({
            apiResult: lagreFormkravStatus,
            errorMessage:
              'Kunne ikke lagre vurderingen av formkrav og klagefrist på grunn av en feil. Last siden på nytt og prøv igjen. Meld sak\n' +
              '            hvis problemet vedvarer.',
          })}
        </Box>
      </form>
      <Box paddingBlock="space-8" paddingInline="space-16 space-8" maxWidth="42.5rem">
        <VStack gap="space-12">
          {!kanVurdereUtfallUtenKontaktMedKlager(klage) && (
            <>
              <BeOmInfoFraKlager klage={klage} />
              <KlagerHarIkkeSvart klage={klage} />
            </>
          )}

          {kanVurdereUtfall(klage) && (
            <HStack justify="center">
              <Button as="a" href={nesteSteg(klage, 'formkrav')}>
                Neste side
              </Button>
            </HStack>
          )}
        </VStack>
      </Box>
    </>
  )
}
