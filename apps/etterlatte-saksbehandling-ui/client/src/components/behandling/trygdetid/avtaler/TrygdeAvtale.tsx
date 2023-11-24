import { HandshakeIcon } from '@navikt/aksel-icons'
import { Alert, BodyShort, Button, Heading, HelpText, Radio, RadioGroup, Select, Textarea } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexHeader, IconWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
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
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { IconSize } from '~shared/types/Icon'
import { FormWrapper } from '../styled'
import { TrygdeavtaleVisning } from './TrygdeavtaleVisning'
import { FlexRow } from '~shared/styled'
import { JaNei } from '~shared/types/ISvar'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { Text } from '~components/behandling/attestering/styled'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'

interface TrygdetidAvtaleOptionProps {
  defaultBeskrivelse: string
  trygdeavtaleOptions: TrygdetidAvtaleOptions[]
}

const TrygdetidAvtaleOptions = ({ defaultBeskrivelse, trygdeavtaleOptions }: TrygdetidAvtaleOptionProps) => {
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
    <TrygdeAvtaleWrapper>
      <FlexHeader>
        <IconWrapper>
          <HandshakeIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size="small" level="3">
          Vurdering av trygdeavtale (Avdød)
        </Heading>
      </FlexHeader>

      {!redigering && avtalerListe && avtaleKriterierListe && (
        <>
          <TrygdeavtaleVisning avtaler={avtalerListe} kriterier={avtaleKriterierListe} trygdeavtale={trygdeavtale} />
          {redigerbar && (
            <FlexRow $spacing>
              <Button size="small" onClick={rediger} type="button">
                Rediger
              </Button>
            </FlexRow>
          )}
        </>
      )}

      {redigerbar && redigering && avtalerListe && avtaleKriterierListe && (
        <>
          <TrygdeAvtaleForm>
            <FlexRow $spacing>
              <FormWrapper>
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
                  <TrygdetidAvtaleOptions defaultBeskrivelse="Velg avtale" trygdeavtaleOptions={avtalerListe} />
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
                    <TrygdetidAvtaleOptions
                      defaultBeskrivelse="Velg avtaledato"
                      trygdeavtaleOptions={valgtAvtale.datoer}
                    />
                  </Select>
                )}
              </FormWrapper>
            </FlexRow>
            <FlexRow $spacing>
              <AvtaleColumn>
                <FlexRow $spacing>
                  <FormWrapper>
                    <Select
                      label="Kriterier for å omfattes av avtalen"
                      autoComplete="off"
                      value={trygdeavtale.avtaleKriteriaKode}
                      onChange={(e) => {
                        setTrygdeavtale({ ...trygdeavtale, avtaleKriteriaKode: e.target.value })
                      }}
                    >
                      <TrygdetidAvtaleOptions
                        defaultBeskrivelse="Velg kriteria"
                        trygdeavtaleOptions={avtaleKriterierListe}
                      />
                    </Select>
                  </FormWrapper>
                </FlexRow>
              </AvtaleColumn>
              <AvtaleColumn>
                <FlexRow $spacing>
                  <Heading size="xsmall" spacing={false}>
                    Er avdøde i personkretsen i denne avtalen?
                  </Heading>
                </FlexRow>
                <FlexRow $spacing>
                  <RadioGroupWrapper>
                    <RadioGroup
                      legend="Er avdøde i personkretsen i denne avtalen?"
                      hideLegend={true}
                      size="small"
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
                  </RadioGroupWrapper>
                </FlexRow>
              </AvtaleColumn>
            </FlexRow>
            <FlexRow $spacing>
              <FormWrapper>
                <AvtaleColumn>
                  <FlexHeader>
                    <Heading size="xsmall" spacing={false}>
                      Er arbeidsinntekt i avtaleland på minst 1 G på dødstidspunktet?
                    </Heading>
                    <HelpText strategy="fixed">
                      Poengår (år med arbeidsinntekt på mer enn 1 G) i andre EØS-land medregnes som poengår, forutsatt
                      at det ikke er tjent opp poengår i Norge i året. Hvis «Ja» gir det rett til fremtidige poeng,
                      eller fremtidig trygdetid, ved en prorata beregning. Hvis «Nei» gir det ikke rett til dette.
                    </HelpText>
                  </FlexHeader>
                  <BodyShort>
                    <HjemmelLenke
                      tittel="Rundskriv til hovednummer 45 kap. 3 punkt 3.3.2"
                      lenke="https://lovdata.no/pro/rundskriv/r45-00/KAPITTEL_3-3-2-1"
                    />
                  </BodyShort>
                  <RadioGroupWrapper>
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
                      <div className="flex">
                        <Radio value={JaNei.JA}>Ja</Radio>
                        <Radio value={JaNei.NEI}>Nei</Radio>
                      </div>
                    </RadioGroup>
                  </RadioGroupWrapper>
                  {trygdeavtale.arbInntekt1G === JaNei.NEI && (
                    <Alert variant="info" size="small" inline>
                      Det gis ikke rett til fremtidig trygdetid fra utland ved en prorata beregning. Hvis det heller
                      ikke er rett til fremtidig trygdetid etter nasjonale regler, må du ta bort registrert fremtidig
                      trygdetid.
                    </Alert>
                  )}
                </AvtaleColumn>
                <AvtaleColumn>
                  <FlexRow $spacing>
                    <Text>Kommentar (valgfri)</Text>
                  </FlexRow>
                  <FlexRow $spacing>
                    <Textarea
                      style={{ padding: '10px' }}
                      label="Kommentar"
                      hideLabel={true}
                      value={trygdeavtale.arbInntekt1GKommentar}
                      onChange={() =>
                        setTrygdeavtale({ ...trygdeavtale, arbInntekt1GKommentar: trygdeavtale.arbInntekt1GKommentar })
                      }
                      minRows={2}
                      size="small"
                      autoComplete="off"
                    />
                  </FlexRow>
                </AvtaleColumn>
              </FormWrapper>
            </FlexRow>
            <FlexRow $spacing>
              <FormWrapper>
                <AvtaleColumn>
                  <FlexHeader>
                    <Heading size="xsmall" spacing={false}>
                      Beregning etter artikkel 50 (EØS-forordning 883/2004)?
                    </Heading>
                    <HelpText strategy="fixed">
                      Denne artikkelen skal anvendes hvis det foreligger pensjonsrett i minst to EØS-land i tillegg til
                      Norge, og hvis vilkårene for pensjon ikke er oppfylt i alle EØS-landene avdøde har opptjening i.
                      Det skal gjøres en alternativ prorata-beregning med trygdetid kun for de EØS-landene der rett til
                      pensjon er oppfylt. Dette er fordi trygdetid fra land der vilkårene ikke er oppfylte ikke skal
                      medregnes hvis det ikke lønner seg.
                    </HelpText>
                  </FlexHeader>
                  <BodyShort>
                    <HjemmelLenke
                      tittel="EØS-forordning 883/2004 artikkel 50"
                      lenke="https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_50"
                    />
                  </BodyShort>
                  <RadioGroupWrapper>
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
                      <div className="flex">
                        <Radio value={JaNei.JA}>Ja</Radio>
                        <Radio value={JaNei.NEI}>Nei</Radio>
                      </div>
                    </RadioGroup>
                  </RadioGroupWrapper>
                  {trygdeavtale.beregArt50 === JaNei.JA && (
                    <Alert variant="info" size="small" inline>
                      Ta en alternativ prorata-beregning. Huk av for «Ikke i prorata» på trygdetidsperioder for EØS-land
                      som har gitt avslag på ytelse.
                    </Alert>
                  )}
                </AvtaleColumn>
                <AvtaleColumn>
                  <FlexRow $spacing>
                    <Text>Kommentar (valgfri)</Text>
                  </FlexRow>
                  <FlexRow $spacing>
                    <Textarea
                      style={{ padding: '10px' }}
                      label="Kommentar fra attestant"
                      hideLabel={true}
                      value={trygdeavtale.beregArt50Kommentar}
                      onChange={() =>
                        setTrygdeavtale({ ...trygdeavtale, beregArt50Kommentar: trygdeavtale.beregArt50Kommentar })
                      }
                      minRows={2}
                      size="small"
                      autoComplete="off"
                    />
                  </FlexRow>
                </AvtaleColumn>
              </FormWrapper>
            </FlexRow>
            <FlexRow $spacing>
              <FormWrapper>
                <AvtaleColumn>
                  <FlexHeader>
                    <Heading size="xsmall">
                      Nordisk trygdeavtale: Skal artikkel 9 anvendes - fremtidig trygdetid avkortes?
                    </Heading>
                    <HelpText strategy="fixed">
                      Hvis forutgående medlemskap, og derav vilkår for å beregne framtidig trygdetid, er oppfylt etter
                      nasjonale regler i Sverige og/eller Island i tillegg til Norge, skal framtidig trygdetid avkortes.
                      I en prorata-beregnet ytelse, der forutgående medlemskap er oppfylt ved sammenlegging, er den
                      framtidige trygdetiden allerede avkortet, og artikkelen skal ikke anvendes.
                    </HelpText>
                  </FlexHeader>
                  <BodyShort>
                    <HjemmelLenke
                      tittel="Nordisk konvensjon artikkel 9"
                      lenke="https://lovdata.no/pro/traktat/2012-06-12-18/ARTIKKEL_9"
                    />
                  </BodyShort>
                  <RadioGroupWrapper>
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
                      <div className="flex">
                        <Radio value={JaNei.JA}>Ja</Radio>
                        <Radio value={JaNei.NEI}>Nei</Radio>
                      </div>
                    </RadioGroup>
                  </RadioGroupWrapper>
                  {trygdeavtale.nordiskTrygdeAvtale === JaNei.JA && (
                    <Alert variant="info" size="small" inline>
                      Fremtidig trygdetid skal avkortes. Gjenny støtter ikke dette. Du må derfor beregne fremtidig
                      trygdetid manuelt, og beregning av ytelsen må manuelt overstyres. Formel: Avkortet framtidig
                      trygdetid = Framtidig trygdetid x norsk faktisk trygdetid/samlet faktisk trygdetid i de nordiske
                      land som beregner framtidig trygdetid (maks. 40 år).
                    </Alert>
                  )}
                </AvtaleColumn>
                <AvtaleColumn>
                  <FlexRow $spacing>
                    <Text>Kommentar (valgfri)</Text>
                  </FlexRow>
                  <FlexRow $spacing>
                    <Textarea
                      style={{ padding: '10px' }}
                      label="Kommentar fra attestant"
                      hideLabel={true}
                      value={trygdeavtale.nordiskTrygdeAvtaleKommentar}
                      onChange={() =>
                        setTrygdeavtale({
                          ...trygdeavtale,
                          nordiskTrygdeAvtaleKommentar: trygdeavtale.nordiskTrygdeAvtaleKommentar,
                        })
                      }
                      minRows={2}
                      size="small"
                      autoComplete="off"
                    />
                  </FlexRow>
                </AvtaleColumn>
              </FormWrapper>
            </FlexRow>
            <FlexRow $spacing>
              <Button size="small" loading={isPending(lagreTrygdeavtaleRequest)} type="button" onClick={lagre}>
                Lagre
              </Button>
              {trygdeavtale && (
                <Button size="small" onClick={avbryt} type="button">
                  Avbryt
                </Button>
              )}
            </FlexRow>
          </TrygdeAvtaleForm>
        </>
      )}
      {(isPending(hentAlleTrygdetidAvtalerRequest) ||
        isPending(hentAlleTrygdetidAvtalerKriterierRequest) ||
        isPending(hentTrygdeavtaleRequest)) && <Spinner visible={true} label="Henter trgydeavtaler" />}
      {isFailure(hentAlleTrygdetidAvtalerRequest) && (
        <ApiErrorAlert>En feil har oppstått ved henting av trygdeavtaler</ApiErrorAlert>
      )}
      {isFailure(hentAlleTrygdetidAvtalerKriterierRequest) && (
        <ApiErrorAlert>En feil har oppstått ved henting av trygdeavtalekriterier</ApiErrorAlert>
      )}
      {isFailure(hentTrygdeavtaleRequest) && (
        <ApiErrorAlert>En feil har oppstått ved henting av trygdeavtale for behandlingen</ApiErrorAlert>
      )}
      {isFailure(lagreTrygdeavtaleRequest) && (
        <ApiErrorAlert>En feil har oppstått ved lagring av trygdeavtale for behandlingen</ApiErrorAlert>
      )}
    </TrygdeAvtaleWrapper>
  )
}

const TrygdeAvtaleWrapper = styled.div`
  margin-top: 2em;
`

const TrygdeAvtaleForm = styled.form`
  display: flex;
  flex-direction: column;
`

const AvtaleColumn = styled.div`
  min-width: 30rem;
  padding-top: 20px;
  &:nth-child(2) {
    flex-grow: 1;
    border-right: 0px solid #c6c2bf;
    border-left: 0px solid #c6c2bf;
    width: 800px;
    min-width: 800px;
    padding-top: 20px;
  }
`
