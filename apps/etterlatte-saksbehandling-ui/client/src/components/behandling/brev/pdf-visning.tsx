import {BodyLong} from "@navikt/ds-react";
import styled from "styled-components";

const PdfViewer = styled.iframe`
  margin-bottom: 20px;
  width: 800px;
  height: 1080px;
`


export const PdfVisning = ({fileUrl, error}: {
    fileUrl?: string,
    error?: string
}) => {
    return (
        <>
            {error && (
                <BodyLong>
                    En feil har oppstått ved henting av PDF:
                    <br/>
                    <code>{error}</code>
                </BodyLong>
            )}

            <div>{fileUrl && <PdfViewer src={fileUrl}/>}</div>
        </>
    )
}
