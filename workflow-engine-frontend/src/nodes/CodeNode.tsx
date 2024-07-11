import "prismjs/themes/prism-okaidia.css";

import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { NodeProps } from '@xyflow/react';
import { BaseNodeData } from "./BaseNode";
import Editor from "react-simple-code-editor";
import { highlight, languages } from "prismjs";
import { ModifiableNode } from "./ModifiableNode";
import { Node } from "@xyflow/react";
import { Box, Button, ToggleButton, ToggleButtonGroup } from "@mui/material";
import { IChangeCodeNode, ICodeChangeCodeNode, ILanguageChangeCodeNode, Languages } from "../types/Types";
import { $enum } from "ts-enum-util";
import { getGrammarLanguageName } from "../utils/TypeUtils";

export type CodeNodeData = BaseNodeData & {
  initialCode: string;
  initialLanguage: keyof typeof Languages;
}

export type CodeNodeTypeNode = Node<CodeNodeData, "CodeNode">;

export const CodeNode = memo(function CodeNode(props: NodeProps<CodeNodeTypeNode>) {
  const defaultCode = useMemo(() => props.data.initialCode, [props.data.initialCode]);
  const [code, setCode] = useState(defaultCode);

  const defaultLanguage = useMemo(() => props.data.initialLanguage, [props.data.initialLanguage]);
  const [language, setLanguage] = useState(defaultLanguage);

  const cancel = useCallback(() => {
    setCode(defaultCode);
    setLanguage(defaultLanguage);
  }, [defaultCode, defaultLanguage]);

  useEffect(() => {
    cancel();
  }, [cancel]);

  const sendValues = useCallback(() => {
    const baseData: IChangeCodeNode = {
      action: "changeCodeNode",
      uuid: props.data.uuid,
      nodeId: props.data.node.id
    };

    const dataCode: ICodeChangeCodeNode = {
      ...baseData,
      subAction: "code",
      code: code
    };
    props.data.sendToWebsocket(dataCode);

    const dataLanguage: ILanguageChangeCodeNode = {
      ...baseData,
      subAction: "language",
      language: language
    };
    props.data.sendToWebsocket(dataLanguage);
  }, [code, language, props.data]);
 
  const hasChanged = useMemo(() => defaultCode != code || defaultLanguage != language, [defaultCode, code, defaultLanguage, language]);

  const grammarName = getGrammarLanguageName(language);

  return (
    <ModifiableNode {...props} title="Code">
      <ToggleButtonGroup
        sx={{display: "flex", justifyContent: "center"}}
        size="small"
        value={language}
        onChange={(_, value) => {
          if (value != null) {
            setLanguage(value);
          }
        }}
        exclusive
      >
        {$enum(Languages).getKeys().map(k => (
          <ToggleButton size="small" key={k} value={k}>{k}</ToggleButton>
        ))}
      </ToggleButtonGroup>
      <Editor 
        value={code}
        onValueChange={code => setCode(code)}
        highlight={code => highlight(code, languages[grammarName], grammarName)}
        padding={10}
        className="nodrag"
        style={{
          fontFamily: '"Fira code", "Fira Mono", monospace',
          fontSize: 15,
          border: "1px solid gray"
        }}
      />
      <Box sx={{marginTop: 1, display: "flex", justifyContent: "center"}}>
        <Button size="small" variant="contained" sx={{marginRight: 1}} onClick={sendValues} disabled={!hasChanged}>
          Save
        </Button>
        <Button size="small" variant="contained" color="error" onClick={cancel} disabled={!hasChanged}>
          Cancel
        </Button>
      </Box>
    </ModifiableNode>
  );
}, (prev, next) => JSON.stringify(prev.data) == JSON.stringify(next.data));