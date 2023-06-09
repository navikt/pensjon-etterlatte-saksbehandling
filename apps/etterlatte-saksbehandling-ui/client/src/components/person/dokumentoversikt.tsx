import { Dokumentliste } from './dokumentliste'
import styled from 'styled-components'
import { useEffect, useState } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { Journalpost } from '../behandling/types'
import { DokumentlisteLiten } from '~components/person/dokumentlisteLiten'
import { SidebarPanel } from '~components/behandling/SideMeny/SideMeny'

export const Dokumentoversikt = (props: { fnr: string; liten?: boolean }) => {
  const [dokumenter, setDokumenter] = useState<Journalpost[]>([])
  const [error, setError] = useState(false)
  const [dokumenterHentet, setDokumenterHentet] = useState(false)

  useEffect(() => {
    setDokumenterHentet(false)

    fetchDokumenter()

    async function fetchDokumenter() {
      const res = await hentDokumenter(props.fnr)

      if (res.status === 'ok') {
        setDokumenter(res.data)
        setDokumenterHentet(true)
      } else {
        setError(true)
      }
    }
  }, [props.fnr])

  return (
    (props.liten && (
      <SidebarPanel>
        <Overskrift>Dokumenter</Overskrift>
        <DokumentlisteLiten dokumenter={dokumenter} dokumenterHentet={dokumenterHentet} error={error} />
      </SidebarPanel>
    )) || (
      <OversiktWrapper>
        <h1>Dokumenter</h1>
        <Dokumentliste dokumenter={dokumenter} dokumenterHentet={dokumenterHentet} error={error} />
      </OversiktWrapper>
    )
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

export const Overskrift = styled.div`
  font-weight: 600;
  font-size: 20px;
  color: #3e3832;
`
