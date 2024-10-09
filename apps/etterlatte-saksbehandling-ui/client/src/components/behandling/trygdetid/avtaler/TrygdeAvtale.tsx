import { FloppydiskIcon, HandshakeIcon, PencilIcon, XMarkIcon } from '@navikt/aksel-icons'
import { Alert, Box, Button, Heading, HelpText, HGrid, HStack, Radio, Select, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import {
  hentAlleTrygdetidAvtaleKriterier,
  hentAlleTrygdetidAvtaler,
  hentTrygdeavtaleForBehandling,
  lagreTrygdeavtaleForBehandling,
  Trygdeavtale,
  TrygdeavtaleRequest,
  TrygdetidAvtale,
  TrygdetidAvtaleKriteria,
  TrygdetidAvtaleOptions,
} from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IconSize } from '~shared/types/Icon'
import { TrygdeavtaleVisning } from './TrygdeavtaleVisning'
import { JaNei } from '~shared/types/ISvar'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AvdoedesTrygdetidReadMore } from '~components/behandling/trygdetid/components/AvdoedesTrygdetidReadMore'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useBehandling } from '~components/behandling/useBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'

interface TrygdetidAvtaleOptionProps {
  defaultBeskrivelse: string
  trygdeavtaleOptions: TrygdetidAvtaleOptions[]
}

const TrygdetidAvtaleOptionsView = ({ defaultBeskrivelse, trygdeavtaleOptions }: TrygdetidAvtaleOptionProps) => {
  return (
    <>
      <option value="">{defaultBeskrivelse}</option>
      {trygdeavtaleOptions.map((trygdeavtaleOption) => (
        <option key={`${trygdeavtaleOption.kode}`} value={trygdeavtaleOption.kode}>
          {trygdeavtaleOption.beskrivelse}
        </option>
      ))}
    </>
  )
}

interface Props {
  redigerbar: boolean
}

export const TrygdeAvtale = ({ redigerbar }: Props) => {
  const { behandlingId } = useParams()
  const [hentAlleTrygdetidAvtalerRequest, fetchTrygdetidAvtaler] = useApiCall(hentAlleTrygdetidAvtaler)
  const [hentAlleTrygdetidAvtalerKriterierRequest, fetchTrygdetidAvtaleKriterier] = useApiCall(
    hentAlleTrygdetidAvtaleKriterier
  )
  const [hentTrygdeavtaleRequest, fetchTrygdeavtale] = useApiCall(hentTrygdeavtaleForBehandling)
  const [avtalerListe, setAvtalerListe] = useState<TrygdetidAvtale[]>()
  const [avtaleKriterierListe, setAvtaleKriterierListe] = useState<TrygdetidAvtaleKriteria[]>()
  const [trygdeavtale, setTrygdeavtale] = useState<Trygdeavtale | undefined>(undefined)
  const [redigering, setRedigering] = useState<Boolean>(true)

  useEffect(() => {
    fetchTrygdetidAvtaler(null, (avtaler: TrygdetidAvtale[]) => {
      setAvtalerListe(avtaler.sort((a: TrygdetidAvtale, b: TrygdetidAvtale) => trygdeavtaleOptionSort(a, b)))
    })

    fetchTrygdetidAvtaleKriterier(null, (avtaler: TrygdetidAvtaleKriteria[]) => {
      setAvtaleKriterierListe(
        avtaler.sort((a: TrygdetidAvtaleKriteria, b: TrygdetidAvtaleKriteria) => trygdeavtaleOptionSort(a, b))
      )
    })

    if (behandlingId) {
      fetchTrygdeavtale({ behandlingId: behandlingId }, (avtale: Trygdeavtale) => {
        if (avtale.avtaleKode) {
          setTrygdeavtale(avtale)
          setRedigering(false)
        }
      })
    }
  }, [])

  const oppdaterTrygdeavtale = (trygdeavtale: Trygdeavtale) => {
    setRedigering(false)
    setTrygdeavtale(trygdeavtale)
  }

  const avbryt = () => {
    setRedigering(false)
  }

  const rediger = () => {
    setRedigering(true)
  }

  const trygdeavtaleOptionSort = (a: TrygdetidAvtaleOptions, b: TrygdetidAvtaleOptions) => {
    if (a.beskrivelse > b.beskrivelse) {
      return 1
    }
    return -1
  }

  return (
    <Box paddingBlock="8 0">
      <VStack gap="4">
        <HStack gap="2" align="center">
          <HandshakeIcon fontSize={IconSize.DEFAULT} />
          <Heading size="small" level="3">
            Vurdering av trygdeavtale (Avdød)
          </Heading>
        </HStack>

        <AvdoedesTrygdetidReadMore />
        {!redigering &&
          avtalerListe &&
          avtaleKriterierListe &&
          (trygdeavtale ? (
            <>
              <TrygdeavtaleVisning
                avtaler={avtalerListe}
                kriterier={avtaleKriterierListe}
                trygdeavtale={trygdeavtale}
              />
              {redigerbar && (
                <div>
                  <Button size="small" variant="secondary" onClick={rediger} type="button" icon={<PencilIcon />}>
                    Rediger avtale
                  </Button>
                </div>
              )}
            </>
          ) : (
            <ApiErrorAlert>
              Kunne ikke vise trygdeavtale. Meld sak i porten hvis dette ikke løser seg selv ved å laste siden på nytt
            </ApiErrorAlert>
          ))}
        {isSuccess(hentAlleTrygdetidAvtalerRequest) &&
          isSuccess(hentAlleTrygdetidAvtalerKriterierRequest) &&
          isSuccess(hentTrygdeavtaleRequest) &&
          redigerbar &&
          redigering && (
            <TrygdeAvtaleRedigering
              trygdeavtale={trygdeavtale ?? {}}
              avtaler={avtalerListe ?? []}
              kriterier={avtaleKriterierListe ?? []}
              oppdaterAvtale={oppdaterTrygdeavtale}
              avbryt={avbryt}
            />
          )}
        {(isPending(hentAlleTrygdetidAvtalerRequest) ||
          isPending(hentAlleTrygdetidAvtalerKriterierRequest) ||
          isPending(hentTrygdeavtaleRequest)) && <Spinner label="Henter trygdeavtaler" />}

        {isFailureHandler({
          apiResult: hentAlleTrygdetidAvtalerRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdeavtaler',
        })}
        {isFailureHandler({
          apiResult: hentAlleTrygdetidAvtalerKriterierRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdeavtalekriterier',
        })}
        {isFailureHandler({
          apiResult: hentTrygdeavtaleRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdeavtale for behandlingen',
        })}
      </VStack>
    </Box>
  )
}

function TrygdeAvtaleRedigering(props: {
  trygdeavtale: Partial<Trygdeavtale>
  avtaler: TrygdetidAvtale[]
  kriterier: TrygdetidAvtaleKriteria[]
  oppdaterAvtale: (avtale: Trygdeavtale) => void
  avbryt: () => void
}) {
  const { trygdeavtale, avtaler, kriterier, avbryt, oppdaterAvtale } = props
  const behandling = useBehandling()
  const [lagreTrygdeavtaleRequest, lagreTrygdeavtale] = useApiCall(lagreTrygdeavtaleForBehandling)
  const {
    control,
    register,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<Partial<Trygdeavtale>>({
    defaultValues: trygdeavtale,
  })

  if (!behandling?.id) {
    return null
  }

  const lagreOgOppdater = (trygdeavtale: Partial<Trygdeavtale>) => {
    lagreTrygdeavtale(
      {
        behandlingId: behandling.id,
        avtaleRequest: {
          avtaleKode: trygdeavtale?.avtaleKode,
          avtaleDatoKode: trygdeavtale?.avtaleDatoKode,
          avtaleKriteriaKode: trygdeavtale?.avtaleKriteriaKode,
          personKrets: trygdeavtale?.personKrets,
          arbInntekt1G: trygdeavtale?.arbInntekt1G,
          arbInntekt1GKommentar: trygdeavtale?.arbInntekt1GKommentar,
          beregArt50: trygdeavtale?.beregArt50,
          beregArt50Kommentar: trygdeavtale?.beregArt50Kommentar,
          nordiskTrygdeAvtale: trygdeavtale?.nordiskTrygdeAvtale,
          nordiskTrygdeAvtaleKommentar: trygdeavtale?.nordiskTrygdeAvtaleKommentar,
          id: trygdeavtale?.id,
        } as TrygdeavtaleRequest,
      },
      (respons) => {
        oppdaterAvtale(respons)
      }
    )
  }

  const valgtAvtaleKode = watch('avtaleKode')
  const valgtAvtale = avtaler.find((avtale) => avtale.kode === valgtAvtaleKode)

  return (
    <form onSubmit={handleSubmit(lagreOgOppdater)}>
      <VStack gap="6">
        <HStack gap="4" align="start">
          <Select
            {...register('avtaleKode', {
              required: {
                value: true,
                message: 'Du må velge en trygdeavtale',
              },
            })}
            error={errors.avtaleKode?.message}
            label="Avtale"
          >
            <TrygdetidAvtaleOptionsView defaultBeskrivelse="Velg avtale" trygdeavtaleOptions={avtaler} />
          </Select>
          {valgtAvtale && valgtAvtale.datoer.length > 0 && (
            <Select
              label="Dato"
              autoComplete="off"
              {...register('avtaleDatoKode', {
                required: {
                  value: true,
                  message: 'Du må velge datoen for avtalen.',
                },
              })}
              error={errors.avtaleDatoKode?.message}
            >
              <TrygdetidAvtaleOptionsView
                defaultBeskrivelse="Velg avtaledato"
                trygdeavtaleOptions={valgtAvtale.datoer}
              />
            </Select>
          )}
        </HStack>

        <Box width="50%">
          <Select
            label="Kriterier for å omfattes av avtalen"
            autoComplete="off"
            {...register('avtaleKriteriaKode', {
              required: {
                value: true,
                message: 'Du må velge kriterier for å omfattes av avtalen',
              },
            })}
            error={errors.avtaleKriteriaKode?.message}
          >
            <TrygdetidAvtaleOptionsView defaultBeskrivelse="Velg kriterie" trygdeavtaleOptions={kriterier} />
          </Select>
        </Box>

        <VStack gap="2">
          <Heading size="xsmall">Er avdøde i personkretsen i denne avtalen?</Heading>
          <ControlledRadioGruppe
            control={control}
            name="personKrets"
            legend="Er avdøde i personkretsen i denne avtalen?"
            hideLegend
            errorVedTomInput="Du må svare ja eller nei."
            size="small"
            radios={
              <>
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </>
            }
          />
        </VStack>

        <HGrid gap="8 4" columns="60% 40%">
          <VStack gap="2">
            <HStack gap="2">
              <Heading size="xsmall">Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet?</Heading>
              <HelpText>
                Poengår (år med arbeidsinntekt på mer enn 1 G) i andre EØS-land medregnes som poengår, forutsatt at det
                ikke er tjent opp poengår i Norge i året. Hvis «Ja» gir det rett til fremtidige poeng, eller fremtidig
                trygdetid, ved en prorata beregning. Hvis «Nei» gir det ikke rett til dette.
              </HelpText>
            </HStack>
            <>
              <HjemmelLenke
                tittel="Rundskriv til hovednummer 45 kap. 3 punkt 3.3.2"
                lenke="https://lovdata.no/pro/rundskriv/r45-00/KAPITTEL_3-3-2-1"
              />
              <ControlledRadioGruppe
                legend="Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet?"
                hideLegend
                errorVedTomInput="Du må svare ja eller nei."
                control={control}
                name="arbInntekt1G"
                size="small"
                radios={
                  <>
                    <Radio value={JaNei.JA}>Ja</Radio>
                    <Radio value={JaNei.NEI}>Nei</Radio>
                  </>
                }
              />
              {watch('arbInntekt1G') === JaNei.NEI && (
                <Alert variant="info" size="small" inline>
                  Det gis ikke rett til fremtidig trygdetid fra utland ved en prorata beregning. Hvis det heller ikke er
                  rett til fremtidig trygdetid etter nasjonale regler, må du ta bort registrert fremtidig trygdetid.
                </Alert>
              )}
            </>
          </VStack>
          <Box>
            <Textarea
              label="Kommentar"
              {...register('arbInntekt1GKommentar')}
              minRows={2}
              autoComplete="off"
              size="small"
            />
          </Box>

          <VStack gap="2">
            <HStack gap="2">
              <Heading size="xsmall">Beregning etter artikkel 50 (EØS-forordning 883/2004)?</Heading>
              <HelpText>
                Denne artikkelen skal anvendes hvis det foreligger pensjonsrett i minst to EØS-land i tillegg til Norge,
                og hvis vilkårene for pensjon ikke er oppfylt i alle EØS-landene avdøde har opptjening i. Det skal
                gjøres en alternativ prorata-beregning med trygdetid kun for de EØS-landene der rett til pensjon er
                oppfylt. Dette er fordi trygdetid fra land der vilkårene ikke er oppfylte ikke skal medregnes hvis det
                ikke lønner seg.
              </HelpText>
            </HStack>
            <>
              <HjemmelLenke
                tittel="EØS-forordning 883/2004 artikkel 50"
                lenke="https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_50"
              />
              <ControlledRadioGruppe
                legend="Beregning etter artikkel 50 (EØS-forordning 883/2004)?"
                hideLegend
                control={control}
                errorVedTomInput="Du må svare ja eller nei."
                name="beregArt50"
                size="small"
                radios={
                  <>
                    <Radio value={JaNei.JA}>Ja</Radio>
                    <Radio value={JaNei.NEI}>Nei</Radio>
                  </>
                }
              />
              {watch('beregArt50') === JaNei.JA && (
                <Alert variant="info" size="small" inline>
                  Ta en alternativ prorata-beregning. Huk av for «Ikke i prorata» på trygdetidsperioder for EØS-land som
                  har gitt avslag på ytelse.
                </Alert>
              )}
            </>
          </VStack>
          <Box>
            <Textarea
              label="Kommentar"
              {...register('beregArt50Kommentar')}
              minRows={2}
              size="small"
              autoComplete="off"
            />
          </Box>

          <VStack gap="2">
            <HStack gap="2">
              <Heading size="xsmall">Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?</Heading>
              <HelpText>
                Hvis forutgående medlemskap, og derav vilkår for å beregne framtidig trygdetid, er oppfylt etter
                nasjonale regler i Sverige og/eller Island i tillegg til Norge, skal framtidig trygdetid avkortes. I en
                prorata-beregnet ytelse, der forutgående medlemskap er oppfylt ved sammenlegging, er den framtidige
                trygdetiden allerede avkortet, og artikkelen skal ikke anvendes.
              </HelpText>
            </HStack>
            <>
              <HjemmelLenke
                tittel="Nordisk konvensjon artikkel 9"
                lenke="https://lovdata.no/pro/traktat/2012-06-12-18/ARTIKKEL_9"
              />
              <ControlledRadioGruppe
                legend="Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?"
                hideLegend
                control={control}
                errorVedTomInput="Du må svare ja eller nei."
                size="small"
                name="nordiskTrygdeAvtale"
                radios={
                  <>
                    <Radio value={JaNei.JA}>Ja</Radio>
                    <Radio value={JaNei.NEI}>Nei</Radio>
                  </>
                }
              />
              {watch('nordiskTrygdeAvtale') === JaNei.JA && (
                <Alert variant="info" size="small" inline>
                  Fremtidig trygdetid skal avkortes. Gjenny støtter ikke dette. Du må derfor beregne fremtidig trygdetid
                  manuelt, og beregning av ytelsen må manuelt overstyres. Formel: Avkortet framtidig trygdetid =
                  Framtidig trygdetid x norsk faktisk trygdetid/samlet faktisk trygdetid i de nordiske land som beregner
                  framtidig trygdetid (maks. 40 år).
                </Alert>
              )}
            </>
          </VStack>
          <Box>
            <Textarea label="Kommentar" {...register('nordiskTrygdeAvtaleKommentar')} size="small" autoComplete="off" />
          </Box>
        </HGrid>

        {isFailureHandler({
          apiResult: lagreTrygdeavtaleRequest,
          errorMessage: 'En feil har oppstått ved lagring av trygdeavtale for behandlingen',
        })}

        <HStack gap="4">
          {trygdeavtale && (
            <Button size="small" type="button" variant="secondary" icon={<XMarkIcon aria-hidden />} onClick={avbryt}>
              Avbryt
            </Button>
          )}
          <Button size="small" loading={isPending(lagreTrygdeavtaleRequest)} type="submit" icon={<FloppydiskIcon />}>
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
