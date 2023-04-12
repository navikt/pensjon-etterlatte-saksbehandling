import { Button, Loader, Modal, TextField, Alert } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { Add } from '@navikt/ds-icons'
import styled from 'styled-components'
import { Adresse, hentMottakere, Mottaker, opprettBrevFraPDF } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Column, GridContainer } from '~shared/styled'
import { PdfVisning } from '../pdf-visning'
import { MottakerComponent } from './mottaker'
import { IBrev } from '../Brev'
import { Border } from '~components/behandling/soeknadsoversikt/styled'

const CustomModal = styled(Modal)`
  min-width: 540px;
`
const LastOppKnapp = styled.span`
  margin-left: 0.5rem;
`

const FileInput = styled.input`
  display: none;
`

const FileButton = styled.label`
  display: inline-block;
  padding: 12px;
  cursor: pointer;
  background-color: #0067c5;
  color: white;
  border-radius: 3px;
  box-sizing: border-box;
  border: 1px solid white;
  margin-right: 6px;
  &:hover {
    background-color: #0056b4;
  }
  &:active,
  :focus {
    outline: 3px solid black;
  }
`

export interface DefaultMottaker {
  id?: string
  idType?: string
  navn?: string
  land?: string
}

interface FilData {
  mottaker: Mottaker
  filNavn: string
}

export const isEmptyAddressObject = (object?: Adresse): boolean => {
  if (object === undefined) return true
  if (JSON.stringify(object) === '{}') return true
  if (Object.values(object).every((value) => !value.length)) return true
  return false
}

export default function LastOppBrev({ leggTilNytt }: { leggTilNytt: (brev: IBrev) => void }) {
  const { behandlingId } = useParams()

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [klarForLagring, setKlarforLagring] = useState<boolean>(false)
  const [adresse, setAdresse] = useState<Adresse | undefined>(undefined)
  const [fnrMottaker, setFnrMottaker] = useState<string | undefined>(undefined)
  const [orgMottaker, setOrgMottaker] = useState<string | undefined>(undefined)
  const [mottakere, setMottakere] = useState<DefaultMottaker[]>([])
  const [laster, setLaster] = useState(false)
  const [error, setError] = useState<string>()
  const [filURL, setFilURL] = useState<string>()
  const [valgtFil, setValgtFil] = useState<undefined | File>()
  const [filTittel, setFilTittel] = useState<string>('')
  const [filNavn, setFilNavn] = useState<string>('')

  useEffect(() => {
    hentMottakere().then((res) => {
      if (res.status === 'ok') setMottakere(res.data)
      else setError(res.error)
    })
  }, [])

  const opprett = () => {
    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker?.length ? fnrMottaker : undefined,
      orgnummer: orgMottaker?.length ? orgMottaker : undefined,
      adresse: isEmptyAddressObject(adresse) ? undefined : adresse,
    }

    const filData: FilData = {
      mottaker: brevMottaker,
      filNavn: filTittel,
    }

    const formData = new FormData()

    formData.append('fil', valgtFil as Blob, valgtFil!!.name)
    formData.append('filData', JSON.stringify(filData))

    opprettBrevFraPDF(behandlingId!!, formData)
      .then((data) => leggTilNytt(data))
      .catch((error) => setError(error))
      .finally(() => {
        setAdresse(undefined)
        setFnrMottaker(undefined)
        setOrgMottaker(undefined)
        setLaster(false)
        setIsOpen(false)
        setKlarforLagring(false)
        setError(undefined)
        setFilURL(undefined)
        setFilNavn('')
        setFilTittel('')
        setValgtFil(undefined)
      })
  }

  const onFileChange = (event: any) => {
    const fil = event.target.files[0]
    setValgtFil(fil)
    setFilTittel(fil.name.replace('.pdf', ''))
    setFilNavn(fil.name)
    const pdfUrl = URL.createObjectURL(fil)
    setFilURL(pdfUrl)
  }

  const oppdaterMottaker = (value: string, id: string) => {
    setFnrMottaker(id === 'FNR' ? value : '')
    setOrgMottaker(id === 'ORGNR' ? value : '')

    // TODO: Fikse dette når det blir aktuelt å bruke koden
    setAdresse({})
  }

  useEffect(() => {
    if (filURL && (fnrMottaker || orgMottaker || !isEmptyAddressObject(adresse))) {
      setKlarforLagring(true)
    } else {
      setKlarforLagring(false)
    }
  }, [filURL, fnrMottaker, orgMottaker, adresse])

  const opplastetFilInfo = () => {
    if (valgtFil) {
      return (
        <div>
          <h2>Brev detaljer</h2>
          <TextField
            label={'Tittel på filen'}
            description={'Vil vises i oversikten over brev'}
            value={filTittel}
            onChange={(e) => setFilTittel(e.target.value)}
          />
          <br />
          <Border />

          <h3>Mottaker</h3>
          <MottakerComponent
            adresse={adresse}
            mottakere={mottakere}
            fnrMottaker={fnrMottaker}
            orgMottaker={orgMottaker}
            oppdaterMottaker={oppdaterMottaker}
          />
        </div>
      )
    } else {
      return (
        <div>
          <h4>
            Start med å velge en fil du vil laste opp. <br /> Må være i PDF format.
          </h4>
        </div>
      )
    }
  }

  return (
    <>
      <LastOppKnapp>
        <Button variant={'secondary'} onClick={() => setIsOpen(true)}>
          Last opp brev &nbsp;
          <Add />
        </Button>
      </LastOppKnapp>
      <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <GridContainer>
            <Column style={{ width: '500px', paddingRight: '20px' }}>
              <h1>Last opp brev</h1>
              <Alert variant={'warning'} size={'small'}>
                Filen som lastes opp må inneholde adresse og annen relevant informasjon om mottakeren da det vil bli
                sendt som brev.
              </Alert>

              <br />

              <div>
                <FileButton htmlFor="file">
                  <FileInput type="file" name="file" id="file" onChange={onFileChange} accept={'application/pdf'} />
                  Velg fil
                </FileButton>
                {filNavn ? filNavn : 'Ingen fil valgt'}
              </div>
              {opplastetFilInfo()}

              <br />
              <Border />

              <Button
                variant={'primary'}
                style={{ float: 'right' }}
                onClick={opprett}
                disabled={laster || !klarForLagring}
              >
                Lagre {laster && <Loader />}
              </Button>
              <br />
              <br />
            </Column>
            <Column style={{ paddingLeft: '20px', marginTop: '100px' }}>
              <PdfVisning fileUrl={filURL} error={error} />
            </Column>
          </GridContainer>
        </Modal.Content>
      </CustomModal>
    </>
  )
}
