import { Dokumentliste } from './dokumentliste'
import styled from 'styled-components'
import { useEffect } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { SidebarPanel } from '~components/behandling/SideMeny/SideMeny'
import { DokumentlisteLiten } from './dokumentlisteLiten'
import { useApiCall } from '~shared/hooks/useApiCall'

export const Dokumentoversikt = (props: { fnr: string; liten?: boolean }) => {
  const [dokumenter, hentDokumenterForBruker] = useApiCall(hentDokumenter)

  useEffect(() => void hentDokumenterForBruker(props.fnr), [props.fnr])

  return props.liten ? (
    <SidebarPanel>
      <DokumentlisteLiten dokumenter={dokumenter} />
    </SidebarPanel>
  ) : (
    <OversiktWrapper>
      <Dokumentliste dokumenter={dokumenter} />
    </OversiktWrapper>
  )
}

export const OversiktWrapper = styled.div`
  min-width: 40em;
  max-width: 70%;

  margin: 3em 1em;

  .behandlinger {
    margin-top: 5em;
  }
`
