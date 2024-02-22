import { Dokumentliste } from './Dokumentliste'
import { useEffect } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { SidebarPanel } from '~shared/components/Sidebar'
import { DokumentlisteLiten } from './DokumentlisteLiten'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Container } from '~shared/styled'

export const Dokumentoversikt = (props: { fnr: string; liten?: boolean }) => {
  const [dokumenter, hentDokumenterForBruker] = useApiCall(hentDokumenter)

  useEffect(() => void hentDokumenterForBruker(props.fnr), [props.fnr])

  return props.liten ? (
    <SidebarPanel border>
      <DokumentlisteLiten dokumenter={dokumenter} />
    </SidebarPanel>
  ) : (
    <Container>
      <Dokumentliste dokumenter={dokumenter} />
    </Container>
  )
}
