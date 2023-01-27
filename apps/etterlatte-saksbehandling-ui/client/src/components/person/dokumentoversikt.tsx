import { Dokumentliste } from './dokumentliste'
import styled from 'styled-components'
import { useEffect, useState } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { Journalpost } from '../behandling/types'

export const Dokumentoversikt = (props: { fnr: string }) => {
  const [dokumenter, setDokumenter] = useState<Journalpost[]>([])
  const [error, setError] = useState(false)
  const [dokumenterHentet, setDokumenterHentet] = useState(false)

  useEffect(() => {
    setDokumenterHentet(false)

    fetchDokumenter()

    async function fetchDokumenter() {
      const res = await hentDokumenter(props.fnr)

      if (res.status === 'ok') {
        setDokumenter(res.data.data.dokumentoversiktBruker.journalposter)
        setDokumenterHentet(true)
      } else {
        setError(true)
      }
    }
  }, [props.fnr])

  return (
    <OversiktWrapper>
      <h1>Dokumenter</h1>
      <Dokumentliste dokumenter={dokumenter} dokumenterHentet={dokumenterHentet} error={error} />
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
