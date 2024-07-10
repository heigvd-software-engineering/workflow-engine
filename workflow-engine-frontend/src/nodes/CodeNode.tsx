import "prismjs/themes/prism-okaidia.css";

import { useState } from 'react';
import { NodeProps } from '@xyflow/react';
import { BaseNodeData } from "./BaseNode";
import Editor from "react-simple-code-editor";
import { highlight, languages } from "prismjs";
import ModifiableNode from "./ModifiableNode";
import { Node } from "@xyflow/react";

export type CodeNodeData = BaseNodeData & {
  initialCode: string;
}

export type CodeNodeTypeNode = Node<CodeNodeData, "CodeNode">;

export default function CodeNode(props: NodeProps<CodeNodeTypeNode>) {
  const [code, setCode] = useState(props.data.initialCode);
 
  return (
    <ModifiableNode {...props} title="Code">
      <Editor 
        value={code}
        onValueChange={code => setCode(code)}
        highlight={code => highlight(code, languages.js, "js")}
        padding={10}
        className="nodrag"
        style={{
          fontFamily: '"Fira code", "Fira Mono", monospace',
          fontSize: 15,
          border: "1px solid gray"
        }}
      />
    </ModifiableNode>
  );
}