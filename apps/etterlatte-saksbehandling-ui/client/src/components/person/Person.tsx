import React, { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { PdlPersonStatusBar } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { SakOversikt } from './sakOgBehandling/SakOversikt'
import Spinner from '~shared/Spinner'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Tabs } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import {
  BellIcon,
  BulletListIcon,
  CogRotationIcon,
  EnvelopeClosedIcon,
  FileTextIcon,
  PersonIcon,
} from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ApiError } from '~shared/api/apiClient'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { isSuccess, mapAllApiResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { Dokumentliste } from '~components/person/dokumenter/Dokumentliste'
import { hentPersonNavn } from '~shared/api/pdltjenester'
import { SamordningSak } from '~components/person/SamordningSak'
import { SakMedBehandlinger } from '~components/person/typer'
import { SakType } from '~shared/types/sak'
import { Personopplysninger } from '~components/person/personopplysninger/Personopplysninger'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { Hendelser } from '~components/person/hendelser/Hendelser'

export enum PersonOversiktFane {
  PERSONOPPLYSNINGER = 'PERSONOPPLYSNINGER',
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
  BREV = 'BREV',
  SAMORDNING = 'SAMORDNING',
  HENDELSER = 'HENDELSER',
}

export const Person = () => {
  useSidetittel('Personoversikt')

  const [search, setSearch] = useSearchParams()

  const [personNavnResult, personNavnFetch] = useApiCall(hentPersonNavn)
  const [sakResult, sakFetch] = useApiCall(hentSakMedBehandlnger)
  const [fane, setFane] = useState(search.get('fane') || PersonOversiktFane.SAKER)

  const velgFane = (value: string) => {
    const valgtFane = value as PersonOversiktFane

    setSearch({ fane: valgtFane })
    setFane(valgtFane)
  }

  const { fnr } = useParams()

  useEffect(() => {
    if (fnrHarGyldigFormat(fnr)) {
      personNavnFetch(fnr!!)
      sakFetch(fnr!!)
    }
  }, [fnr])

  const handleError = (error: ApiError) => {
    if (error.status === 400) {
      return <ApiErrorAlert>Ugyldig forespørsel: {error.detail}</ApiErrorAlert>
    } else {
      return <ApiErrorAlert>Feil oppsto ved henting av person med fødselsnummer {fnr}</ApiErrorAlert>
    }
  }

  if (!fnrHarGyldigFormat(fnr)) {
    return <ApiErrorAlert>Fødselsnummeret {fnr} har et ugyldig format (ikke 11 siffer)</ApiErrorAlert>
  }

  const isOmstillingsstoenad = (sakStatus: Result<SakMedBehandlinger>) => {
    return isSuccess(sakStatus) && sakStatus.data.sak.sakType === SakType.OMSTILLINGSSTOENAD
  }

  return (
    <>
      {mapSuccess(personNavnResult, (person) => (
        <PdlPersonStatusBar person={person} />
      ))}

      <NavigerTilbakeMeny label="Tilbake til oppgavebenken" path="/" />
      {mapAllApiResult(
        personNavnResult,
        <Spinner visible label="Laster personinfo ..." />,
        null,
        (error) => (
          <Container>{handleError(error)}</Container>
        ),
        (person) => (
          <Tabs value={fane} onChange={velgFane}>
            <Tabs.List>
              <Tabs.Tab value={PersonOversiktFane.SAKER} label="Sak og behandling" icon={<BulletListIcon />} />
              <Tabs.Tab
                value={PersonOversiktFane.PERSONOPPLYSNINGER}
                label="Personopplysninger"
                icon={<PersonIcon />}
              />
              <Tabs.Tab value={PersonOversiktFane.HENDELSER} label="Hendelser" icon={<BellIcon />} />
              <Tabs.Tab value={PersonOversiktFane.DOKUMENTER} label="Dokumentoversikt" icon={<FileTextIcon />} />
              <Tabs.Tab value={PersonOversiktFane.BREV} label="Brev" icon={<EnvelopeClosedIcon />} />
              {isOmstillingsstoenad(sakResult) && (
                <Tabs.Tab value={PersonOversiktFane.SAMORDNING} label="Samordning" icon={<CogRotationIcon />} />
              )}
            </Tabs.List>

            <Tabs.Panel value={PersonOversiktFane.SAKER}>
              <SakOversikt sakResult={sakResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.PERSONOPPLYSNINGER}>
              <Personopplysninger sakResult={sakResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.HENDELSER}>
              <Hendelser sakResult={sakResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.DOKUMENTER}>
              <Dokumentliste sakResult={sakResult} fnr={person.foedselsnummer} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.BREV}>
              <BrevOversikt sakResult={sakResult} />
            </Tabs.Panel>
            <Tabs.Panel value={PersonOversiktFane.SAMORDNING}>
              <SamordningSak sakResult={sakResult} />
            </Tabs.Panel>
          </Tabs>
        )
      )}
    </>
  )
}
