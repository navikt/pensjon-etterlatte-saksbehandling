import React from "react";
import ReactDOM from "react-dom/client";
import "@navikt/ds-css";
import { Box, Heading, HStack, Tag, VStack } from "@navikt/ds-react";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Box paddingBlock="8" paddingInline="8">
      <VStack gap="4">
        <Heading size="large">
          Etterlatte sine visuelle bestemmelser for Gjenny!
        </Heading>

        <Heading size="medium">Tags</Heading>
        <VStack gap="2">
          <div>
            <Tag variant="warning">Barnepejson</Tag>
          </div>
          <div>
            <Tag variant="warning">Omstillingstønad</Tag>
          </div>
        </VStack>
      </VStack>
    </Box>
  </React.StrictMode>,
);
