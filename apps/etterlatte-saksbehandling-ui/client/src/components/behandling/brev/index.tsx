import { Content, ContentHeader } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import { useParams } from "react-router-dom";
import { hentBrev } from "../../../shared/api/brev";
import { BodyLong, Cell, Grid } from "@navikt/ds-react";

export const Brev = () => {
  const { behandlingId } = useParams()

  const [error, setError] = useState<string>("wtf")
  const [fileURL, setFileURL] = useState<string>()

  const generatePDF = () => hentBrev(behandlingId!!)
      .then(file => URL.createObjectURL(file))
      .then(url => setFileURL(url))
      .catch(err => setError(err.message))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)
      })

  useEffect(() => {
    generatePDF()
  }, [])

  return (
    <Content>
      <ContentHeader>
        <h1>Brev</h1>

        <Grid>
          <Cell xs={12} lg={6}>
            <BodyLong>
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur sed ante sit amet tellus aliquet mattis. Donec blandit, urna ac vulputate tincidunt, lorem massa tempor lectus, nec porttitor velit nunc ac ex. Vivamus vel elementum magna. Nullam tristique nisl sit amet ante interdum, vitae tincidunt libero placerat. Pellentesque et dolor at felis dapibus cursus viverra ut massa. Nunc ac pharetra est. Donec finibus ante ut volutpat blandit. Integer condimentum eros malesuada luctus egestas. Integer sodales aliquet nisi non elementum. Nunc congue, nisi in congue dictum, odio est ultrices enim, non venenatis nulla diam non purus. Pellentesque dapibus rutrum elementum.
            </BodyLong>
          </Cell>

          {/*  TODO: Løse feilhåndtering på en god måte */}
          {error ? (
              <BodyLong>{error}</BodyLong>
          ) : (
            <Cell xs={12} lg={6}>
              {fileURL && <iframe width={'800px'} height={'1080px'} src={fileURL} />}
            </Cell>
          )}
        </Grid>

      </ContentHeader>
    </Content>
  )
}
