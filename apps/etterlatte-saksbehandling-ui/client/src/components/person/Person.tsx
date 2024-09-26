import React, { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { SakOversikt } from './sakOgBehandling/SakOversikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Tabs } from '@navikt/ds-react'
import { fnrHarGyldigFormat } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import {
  BellIcon,
  BriefcaseClockIcon,
  BulletListIcon,
  CogRotationIcon,
  EnvelopeClosedIcon,
  FileTextIcon,
  PersonIcon,
} from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevOversikt from '~components/person/brev/BrevOversikt'
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { isSuccess, Result } from '~shared/api/apiUtils'
import { Dokumentliste } from '~components/person/dokumenter/Dokumentliste'
import { SamordningSak } from '~components/person/SamordningSak'
import { SakMedBehandlinger } from '~components/person/typer'
import { SakType } from '~shared/types/sak'
import { Personopplysninger } from '~components/person/personopplysninger/Personopplysninger'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { Hendelser } from '~components/person/hendelser/Hendelser'
import NotatOversikt from '~components/person/notat/NotatOversikt'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { usePersonLocationState } from '~components/person/lenker/usePersonLocationState'
import { Aktivitet } from '~components/person/aktivitet/Aktivitet'

export enum PersonOversiktFane {
  PERSONOPPLYSNINGER = 'PERSONOPPLYSNINGER',
  SAKER = 'SAKER',
  DOKUMENTER = 'DOKUMENTER',
  BREV = 'BREV',
  NOTATER = 'NOTATER',
  SAMORDNING = 'SAMORDNING',
  HENDELSER = 'HENDELSER',
  AKTIVITET = 'AKTIVITET',
}

export const Person = () => {
  useSidetittel('Personoversikt')

  const [search, setSearch] = useSearchParams()
  const { fnr } = usePersonLocationState(search.get('key'))

  const [sakResult, sakFetch] = useApiCall(hentSakMedBehandlnger)
  const [fane, setFane] = useState(search.get('fane') || PersonOversiktFane.SAKER)
  const skalViseNotater = useFeatureEnabledMedDefault('notater', false)

  const velgFane = (value: string) => {
    const valgtFane = value as PersonOversiktFane

    setSearch({ fane: valgtFane }, { state: { fnr } })
    setFane(valgtFane)
  }

  useEffect(() => {
    if (fnrHarGyldigFormat(fnr)) {
      sakFetch(fnr!!)
    }
  }, [fnr])

  if (!fnrHarGyldigFormat(fnr)) {
    return <ApiErrorAlert>Fødselsnummeret {fnr} har et ugyldig format (ikke 11 siffer)</ApiErrorAlert>
  }

  const isOmstillingsstoenad = (sakStatus: Result<SakMedBehandlinger>) => {
    return isSuccess(sakStatus) && sakStatus.data.sak.sakType === SakType.OMSTILLINGSSTOENAD
  }

  return (
    <>
      <StatusBar ident={fnr} />

      <NavigerTilbakeMeny to="/">Tilbake til oppgavebenken</NavigerTilbakeMeny>

      <Tabs value={fane} onChange={velgFane}>
        <Tabs.List>
          <Tabs.Tab value={PersonOversiktFane.SAKER} label="Sak og behandling" icon={<BulletListIcon />} />
          <Tabs.Tab value={PersonOversiktFane.PERSONOPPLYSNINGER} label="Personopplysninger" icon={<PersonIcon />} />
          <Tabs.Tab value={PersonOversiktFane.HENDELSER} label="Hendelser" icon={<BellIcon />} />
          {isOmstillingsstoenad(sakResult) && (
            <Tabs.Tab value={PersonOversiktFane.AKTIVITET} label="Aktivitet" icon={<BriefcaseClockIcon />} />
          )}
          <Tabs.Tab value={PersonOversiktFane.DOKUMENTER} label="Dokumentoversikt" icon={<FileTextIcon />} />
          <Tabs.Tab value={PersonOversiktFane.BREV} label="Brev" icon={<EnvelopeClosedIcon />} />
          {skalViseNotater && <Tabs.Tab value={PersonOversiktFane.NOTATER} label="Notater" icon={<FileTextIcon />} />}
          {isOmstillingsstoenad(sakResult) && (
            <Tabs.Tab value={PersonOversiktFane.SAMORDNING} label="Samordning" icon={<CogRotationIcon />} />
          )}
        </Tabs.List>

        <Tabs.Panel value={PersonOversiktFane.SAKER}>
          <SakOversikt sakResult={sakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.PERSONOPPLYSNINGER}>
          <Personopplysninger sakResult={sakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.HENDELSER}>
          <Hendelser sakResult={sakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.DOKUMENTER}>
          <Dokumentliste sakResult={sakResult} fnr={fnr} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.BREV}>
          <BrevOversikt sakResult={sakResult} />
        </Tabs.Panel>
        {skalViseNotater && (
          <Tabs.Panel value={PersonOversiktFane.NOTATER}>
            <NotatOversikt sakResult={sakResult} />
          </Tabs.Panel>
        )}
        <Tabs.Panel value={PersonOversiktFane.SAMORDNING}>
          <SamordningSak fnr={fnr} sakResult={sakResult} />
        </Tabs.Panel>
        <Tabs.Panel value={PersonOversiktFane.AKTIVITET}>
          <Aktivitet fnr={fnr} sakResult={sakResult} />
        </Tabs.Panel>
      </Tabs>
    </>
  )
}
