import { Button, Loader, Modal, Select } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { Add } from '@navikt/ds-icons'
import styled from 'styled-components'
import {
  Adresse,
  hentForhaandsvisning,
  hentMaler,
  hentMottakere,
  Mal,
  Mottaker,
  nyttBrevForBehandling,
} from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Column, GridContainer } from '~shared/styled'
import { PdfVisning } from '../pdf-visning'
import { MottakerComponent } from './mottaker'
import { isEmptyAddressObject } from './last-opp'
import { IBrev } from '../Brev'
import { useAppSelector } from '~store/Store'
import { Border } from '~components/behandling/soeknadsoversikt/styled'

const CustomModal = styled(Modal)`
  min-width: 540px;
`

interface DefaultMottaker {
  id?: string
  idType?: string
  navn?: string
  land?: string
}

export default function NyttBrev({ leggTilNytt }: { leggTilNytt: (brev: IBrev) => void }) {
  const { behandlingId } = useParams()

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [klarForLagring, setKlarforLagring] = useState<boolean>(false)
  const [adresse, setAdresse] = useState<Adresse>({})
  const [fnrMottaker, setFnrMottaker] = useState<string | undefined>(undefined)
  const [orgMottaker, setOrgMottaker] = useState<string | undefined>(undefined)
  const [mottakere, setMottakere] = useState<DefaultMottaker[]>([])
  const [mal, setMal] = useState<string>()
  const [maler, setMaler] = useState<Mal[]>([])
  const [laster, setLaster] = useState(false)
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [enhet, setEnhet] = useState<string>('')

  const saksbehandlerEnheter = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.enheter)

  useEffect(() => {
    hentMaler().then((res) => {
      if (res.status === 'ok') setMaler(res.data)
      else setError(res.error)
    })
    hentMottakere().then((res) => {
      if (res.status === 'ok') setMottakere(res.data)
      else setError(res.error)
    })
  }, [])

  const forhaandsvis = () => {
    if (!mal || !enhet) return

    setLaster(true)

    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker?.length ? fnrMottaker : undefined,
      orgnummer: orgMottaker?.length ? orgMottaker : undefined,
      adresse: isEmptyAddressObject(adresse) ? undefined : adresse,
    }

    hentForhaandsvisning(
      brevMottaker,
      {
        tittel: maler.find((m: Mal) => m.navn === mal)?.tittel,
        navn: mal,
      },
      enhet
    )
      .then((res) => {
        if (res.status === 'ok') {
          return new Blob([res.data], { type: 'application/pdf' })
        } else {
          throw Error(res.error)
        }
      })
      .then((file) => URL.createObjectURL(file!!))
      .then((url) => {
        setFileURL(url)
        setError(undefined)
        setKlarforLagring(true)
      })
      .catch((e) => setError(e.message))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)
        setLaster(false)
      })
  }

  const opprett = () => {
    if (!mal || !enhet) return

    setLaster(true)

    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker?.length ? fnrMottaker : undefined,
      orgnummer: orgMottaker?.length ? orgMottaker : undefined,
      adresse: isEmptyAddressObject(adresse) ? undefined : adresse,
    }

    nyttBrevForBehandling(
      behandlingId!!,
      brevMottaker,
      {
        tittel: maler.find((m: Mal) => m.navn === mal)?.tittel,
        navn: mal,
      },
      enhet
    )
      .then((res) => {
        if (res.status === 'ok') leggTilNytt(res.data)
        else throw Error(res.error)
      })
      .finally(() => {
        setAdresse({})
        setFnrMottaker(undefined)
        setOrgMottaker(undefined)
        setLaster(false)
        setIsOpen(false)
        setKlarforLagring(false)
        setMal('')
        setError(undefined)
        setFileURL(undefined)
      })
  }

  const oppdaterMottaker = (value: string, id: string) => {
    setKlarforLagring(false)
    setFnrMottaker(id === 'FNR' ? value : '')
    setOrgMottaker(id === 'ORGNR' ? value : '')

    // TODO: Fikse dette når det blir aktuelt å bruke koden
    setAdresse({})
  }

  return (
    <>
      <Button variant={'secondary'} onClick={() => setIsOpen(true)}>
        Nytt brev &nbsp;
        <Add />
      </Button>

      <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <GridContainer>
            <Column style={{ width: '500px', paddingRight: '20px' }}>
              <h1>Opprett nytt brev</h1>

              <br />

              <Select
                label={'Mal'}
                size={'medium'}
                onChange={(e) => {
                  setMal(e.target.value ? e.target.value : '')
                  setKlarforLagring(false)
                }}
              >
                <option value={undefined} label={'Velg mal ...'} />
                {maler.map((mal: Mal, i: number) => (
                  <option key={i} value={mal.navn}>
                    {mal.tittel}
                  </option>
                ))}
              </Select>

              <br />
              <h3>Mottaker</h3>
              <MottakerComponent
                oppdaterMottaker={oppdaterMottaker}
                fnrMottaker={fnrMottaker}
                orgMottaker={orgMottaker}
                mottakere={mottakere}
                adresse={adresse}
              />

              <br />
              <Border />

              <h3>Enhet</h3>
              <Select
                label={'Velg hvilken enhet du tilhører'}
                value={enhet}
                onChange={(e) => {
                  if (e.target.value !== enhet && enhet) setKlarforLagring(false)
                  setEnhet(e.target.value)
                }}
              >
                <option value={''}>Velg en enhet</option>
                {saksbehandlerEnheter?.map((m, i) => (
                  <option key={i} value={m.enhetId}>
                    {m.navn} ({m.enhetId})
                  </option>
                ))}
              </Select>

              <br />
              <Border />

              <br />
              <br />

              {!klarForLagring && (
                <Button variant={'secondary'} style={{ float: 'right' }} onClick={forhaandsvis} disabled={laster}>
                  Forhåndsvis {laster && <Loader />}
                </Button>
              )}
              {klarForLagring && (
                <Button variant={'primary'} style={{ float: 'right' }} onClick={opprett} disabled={laster}>
                  Lagre {laster && <Loader />}
                </Button>
              )}
              <br />
              <br />
            </Column>
            <Column style={{ paddingLeft: '20px', marginTop: '100px' }}>
              <PdfVisning fileUrl={fileURL} error={error} />
              {!fileURL && (
                <p>
                  Vennligst velg mal, mottaker og enhet. <br /> Deretter trykk forhåndsvis for å se dokumentet.
                </p>
              )}
            </Column>
          </GridContainer>
        </Modal.Content>
      </CustomModal>
    </>
  )
}
