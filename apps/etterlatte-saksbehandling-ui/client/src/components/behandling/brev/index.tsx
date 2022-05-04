import { Column, Content, ContentHeader, GridContainer } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import { useParams } from "react-router-dom";
import { hentBrev } from "../../../shared/api/brev";

export const Brev = () => {
  const { behandlingId } = useParams()

  const [fileURL, setFileURL] = useState<string>()

  const generatePDF = () => hentBrev(behandlingId!!)
      .then(res => setFileURL(res.data))
      .catch(err => console.log(err))

  useEffect(() => {
    generatePDF()
  }, [])

  return (
    <Content>
      <ContentHeader>
        <h1>Brev</h1>

        <GridContainer>
          <Column>
            {fileURL && <iframe width={'800px'} height={'1080px'} src={fileURL} />}
          </Column>
        </GridContainer>

      </ContentHeader>
    </Content>
  )
}
