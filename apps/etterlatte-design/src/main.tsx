import React from "react";
import ReactDOM from "react-dom/client";
import "@navikt/ds-css";
import { Box, Heading, HStack, Label, Tag, VStack } from "@navikt/ds-react";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Box paddingBlock="8" paddingInline="8">
      <VStack gap="4">
        <Heading size="xlarge">
          Etterlatte sine visuelle bestemmelser for Gjenny!
        </Heading>

        <Heading size="large">Tags</Heading>

        <HStack gap="4">
          <VStack gap="2">
            <Heading size="medium">Ytelse</Heading>

            <Label>Lang</Label>
            <div>
              <Tag variant="warning">Barnepejson</Tag>
            </div>
            <div>
              <Tag variant="warning">Omstillingstønad</Tag>
            </div>

            <Label>Kort</Label>
          </VStack>

          <VStack gap="2">
            <Heading size="medium">Behandlingstype</Heading>
          </VStack>
        </HStack>
      </VStack>
    </Box>
  </React.StrictMode>,
);
