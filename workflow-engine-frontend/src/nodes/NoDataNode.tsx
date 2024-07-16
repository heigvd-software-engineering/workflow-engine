import { memo } from 'react';
import { Node, NodeProps } from '@xyflow/react';
import { BaseNode, BaseNodeData } from "./BaseNode";

export type NoDataNodeData = BaseNodeData

export type NoDataNodeTypeNode = Node<NoDataNodeData, "NoDataNode">;

export const NoDataNode = (title: string) => memo(function NoDataNode(props: NodeProps<NoDataNodeTypeNode>) { 
  return (
    <BaseNode {...props} title={title} />
  );
}, (prev, next) => JSON.stringify(prev.data) == JSON.stringify(next.data));