import { Button, Loader, Modal, Select } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { Add } from '@navikt/ds-icons'
import styled from 'styled-components'
import {
  Adresse,
  hentForhaandsvisning,
  hentMaler,
  hentMottakere,
  Mottaker,
  nyttBrevForBehandling,
} from '../../../../shared/api/brev'
import { useParams } from 'react-router-dom'
import { Border } from '../../soeknadsoversikt/styled'
import { Column, GridContainer } from '../../../../shared/styled'
import { PdfVisning } from '../pdf-visning'
import { MottakerComponent } from './mottaker'
import { isEmptyAddressObject } from './last-opp'

const CustomModal = styled(Modal)`
  min-width: 540px;
`

interface DefaultMottaker {
  id?: string
  idType?: string
  navn?: string
  land?: string
}

export default function NyttBrev({ leggTilNytt }: { leggTilNytt: (brev: any) => void }) {
  const { behandlingId } = useParams()

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [klarForLagring, setKlarforLagring] = useState<boolean>(false)
  const [adresse, setAdresse] = useState<Adresse | undefined>(undefined)
  const [fnrMottaker, setFnrMottaker] = useState<string | undefined>(undefined)
  const [orgMottaker, setOrgMottaker] = useState<string | undefined>(undefined)
  const [mottakere, setMottakere] = useState<DefaultMottaker[]>([])
  const [mal, setMal] = useState<any>(undefined)
  const [maler, setMaler] = useState<any>([])
  const [laster, setLaster] = useState(false)
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()

  useEffect(() => {
    hentMaler().then((res) => setMaler(res))
    hentMottakere().then((res) => setMottakere(res))
  }, [])

  const forhaandsvis = () => {
    if (!mal) return

    setLaster(true)

    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker,
      orgnummer: orgMottaker,
      adresse: isEmptyAddressObject(adresse) ? undefined : adresse,
    }

    hentForhaandsvisning(brevMottaker, {
      tittel: maler.find((m: any) => m.navn === mal).tittel,
      navn: mal,
    })
      .then((file) => URL.createObjectURL(file))
      .then((url) => {
        setFileURL(url)
        setError(undefined)
        setKlarforLagring(true)
      })
      .catch((e) => {
        setError(e.message)
      })
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)
        setLaster(false)
      })
  }

  const opprett = () => {
    if (!mal) return

    setLaster(true)

    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker,
      orgnummer: orgMottaker,
      adresse: isEmptyAddressObject(adresse) ? undefined : adresse,
    }

    nyttBrevForBehandling(behandlingId!!, brevMottaker, {
      tittel: maler.find((m: any) => m.navn === mal).tittel,
      navn: mal,
    })
      .then((brev) => leggTilNytt(brev))
      .finally(() => {
        setAdresse(undefined)
        setFnrMottaker(undefined)
        setOrgMottaker(undefined)
        setLaster(false)
        setIsOpen(false)
        setKlarforLagring(false)
        setMal(undefined)
        setError(undefined)
        setFileURL(undefined)
      })
  }

  const oppdaterMottaker = (value: string, id: string, section?: string) => {
    if (id === 'ORGNR') {
      setFnrMottaker('')
      setOrgMottaker(value)
      setAdresse({})
    }

    if (id === 'FNR') {
      setOrgMottaker('')
      setFnrMottaker(value)
      setAdresse({})
    }

    if (id === 'ADRESSE') {
      setOrgMottaker('')
      setFnrMottaker('')
      if (section === 'fornavn') setAdresse({ ...adresse, fornavn: value })
      if (section === 'etternavn') setAdresse({ ...adresse, etternavn: value })
      if (section === 'adresse') setAdresse({ ...adresse, adresse: value })
      if (section === 'postnummer') setAdresse({ ...adresse, postnummer: value })
      if (section === 'poststed') setAdresse({ ...adresse, poststed: value })
    }
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
                  setMal(e.target.value)
                  setKlarforLagring(false)
                }}
              >
                <option value={undefined} label={'Velg mal ...'} />
                {maler.map((mal: any, i: number) => (
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
                  Vennligst velg mal og mottaker. <br /> Deretter trykk forhåndsvis for å se dokumentet.
                </p>
              )}
            </Column>
          </GridContainer>
        </Modal.Content>
      </CustomModal>
    </>
  )
}
