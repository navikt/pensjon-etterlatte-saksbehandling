import { Column, Content, ContentHeader, GridContainer } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import axios, { AxiosResponse } from 'axios'
import { Button } from '@navikt/ds-react'

enum Type {
  gjenlevendepensjon = 'gjenlevendepensjon',
  barnepensjon = 'barnepensjon',
}

enum Vedtak {
  avslag = 'avslag',
  innvilget = 'innvilget'
}

interface PDFBody {
  type?: Type
  vedtak?: Vedtak
  tidMeldemskapEllerPensjon?: string | number
}

const pdfBody: any = {
  saksnummer: '143',
  utbetalingsinfo: {
    beloep: 'kr. 48 000,-',
    kontonummer: '1200.34.23321',
    virkningsdato: '4. April 2022',
  },
  barn: {
    navn: 'Blåøyd Saks',
    fnr: '05111850870',
  },
  avdoed: {
    navn: 'Grønn Kopp',
    doedsdato: '29018322402',
  },
  aktuelleParagrafer: [],
}

export const Brev = () => {
  const [fileURL, setFileURL] = useState<string>()
  const [body, setBody] = useState<string>(JSON.stringify(pdfBody, undefined, 4))

  const generatePDF = () => {
    axios('http://localhost:8080/pdf/api/v1/genpdf/brev/innvilget', {
      method: 'POST',
      responseType: 'blob',
      data: JSON.parse(body) as PDFBody,
    }).then((response: AxiosResponse) => {
      return new Blob([response.data], { type: 'application/pdf' })
    }).then((file: Blob) => {
      const url = URL.createObjectURL(file)
      setFileURL(url)
    }).catch((e) => {
      console.error(e)
    })
  }

  const updateBody = (value: string) => {
    setBody(value)
  }

  useEffect(() => {
    generatePDF()
  }, [])

  return (
    <Content>
      <ContentHeader>
        <h1>Brev</h1>

        <GridContainer>

          <Column>
            <textarea
              style={{
                width: '500px',
                height: '500px',
              }}
              value={body || '{}'}
              onChange={(e) => updateBody(e.target.value)}
            />

            <br />

            <Button variant={'primary'} onClick={generatePDF}>
              Oppdater
            </Button>
          </Column>

          <br />

          <Column>
            {fileURL && <iframe width={'650px'} height={'1080px'} src={fileURL} />}
          </Column>
        </GridContainer>

      </ContentHeader>
    </Content>
  )
}
