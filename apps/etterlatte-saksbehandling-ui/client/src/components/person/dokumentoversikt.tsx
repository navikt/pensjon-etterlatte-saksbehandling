import { Dokumentliste } from './dokumentliste'
import styled from 'styled-components'
import { useEffect, useState } from 'react'
import { hentDokumenter } from '../../shared/api/brev'
import { Journalpost } from '../behandling/types'

export const Dokumentoversikt = (props: any) => {
  const [dokumenter, setDokumenter] = useState<Journalpost[]>([])
  const [error, setError] = useState(false)
  const [dokumenterHentet, setDokumenterHentet] = useState(false)

  useEffect(() => {
    setDokumenterHentet(false)
    hentDokumenter(props.fnr)
      .then((res) => setDokumenter(res.data.dokumentoversiktBruker.journalposter))
      .catch(() => setError(true))
      .finally(() => setDokumenterHentet(true))
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
