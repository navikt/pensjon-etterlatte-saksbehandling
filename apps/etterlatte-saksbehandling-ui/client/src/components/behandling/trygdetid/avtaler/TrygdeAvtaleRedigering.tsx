import {
  lagreTrygdeavtaleForBehandling,
  Trygdeavtale,
  TrygdeavtaleRequest,
  TrygdetidAvtale,
  TrygdetidAvtaleKriteria,
  TrygdetidAvtaleOptions,
} from '~shared/api/trygdetid'
import { useBehandling } from '~components/behandling/useBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useForm } from 'react-hook-form'
import {
  Alert,
  BodyLong,
  Box,
  Button,
  Heading,
  HelpText,
  HGrid,
  HStack,
  Radio,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { JaNei } from '~shared/types/ISvar'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import React, { useEffect } from 'react'

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

export function TrygdeAvtaleRedigering(props: {
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
    setValue,
    formState: { errors, isDirty },
  } = useForm<Partial<Trygdeavtale>>({
    defaultValues: trygdeavtale,
  })

  const valgtAvtaleKode = watch('avtaleKode')
  const valgtAvtale = avtaler.find((avtale) => avtale.kode === valgtAvtaleKode)

  useEffect(() => {
    // passer på å resette avtaleDatoKode hvis valgt avtale endrer seg
    if (isDirty) {
      setValue('avtaleDatoKode', undefined)
    }
  }, [valgtAvtaleKode])

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
  return (
    <form onSubmit={handleSubmit(lagreOgOppdater)}>
      <VStack gap="space-6">
        <HStack gap="space-4" align="start">
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
              {...register('avtaleDatoKode', {
                required: {
                  value: true,
                  message: 'Du må velge datoen for avtalen.',
                },
              })}
              label="Dato"
              autoComplete="off"
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
            {...register('avtaleKriteriaKode', {
              required: {
                value: true,
                message: 'Du må velge kriterier for å omfattes av avtalen',
              },
            })}
            label="Kriterier for å omfattes av avtalen"
            autoComplete="off"
            error={errors.avtaleKriteriaKode?.message}
          >
            <TrygdetidAvtaleOptionsView defaultBeskrivelse="Velg kriterie" trygdeavtaleOptions={kriterier} />
          </Select>
        </Box>

        <VStack gap="space-2">
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

        <HGrid gap="space-8 space-4" columns="60% 40%">
          <VStack gap="space-2">
            <HStack gap="space-2">
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
              {...register('arbInntekt1GKommentar')}
              label="Kommentar"
              minRows={2}
              autoComplete="off"
              size="small"
            />
          </Box>

          <VStack gap="space-2">
            <HStack gap="space-2">
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
              {...register('beregArt50Kommentar')}
              label="Kommentar"
              minRows={2}
              size="small"
              autoComplete="off"
            />
          </Box>

          <VStack gap="space-2">
            <HStack gap="space-2">
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
                  <VStack gap="space-2">
                    <BodyLong size="small">
                      Fremtidig trygdetid skal avkortes. De automatiske beregningene av fremtidig trygdetid i Gjenny
                      støtter ikke dette. Du må derfor beregne fremtidig trygdetid manuelt og registrere en periode som
                      tilsvarer det antall år avdøde skal ha på fremtidig trygdetid. Husk å skrive om avkortet fremtidig
                      trygdetid som følge av nordisk konvensjon art. 9 under trygdetid i beregningsvedlegget. Husk å
                      skrive hvorfor du endrer i begrunnelsesfeltet under fremtidig trygdetid.
                    </BodyLong>
                    <BodyLong size="small">
                      <strong>Formel: </strong>
                      <em>
                        Avkortet framtidig trygdetid = Framtidig trygdetid x norsk faktisk trygdetid/samlet faktisk
                        trygdetid i de nordiske land som beregner framtidig trygdetid
                      </em>{' '}
                      (maks. 40 år).
                    </BodyLong>
                    <BodyLong size="small">
                      Obs! Hvis 4/5-regelen gjelder her, må du manuelt beregne avkortet fremtidig trygdetid som følge av
                      dette, før du avkorter etter artikkel 9. Vær oppmerksom på at i saker der artikkel 9 gjelder, står
                      det &quot;Nei&quot; på spørsmålet om faktisk trygdetid er mindre enn 4/5 av opptjeningstiden under
                      beregnet fremtidig trygdetid – selv om den faktisk er det.
                    </BodyLong>
                  </VStack>
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

        <HStack gap="space-4">
          {trygdeavtale && (
            <Button size="small" type="button" variant="secondary" icon={<XMarkIcon aria-hidden />} onClick={avbryt}>
              Avbryt
            </Button>
          )}
          <Button
            size="small"
            loading={isPending(lagreTrygdeavtaleRequest)}
            type="submit"
            icon={<FloppydiskIcon aria-hidden />}
          >
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
