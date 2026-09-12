/** Supervisor 以 Agent-as-Tool 挂载的子 Agent 工具名 */
export const AGENT_TOOL_NAMES = new Set([
  'query_bank',
  'summarize_bank',
  'export_excel',
  'create_chart',
])

const AGENT_TOOL_LABELS: Record<string, string> = {
  query_bank: '查询',
  summarize_bank: '汇总',
  export_excel: '导出',
  create_chart: '图表',
}

/**
 * @param agentName 子 Agent 工具名
 * @return 卡片标题用短名
 */
export function agentToolLabel(agentName: string): string {
  return AGENT_TOOL_LABELS[agentName] ?? agentName
}
