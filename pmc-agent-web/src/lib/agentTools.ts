/**
 * 需要嵌套卡片展示的业务子 Agent。
 * general_chat 故意不在此集合：问候/澄清按普通助手气泡展示。
 */
export const SUB_AGENT_NAMES = new Set([
  'query_bank',
  'summarize_bank',
  'export_excel',
  'create_chart',
])

const AGENT_LABELS: Record<string, string> = {
  general_chat: '问答',
  pmc_supervisor: '总助手',
  query_bank: '查询',
  summarize_bank: '汇总',
  export_excel: '导出',
  create_chart: '图表',
}

/**
 * @param agentName 子 Agent 名
 * @return 卡片标题用短名
 */
export function agentToolLabel(agentName: string): string {
  return AGENT_LABELS[agentName] ?? agentName
}

/**
 * @param agentName 图上的 agent 字段
 * @return 是否为业务子 Agent
 */
export function isSubAgentName(agentName?: string): boolean {
  return !!agentName && SUB_AGENT_NAMES.has(agentName)
}
