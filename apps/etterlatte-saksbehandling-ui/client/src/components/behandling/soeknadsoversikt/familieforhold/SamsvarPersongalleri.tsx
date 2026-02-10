import { MismatchPersongalleri, PersongalleriSamsvar } from '~shared/types/grunnlag'
import { Alert, BodyShort, Box, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import styled from 'styled-components'
import { PersonNavn, PersonUtenIdent, relativPersonrolleTekst } from '~shared/types/Person'
import { UstiletListe } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import { formaterKanskjeStringDato, formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'
import { useBehandling } from '~components/behandling/useBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersongalleriSamsvar } from '~shared/api/grunnlag'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { SakType } from '~shared/types/sak'
import { mapApiResult } from '~shared/api/apiUtils'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { ILand } from '~utils/kodeverk'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'

export function formaterKanskjeNavn(navn: Partial<PersonNavn>) {
  return [navn.fornavn, navn.mellomnavn, navn.etternavn].filter((navn) => !!navn).join(' ')
}

function PersonerUtenIdenterVisning(props: { saktype: SakType; personer: Array<PersonUtenIdent>; landListe: ILand[] }) {
  const { personer, saktype } = props
  if (personer.length === 0) {
    return <BodyShort spacing>Ingen.</BodyShort>
  }

  return (
    <PersonUtenIdentWrapper>
      {personer.map((person, index) => (
        <Box paddingInline="space-4" key={index}>
          <UstiletListe>
            <li>Rolle: {relativPersonrolleTekst[saktype][person.rolle]}</li>
            <li>Navn: {person.person.navn ? formaterKanskjeNavn(person.person.navn) || 'Ukjent' : 'Ukjent'} </li>
            <li>Kjønn: {person.person.kjoenn ?? 'Ukjent'} </li>
            <li>Fødselsdato: {formaterKanskjeStringDatoMedFallback('Ukjent', person.person.foedselsdato)} </li>
            <li>
              Statsborgerskap:{' '}
              {!!person.person.statsborgerskap
                ? finnLandSomTekst(person.person.statsborgerskap, props.landListe)
                : 'Ukjent'}
            </li>
          </UstiletListe>
        </Box>
      ))}
    </PersonUtenIdentWrapper>
  )
}

const formaterListeMedIdenter = (identer?: string[]): string => {
  if (!identer || identer.length === 0) {
    return 'Ingen'
  }
  return identer.join(', ')
}

const PersonUtenIdentWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
`

const erAvvikRelevantForSaktype = (avvik: MismatchPersongalleri, sakType: SakType) => {
  switch (avvik) {
    case 'ENDRET_SOEKER_FNR':
      return true
    case 'EKSTRA_AVDOED':
    case 'MANGLER_AVDOED':
      return true
    case 'EKSTRA_GJENLEVENDE':
    case 'MANGLER_GJENLEVENDE':
      return sakType === SakType.BARNEPENSJON
    case 'EKSTRA_SOESKEN':
    case 'HAR_PERSONER_UTEN_IDENTER':
    // Håndteres separat fra person-avvik
    case 'MANGLER_SOESKEN':
      return false
  }
}

function VisSamsvarPersongalleri(props: { samsvar: PersongalleriSamsvar; saktype: SakType; landListe: ILand[] }) {
  const { samsvar, saktype } = props
  const personerUtenIdenterSak = samsvar.persongalleri?.personerUtenIdent ?? []
  const personerUtenIdenterPdl = samsvar.persongalleriPdl?.personerUtenIdent ?? []

  const endretSoeker = samsvar.problemer.includes('ENDRET_SOEKER_FNR')
  const harPersonerUtenIdenter = samsvar.problemer.includes('HAR_PERSONER_UTEN_IDENTER')
  const harAvvikMotPdl = samsvar.problemer.filter((avvik) => erAvvikRelevantForSaktype(avvik, saktype)).length > 0

  if (samsvar.problemer.length === 0) {
    return null
  }

  if (endretSoeker) {
    return (
      <BredAlert variant="error">
        <BodyShort spacing>
          Søkers identifikator er forskjellig i behandling og PDL. Dette kan f.eks. komme av at bruker har gått fra
          D-nummer til fødselsnummer.
        </BodyShort>

        <HStack gap="space-4">
          <VStack gap="space-2">
            <Heading level="4" size="xsmall">
              Ident i behandlingen
            </Heading>

            <Info label="Søker" tekst={samsvar.persongalleri.soeker} />
            <Info
              label="Kilde"
              tekst={`${samsvar.kilde?.type.toUpperCase()} (${formaterKanskjeStringDato(samsvar.kilde?.tidspunkt)})`}
            />
          </VStack>

          <VStack gap="space-2">
            <Heading level="4" size="xsmall">
              Ident i PDL
            </Heading>

            <Info label="Søker" tekst={samsvar.persongalleriPdl?.soeker || 'mangler'} />
            <Info
              label="Kilde"
              tekst={`${samsvar.kildePdl?.type.toUpperCase()} (${formaterKanskjeStringDato(samsvar.kildePdl?.tidspunkt)})`}
            />
          </VStack>
        </HStack>
      </BredAlert>
    )
  }

  return (
    <>
      {harAvvikMotPdl && (
        <BredAlert variant="warning">
          {saktype === SakType.BARNEPENSJON ? (
            <BodyShort spacing>
              Det er forskjeller mellom familieforholdet i behandlingen og det familieforholdet vi utleder ut i fra PDL.
              Se nøye over og eventuelt korriger persongalleriet ved å redigere.
            </BodyShort>
          ) : (
            <BodyShort spacing>
              Familieforhold må kontrolleres fordi det er avvik mellom registrert informasjon i behandlingen og det som
              er registrert i PDL. Merk at PDL kan ha mangler i informasjon om samboerskap.
            </BodyShort>
          )}
          <VStack gap="space-4">
            <VStack gap="space-2">
              <Heading level="4" size="xsmall">
                Familieforholdet i behandlingen
              </Heading>
              <HStack gap="space-4">
                <Info label="Avdøde" tekst={formaterListeMedIdenter(samsvar.persongalleri.avdoed)} />
                {saktype === SakType.BARNEPENSJON && (
                  <Info label="Gjenlevende" tekst={formaterListeMedIdenter(samsvar.persongalleri.gjenlevende)} />
                )}
                <Info
                  label="Kilde"
                  tekst={`${samsvar.kilde?.type.toUpperCase()} (${formaterKanskjeStringDato(samsvar.kilde?.tidspunkt)})`}
                />
              </HStack>
            </VStack>

            <VStack gap="space-4">
              <Heading level="4" size="xsmall">
                Familieforholdet i PDL
              </Heading>
              <HStack gap="space-4">
                <Info label="Avdøde" tekst={formaterListeMedIdenter(samsvar.persongalleriPdl?.avdoed)} />
                {saktype === SakType.BARNEPENSJON && (
                  <Info label="Gjenlevende" tekst={formaterListeMedIdenter(samsvar.persongalleriPdl?.gjenlevende)} />
                )}
                <Info
                  label="Kilde"
                  tekst={`${samsvar.kildePdl?.type.toUpperCase()} (${formaterKanskjeStringDato(samsvar.kildePdl?.tidspunkt)})`}
                />
              </HStack>
            </VStack>
          </VStack>
        </BredAlert>
      )}

      {harPersonerUtenIdenter && (
        <div>
          <BredAlert variant="warning">
            Det er personer uten identer i PDL i saksgrunnlaget. Disse personene kan ikke brukes i beregning av
            trygdetid eller en eventuell søskenjustering på gammelt regelverk.
          </BredAlert>
          {personerUtenIdenterSak.length > 0 && (
            <>
              <Heading size="small" level="4">
                Personer uten identer i saksgrunnlag:
              </Heading>
              <PersonerUtenIdenterVisning
                saktype={saktype}
                personer={personerUtenIdenterSak}
                landListe={props.landListe}
              />
            </>
          )}
          <Heading size="small" level="4">
            Personer uten identer i PDL:
          </Heading>
          <PersonerUtenIdenterVisning saktype={saktype} personer={personerUtenIdenterPdl} landListe={props.landListe} />
        </div>
      )}
    </>
  )
}

const BredAlert = styled(Alert)`
  width: fit-content;
`

export function SamsvarPersongalleri({ landListe }: { landListe: ILand[] }) {
  const behandling = useBehandling()
  const [samsvarPersongalleri, fetchSamsvarPersongalleri] = useApiCall(hentPersongalleriSamsvar)

  useEffect(() => {
    if (behandling?.id) {
      fetchSamsvarPersongalleri({ behandlingId: behandling.id })
    }
  }, [behandling?.id])

  if (!behandling) {
    return null
  }

  return mapApiResult(
    samsvarPersongalleri,
    <Spinner label="Henter samsvar persongalleri" />,
    (error) => <ApiErrorAlert>Kunne ikke hente samsvar persongalleri: {error.detail}</ApiErrorAlert>,
    (samsvarPersongalleri) => (
      <VisSamsvarPersongalleri saktype={behandling.sakType} samsvar={samsvarPersongalleri} landListe={landListe} />
    )
  )
}
