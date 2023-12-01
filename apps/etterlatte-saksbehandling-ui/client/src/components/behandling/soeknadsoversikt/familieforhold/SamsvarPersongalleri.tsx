import { PersongalleriSamsvar } from '~shared/types/grunnlag'
import { Alert, BodyShort, Box, Heading } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import styled from 'styled-components'
import { PersonNavn, PersonUtenIdent, relativPersonrolleTekst } from '~shared/types/Person'
import { UstiletListe } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'
import { useBehandling } from '~components/behandling/useBehandling'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentPersongalleriSamsvar } from '~shared/api/grunnlag'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { SakType } from '~shared/types/sak'

function formaterKanskjeNavn(navn: Partial<PersonNavn>) {
  return [navn.fornavn, navn.mellomnavn, navn.etternavn].filter((navn) => !!navn).join(' ')
}

function PersonerUtenIdenterVisning(props: { saktype: SakType; personer: Array<PersonUtenIdent> }) {
  const { personer, saktype } = props
  if (personer.length === 0) {
    return <BodyShort spacing>Ingen.</BodyShort>
  }

  return (
    <PersonUtenIdentWrapper>
      {personer.map((person, index) => (
        <Box background="surface-alt-3-subtle" paddingInline="4" key={index}>
          <UstiletListe>
            <li>Rolle: {relativPersonrolleTekst[saktype][person.rolle]}</li>
            <li>Navn: {person.person.navn ? formaterKanskjeNavn(person.person.navn) || 'Ukjent' : 'Ukjent'} </li>
            <li>Kjønn: {person.person.kjoenn ?? 'Ukjent'} </li>
            <li>Fødselsdato: {formaterKanskjeStringDatoMedFallback('Ukjent', person.person.foedselsdato)} </li>
            <li>Statsborgerskap: {person.person.statsborgerskap ?? 'Ukjent'}</li>
          </UstiletListe>
        </Box>
      ))}
    </PersonUtenIdentWrapper>
  )
}

const PersonUtenIdentWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
`

// TODO: Håndter liste over avvik mellom persongalleri i PDL og det vi har i søknaden
function VisSamsvarPersongalleri(props: { samsvar: PersongalleriSamsvar; saktype: SakType }) {
  const { samsvar, saktype } = props
  const personerUtenIdenterSak = samsvar.persongalleri?.personerUtenIdent ?? []
  const personerUtenIdenterPdl = samsvar.persongalleriPdl?.personerUtenIdent ?? []
  const harPersonerUtenIdenter = personerUtenIdenterPdl.length > 0 || personerUtenIdenterSak.length > 0

  if (!harPersonerUtenIdenter) {
    return null
  }

  return (
    <div>
      <Alert variant="warning">
        Det er personer uten identer i PDL i saksgrunnlaget. Disse personene kan ikke brukes i beregning av trygdetid
        eller en eventuell søskenjustering på gammelt regelverk.
      </Alert>
      <Heading size="small" level="4">
        Personer uten identer i saksgrunnlag:
      </Heading>
      <PersonerUtenIdenterVisning saktype={saktype} personer={personerUtenIdenterSak} />

      <Heading size="small" level="4">
        Personer uten identer i PDL:
      </Heading>
      <PersonerUtenIdenterVisning saktype={saktype} personer={personerUtenIdenterPdl} />
    </div>
  )
}

export function SamsvarPersongalleri() {
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
    <Spinner label="Henter samsvar persongalleri" visible />,
    (error) => <ApiErrorAlert>Kunne ikke hente samsvar persongalleri: {error.detail}</ApiErrorAlert>,
    (samsvarPersongalleri) => <VisSamsvarPersongalleri saktype={behandling.sakType} samsvar={samsvarPersongalleri} />
  )
}
