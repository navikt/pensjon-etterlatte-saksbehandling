import { Button, Loader, Modal, TextField, Alert } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { Add } from '@navikt/ds-icons'
import styled from 'styled-components'
import { Adresse, hentMottakere, Mottaker, opprettBrevFraPDF } from '../../../../shared/api/brev'
import { useParams } from 'react-router-dom'
import { Border } from '../../soeknadsoversikt/styled'
import { Column, GridContainer } from '../../../../shared/styled'
import { PdfVisning } from '../pdf-visning'
import { MottakerComponent } from './mottaker'

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

interface FileMeta {
  mottaker: Mottaker
  filNavn: string
}

export const isEmptyAddressObject = (object?: Adresse): boolean => {
  if (object === undefined) return true
  if (JSON.stringify(object) === '{}') return true
  if (Object.values(object).every((value) => !value.length)) return true
  return false
}

export default function LastOppBrev({ leggTilNytt }: { leggTilNytt: (brev: any) => void }) {
  const { behandlingId } = useParams()

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [klarForLagring, setKlarforLagring] = useState<boolean>(false)
  const [adresse, setAdresse] = useState<Adresse | undefined>(undefined)
  const [fnrMottaker, setFnrMottaker] = useState<string | undefined>(undefined)
  const [orgMottaker, setOrgMottaker] = useState<string | undefined>(undefined)
  const [mottakere, setMottakere] = useState<DefaultMottaker[]>([])
  const [laster, setLaster] = useState(false)
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [selectedFile, setSelectedFile] = useState<any>()
  const [fileTitle, setFileTitle] = useState<string>('')
  const [fileName, setFileName] = useState<string>('')

  useEffect(() => {
    hentMottakere().then((res) => setMottakere(res))
  }, [])

  const opprett = () => {
    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker,
      orgnummer: orgMottaker,
      adresse: isEmptyAddressObject(adresse) ? undefined : adresse,
    }

    const fileMeta: FileMeta = {
      mottaker: brevMottaker,
      filNavn: fileTitle,
    }

    const formData = new FormData()

    formData.append('file', selectedFile, selectedFile.name)
    formData.append('fileMeta', JSON.stringify(fileMeta))

    opprettBrevFraPDF(behandlingId!!, brevMottaker, formData)
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
        setFileURL(undefined)
        setFileName('')
        setFileTitle('')
        setSelectedFile(null)
      })
  }

  const onFileChange = (event: any) => {
    const file = event.target.files[0]
    setSelectedFile(file)
    setFileTitle(file.name.replace('.pdf', ''))
    setFileName(file.name)
    const pdfUrl = URL.createObjectURL(file)
    setFileURL(pdfUrl)
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

  useEffect(() => {
    if (fileURL && (fnrMottaker || orgMottaker || !isEmptyAddressObject(adresse))) {
      setKlarforLagring(true)
    } else {
      setKlarforLagring(false)
    }
  }, [fileURL, fnrMottaker, orgMottaker, adresse])

  const fileData = () => {
    if (selectedFile) {
      return (
        <div>
          <h2>Brev detaljer</h2>
          <TextField
            label={'Tittel på filen'}
            description={'Vil vises i oversikten over brev'}
            value={fileTitle}
            onChange={(e) => setFileTitle(e.target.value)}
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
      {
        // TODO: legg til informasjon om at adresse må ligge i dokumentet
      }
      <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <GridContainer>
            <Column style={{ width: '500px', paddingRight: '20px' }}>
              <h1>Last opp brev</h1>
              <Alert variant={"warning"} size={'small'}>
                Filen som lastes opp må inneholde adresse og annen relevant informasjon om mottakeren da det vil bli
                sendt som brev.
              </Alert>

              <br />

              <div>
                <FileButton htmlFor="file">
                  <FileInput type="file" name="file" id="file" onChange={onFileChange} accept={'application/pdf'} />
                  Velg fil
                </FileButton>
                {fileName ? fileName : 'Ingen fil valgt'}
              </div>
              {fileData()}

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
              <PdfVisning fileUrl={fileURL} error={error} />
            </Column>
          </GridContainer>
        </Modal.Content>
      </CustomModal>
    </>
  )
}
