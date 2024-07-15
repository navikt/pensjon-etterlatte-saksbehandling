import { HandshakeIcon } from '@navikt/aksel-icons'
import {
  Alert,
  Box,
  Button,
  Heading,
  HelpText,
  HGrid,
  HStack,
  Radio,
  RadioGroup,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
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
  const [lagreTrygdeavtaleRequest, lagreTrygdeavtale] = useApiCall(lagreTrygdeavtaleForBehandling)
  const [hentTrygdeavtaleRequest, fetchTrygdeavtale] = useApiCall(hentTrygdeavtaleForBehandling)
  const [avtalerListe, setAvtalerListe] = useState<TrygdetidAvtale[]>()
  const [avtaleKriterierListe, setAvtaleKriterierListe] = useState<TrygdetidAvtaleKriteria[]>()
  const [trygdeavtale, setTrygdeavtale] = useState<Trygdeavtale>({} as Trygdeavtale)
  const [valgtAvtale, setValgtAvtale] = useState<TrygdetidAvtale>()
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

  const avbryt = () => {
    if (redigering) {
      setRedigering(false)
    }
  }

  const rediger = () => {
    if (!redigering) {
      velgAvtale(trygdeavtale?.avtaleKode)
      setRedigering(true)
    }
  }

  const lagre = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    lagreTrygdeavtale(
      {
        behandlingId,
        avtaleRequest: {
          avtaleKode: trygdeavtale.avtaleKode,
          avtaleDatoKode: trygdeavtale.avtaleDatoKode,
          avtaleKriteriaKode: trygdeavtale.avtaleKriteriaKode,
          personKrets: trygdeavtale.personKrets,
          arbInntekt1G: trygdeavtale.arbInntekt1G,
          arbInntekt1GKommentar: trygdeavtale.arbInntekt1GKommentar,
          beregArt50: trygdeavtale.beregArt50,
          beregArt50Kommentar: trygdeavtale.beregArt50Kommentar,
          nordiskTrygdeAvtale: trygdeavtale.nordiskTrygdeAvtale,
          nordiskTrygdeAvtaleKommentar: trygdeavtale.nordiskTrygdeAvtaleKommentar,
          id: trygdeavtale.id,
        } as TrygdeavtaleRequest,
      },
      (respons) => {
        setTrygdeavtale(respons)
        setRedigering(false)
      }
    )
  }

  const velgAvtale = (kode?: string) => {
    if (kode && avtalerListe) {
      setValgtAvtale(avtalerListe.find((avtale) => avtale.kode === kode))
    } else {
      setValgtAvtale(undefined)
    }
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

        {!redigering && avtalerListe && avtaleKriterierListe && (
          <>
            <TrygdeavtaleVisning avtaler={avtalerListe} kriterier={avtaleKriterierListe} trygdeavtale={trygdeavtale} />
            {redigerbar && (
              <div>
                <Button size="small" onClick={rediger} type="button">
                  Rediger
                </Button>
              </div>
            )}
          </>
        )}
        {isSuccess(hentAlleTrygdetidAvtalerRequest) &&
          isSuccess(hentAlleTrygdetidAvtalerKriterierRequest) &&
          isSuccess(hentTrygdeavtaleRequest) &&
          redigerbar &&
          redigering && (
            <form>
              <VStack gap="4">
                <HStack gap="4">
                  <Select
                    label="Avtale"
                    autoComplete="off"
                    value={trygdeavtale.avtaleKode}
                    onChange={(e) => {
                      if (e.target.value) {
                        setTrygdeavtale({ ...trygdeavtale, avtaleKode: e.target.value })
                      }
                      velgAvtale(e.target.value)
                    }}
                  >
                    <TrygdetidAvtaleOptionsView
                      defaultBeskrivelse="Velg avtale"
                      trygdeavtaleOptions={hentAlleTrygdetidAvtalerRequest.data}
                    />
                  </Select>
                  {valgtAvtale && valgtAvtale.datoer.length > 0 && (
                    <Select
                      label="Dato"
                      autoComplete="off"
                      value={trygdeavtale.avtaleDatoKode}
                      onChange={(e) => {
                        setTrygdeavtale({ ...trygdeavtale, avtaleDatoKode: e.target.value })
                      }}
                    >
                      <TrygdetidAvtaleOptionsView
                        defaultBeskrivelse="Velg avtaledato"
                        trygdeavtaleOptions={valgtAvtale.datoer}
                      />
                    </Select>
                  )}
                </HStack>

                <Select
                  label="Kriterier for å omfattes av avtalen"
                  autoComplete="off"
                  value={trygdeavtale.avtaleKriteriaKode}
                  onChange={(e) => {
                    setTrygdeavtale({ ...trygdeavtale, avtaleKriteriaKode: e.target.value })
                  }}
                >
                  <TrygdetidAvtaleOptionsView
                    defaultBeskrivelse="Velg kriteria"
                    trygdeavtaleOptions={hentAlleTrygdetidAvtalerKriterierRequest.data}
                  />
                </Select>

                <RadioGroup
                  legend="Er avdøde i personkretsen i denne avtalen?"
                  className="radioGroup"
                  onChange={(event) => {
                    setTrygdeavtale({ ...trygdeavtale, personKrets: event as JaNei })
                  }}
                  value={trygdeavtale.personKrets || ''}
                >
                  <div className="flex">
                    <Radio value={JaNei.JA}>Ja</Radio>
                    <Radio value={JaNei.NEI}>Nei</Radio>
                  </div>
                </RadioGroup>

                <HGrid gap="8 4" columns="60% 40%">
                  <VStack gap="2">
                    <HStack gap="2">
                      <Heading size="xsmall">Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet?</Heading>
                      <HelpText>
                        Poengår (år med arbeidsinntekt på mer enn 1 G) i andre EØS-land medregnes som poengår, forutsatt
                        at det ikke er tjent opp poengår i Norge i året. Hvis «Ja» gir det rett til fremtidige poeng,
                        eller fremtidig trygdetid, ved en prorata beregning. Hvis «Nei» gir det ikke rett til dette.
                      </HelpText>
                    </HStack>
                    <>
                      <HjemmelLenke
                        tittel="Rundskriv til hovednummer 45 kap. 3 punkt 3.3.2"
                        lenke="https://lovdata.no/pro/rundskriv/r45-00/KAPITTEL_3-3-2-1"
                      />
                      <RadioGroup
                        legend="Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet?"
                        hideLegend={true}
                        size="small"
                        className="radioGroup"
                        onChange={(event) => {
                          setTrygdeavtale({ ...trygdeavtale, arbInntekt1G: event as JaNei })
                        }}
                        value={trygdeavtale.arbInntekt1G || ''}
                      >
                        <HStack gap="8">
                          <Radio value={JaNei.JA}>Ja</Radio>
                          <Radio value={JaNei.NEI}>Nei</Radio>
                        </HStack>
                      </RadioGroup>
                      {trygdeavtale.arbInntekt1G === JaNei.NEI && (
                        <Alert variant="info" size="small" inline>
                          Det gis ikke rett til fremtidig trygdetid fra utland ved en prorata beregning. Hvis det heller
                          ikke er rett til fremtidig trygdetid etter nasjonale regler, må du ta bort registrert
                          fremtidig trygdetid.
                        </Alert>
                      )}
                    </>
                  </VStack>
                  <Textarea
                    label="Kommentar"
                    value={trygdeavtale.arbInntekt1GKommentar}
                    onChange={(e) => setTrygdeavtale({ ...trygdeavtale, arbInntekt1GKommentar: e.target.value })}
                    minRows={2}
                    autoComplete="off"
                    size="small"
                  />

                  <VStack gap="2">
                    <HStack gap="2">
                      <Heading size="xsmall">Beregning etter artikkel 50 (EØS-forordning 883/2004)?</Heading>
                      <HelpText>
                        Denne artikkelen skal anvendes hvis det foreligger pensjonsrett i minst to EØS-land i tillegg
                        til Norge, og hvis vilkårene for pensjon ikke er oppfylt i alle EØS-landene avdøde har
                        opptjening i. Det skal gjøres en alternativ prorata-beregning med trygdetid kun for de
                        EØS-landene der rett til pensjon er oppfylt. Dette er fordi trygdetid fra land der vilkårene
                        ikke er oppfylte ikke skal medregnes hvis det ikke lønner seg.
                      </HelpText>
                    </HStack>
                    <>
                      <HjemmelLenke
                        tittel="EØS-forordning 883/2004 artikkel 50"
                        lenke="https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_50"
                      />
                      <RadioGroup
                        legend="Beregning etter artikkel 50 (EØS-forordning 883/2004)?"
                        hideLegend={true}
                        size="small"
                        className="radioGroup"
                        defaultValue={trygdeavtale.beregArt50}
                        onChange={(event) => {
                          setTrygdeavtale({ ...trygdeavtale, beregArt50: event as JaNei })
                        }}
                        value={trygdeavtale.beregArt50 || ''}
                      >
                        <HStack gap="8">
                          <Radio value={JaNei.JA}>Ja</Radio>
                          <Radio value={JaNei.NEI}>Nei</Radio>
                        </HStack>
                      </RadioGroup>
                      {trygdeavtale.beregArt50 === JaNei.JA && (
                        <Alert variant="info" size="small" inline>
                          Ta en alternativ prorata-beregning. Huk av for «Ikke i prorata» på trygdetidsperioder for
                          EØS-land som har gitt avslag på ytelse.
                        </Alert>
                      )}
                    </>
                  </VStack>
                  <Textarea
                    label="Kommentar"
                    value={trygdeavtale.beregArt50Kommentar}
                    onChange={(e) => setTrygdeavtale({ ...trygdeavtale, beregArt50Kommentar: e.target.value })}
                    minRows={2}
                    size="small"
                    autoComplete="off"
                  />

                  <VStack gap="2">
                    <HStack gap="2">
                      <Heading size="xsmall">
                        Nordisk trygdeavtale: Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?
                      </Heading>
                      <HelpText>
                        Hvis forutgående medlemskap, og derav vilkår for å beregne framtidig trygdetid, er oppfylt etter
                        nasjonale regler i Sverige og/eller Island i tillegg til Norge, skal framtidig trygdetid
                        avkortes. I en prorata-beregnet ytelse, der forutgående medlemskap er oppfylt ved sammenlegging,
                        er den framtidige trygdetiden allerede avkortet, og artikkelen skal ikke anvendes.
                      </HelpText>
                    </HStack>
                    <>
                      <HjemmelLenke
                        tittel="Nordisk konvensjon artikkel 9"
                        lenke="https://lovdata.no/pro/traktat/2012-06-12-18/ARTIKKEL_9"
                      />
                      <RadioGroup
                        legend="Nordisk trygdeavtale: Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?"
                        hideLegend={true}
                        size="small"
                        className="radioGroup"
                        onChange={(event) => {
                          setTrygdeavtale({ ...trygdeavtale, nordiskTrygdeAvtale: event as JaNei })
                        }}
                        value={trygdeavtale.nordiskTrygdeAvtale || ''}
                      >
                        <HStack gap="8">
                          <Radio value={JaNei.JA}>Ja</Radio>
                          <Radio value={JaNei.NEI}>Nei</Radio>
                        </HStack>
                      </RadioGroup>
                      {trygdeavtale.nordiskTrygdeAvtale === JaNei.JA && (
                        <Alert variant="info" size="small" inline>
                          Fremtidig trygdetid skal avkortes. Gjenny støtter ikke dette. Du må derfor beregne fremtidig
                          trygdetid manuelt, og beregning av ytelsen må manuelt overstyres. Formel: Avkortet framtidig
                          trygdetid = Framtidig trygdetid x norsk faktisk trygdetid/samlet faktisk trygdetid i de
                          nordiske land som beregner framtidig trygdetid (maks. 40 år).
                        </Alert>
                      )}
                    </>
                  </VStack>
                  <Textarea
                    label="Kommentar"
                    value={trygdeavtale.nordiskTrygdeAvtaleKommentar}
                    onChange={(e) =>
                      setTrygdeavtale({
                        ...trygdeavtale,
                        nordiskTrygdeAvtaleKommentar: e.target.value,
                      })
                    }
                    minRows={2}
                    size="small"
                    autoComplete="off"
                  />
                </HGrid>

                <HStack gap="4">
                  <Button size="small" loading={isPending(lagreTrygdeavtaleRequest)} type="button" onClick={lagre}>
                    Lagre
                  </Button>
                  {trygdeavtale && (
                    <Button size="small" onClick={avbryt} type="button" variant="secondary">
                      Avbryt
                    </Button>
                  )}
                </HStack>
              </VStack>
            </form>
          )}
        {(isPending(hentAlleTrygdetidAvtalerRequest) ||
          isPending(hentAlleTrygdetidAvtalerKriterierRequest) ||
          isPending(hentTrygdeavtaleRequest)) && <Spinner visible={true} label="Henter trgydeavtaler" />}

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
        {isFailureHandler({
          apiResult: lagreTrygdeavtaleRequest,
          errorMessage: 'En feil har oppstått ved lagring av trygdeavtale for behandlingen',
        })}
      </VStack>
    </Box>
  )
}
